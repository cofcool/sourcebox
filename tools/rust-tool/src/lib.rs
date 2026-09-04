use anyhow::{anyhow, Result};
use regex::Regex;
use std::collections::{HashMap, HashSet};
use std::fs;
use std::fs::OpenOptions;
use std::io::Write;
use std::path::PathBuf;

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RunnerType {
    Web,
    Cli,
    Gui,
}

#[derive(Debug, Clone)]
pub enum ToolName {
    Link2Tool,
    Other(String),
}

impl ToolName {
    pub fn name(&self) -> String {
        match self {
            ToolName::Link2Tool => "link2Tool".into(),
            ToolName::Other(s) => s.clone(),
        }
    }
}

pub trait ToolContext {
    fn runner_type(&self) -> RunnerType;
}

pub trait Logger {
    fn info(&self, s: &str);
    fn error(&self, s: &str);
}

pub trait Tool {
    fn name(&self) -> ToolName;
    fn run(&self, args: Args) -> Result<()>;
    fn config(&self) -> Args;

    fn get_logger(&self) -> Option<Box<dyn Logger>> {
        None
    }
}

#[derive(Debug, Clone)]
pub struct Arg {
    pub key: String,
    pub val: Option<String>,
    pub desc: Option<String>,
    pub required: bool,
    pub demo: Option<String>,
}

impl Arg {
    pub fn new(key: impl Into<String>, val: Option<impl Into<String>>) -> Self {
        Arg {
            key: key.into(),
            val: val.map(|v| v.into()),
            desc: None,
            required: false,
            demo: None,
        }
    }

    pub fn with_meta(
        key: impl Into<String>,
        val: Option<impl Into<String>>,
        desc: Option<impl Into<String>>,
        required: bool,
        demo: Option<impl Into<String>>,
    ) -> Self {
        Arg {
            key: key.into(),
            val: val.map(|v| v.into()),
            desc: desc.map(|s| s.into()),
            required,
            demo: demo.map(|s| s.into()),
        }
    }

    pub fn is_present(&self) -> bool {
        self.val.is_some()
    }

    pub fn required_val(&self, message: &str) -> Result<String> {
        self.val
            .clone()
            .ok_or_else(|| anyhow!(message.to_string()))
    }

    pub fn opt_val(&self) -> Option<String> {
        self.val.clone()
    }

    pub fn test<F: FnOnce(&str) -> bool>(&self, predicate: F) -> bool {
        if let Some(v) = &self.val {
            predicate(v)
        } else {
            true
        }
    }
}

type AliasInterceptor = Box<dyn FnMut(&mut HashMap<String, Arg>, &Arg, &Arg) -> Result<()>>;

pub struct Args {
    pub map: HashMap<String, Arg>,
    aliases: HashMap<String, Arg>,
    alias_interceptors: HashMap<String, AliasInterceptor>,
    runner_types: HashSet<RunnerType>,
    // context omitted for simplicity
    raw: Vec<String>,
}

impl Args {
    pub fn new() -> Self {
        let mut s = HashSet::new();
        s.insert(RunnerType::Cli);
        Args {
            map: HashMap::new(),
            aliases: HashMap::new(),
            alias_interceptors: HashMap::new(),
            runner_types: s,
            raw: vec![],
        }
    }

    pub fn with_capacity(cap: usize) -> Self {
        let mut s = HashSet::new();
        s.insert(RunnerType::Cli);
        Args {
            map: HashMap::with_capacity(cap),
            aliases: HashMap::new(),
            alias_interceptors: HashMap::new(),
            runner_types: s,
            raw: vec![],
        }
    }

    pub fn from_args<I, S>(args: I) -> Self
    where
        I: IntoIterator<Item = S>,
        S: Into<String>,
    {
        let mut a = Args::new();
        let raw: Vec<String> = args.into_iter().map(|s| s.into()).collect();
        a.raw = raw.clone();
        for s in raw {
            if !s.starts_with("--") {
                continue;
            }
            if let Some(idx) = s.find('=') {
                let key = s[2..idx].to_string();
                let val = s[idx + 1..].to_string();
                a.arg(Arg::new(key.clone(), Some(val)));
            } else {
                let key = s[2..].to_string();
                a.arg(Arg::new(key.clone(), None::<String>));
            }
        }
        a
    }

    pub fn from_file(path: impl Into<PathBuf>) -> Result<Self> {
        let p = path.into();
        let content = fs::read_to_string(&p)?;
        let mut items = vec![];
        for line in content.lines() {
            let line = line.trim();
            if line.starts_with('#') || line.is_empty() {
                continue;
            }
            items.push(format!("--{}", line));
        }
        Ok(Args::from_args(items))
    }

    pub fn arg(&mut self, arg: Arg) -> &mut Self {
        self.map.insert(arg.key.clone(), arg);
        self
    }

    pub fn arg_kv(&mut self, key: impl Into<String>, val: Option<impl Into<String>>) -> &mut Self {
        let k = key.into();
        let v = val.map(|s| s.into());
        self.map.insert(k.clone(), Arg::new(k, v));
        self
    }

    pub fn read_arg(&self, key: &str) -> Result<&Arg> {
        self.map.get(key).ok_or_else(|| {
            anyhow!(format!("Do not support argument {}, please see the help", key))
        })
    }

    pub fn get_arg_val(&self, key: &str) -> Option<String> {
        self.map.get(key).and_then(|a| a.val.clone())
    }

    pub fn alias(
        &mut self,
        alias: impl Into<String>,
        name: ToolName,
        arg_name: impl Into<String>,
        desc: impl Into<String>,
    ) -> &mut Self {
        let alias = alias.into();
        let value = Arg::with_meta(name.name(), Some(arg_name.into()), Some(desc.into()), false, None::<String>);
        self.aliases.insert(alias, value);
        self
    }

    pub fn alias_with_interceptor<F>(
        &mut self,
        alias: impl Into<String>,
        name: ToolName,
        arg_name: impl Into<String>,
        desc: impl Into<String>,
        interceptor: F,
    ) -> &mut Self
    where
        F: FnMut(&mut HashMap<String, Arg>, &Arg, &Arg) -> Result<()> + 'static,
    {
        let alias = alias.into();
        let value = Arg::with_meta(name.name(), Some(arg_name.into()), Some(desc.into()), false, None::<String>);
        self.aliases.insert(alias.clone(), value.clone());
        self.alias_interceptors
            .insert(alias, Box::new(interceptor));
        self
    }

    pub fn runner_types(&mut self, types: HashSet<RunnerType>) -> &mut Self {
        self.runner_types = types;
        self
    }

    pub fn supports_type(&self, t: &RunnerType) -> bool {
        self.runner_types.contains(t)
    }

    pub fn is_current_type(&self, _t: &RunnerType) -> bool {
        // context omitted in this simplified implementation
        false
    }

    pub fn copy_alias_from(&mut self, other: &Args) -> Result<()> {
        for (k, v) in &other.aliases {
            self.aliases.insert(k.clone(), v.clone());
        }

        let mut cmds: HashMap<String, Arg> = HashMap::new();
        for arg in self.map.values() {
            if self.aliases.contains_key(&arg.key) {
                if let Some(alias) = self.aliases.get(&arg.key) {
                    cmds.insert("tool".into(), Arg::new("tool", Some(alias.key.clone())));
                    if let Some(name) = &alias.val {
                        cmds.insert(name.clone(), Arg::new(name.clone(), arg.val.clone()));
                    }
                    if let Some(consumer) = self.alias_interceptors.get_mut(&arg.key) {
                        (consumer)(&mut cmds, arg, alias)?;
                    }
                }
            }
        }

        for (k, v) in cmds {
            self.map.insert(k, v);
        }

        Ok(())
    }

    pub fn remove_prefix(&self, prefix: &str) -> Self {
        let mut new = Args::with_capacity(self.map.len());
        new.aliases = self.aliases.clone();
        // alias_interceptors not cloned for simplicity
        for (k, v) in &self.map {
            if k.starts_with(&format!("{}.", prefix)) && k != prefix {
                let nk = k[prefix.len() + 1..].to_string();
                new.map.insert(nk, Arg::with_meta(nk.clone(), v.val.clone(), v.desc.clone(), v.required, v.demo.clone()));
            } else {
                new.map.insert(k.clone(), v.clone());
            }
        }
        new
    }

    pub fn copy_config_from(&mut self, config: &Args) -> Result<()> {
        let mut missing: Vec<String> = vec![];
        for arg in config.map.values() {
            if !self.map.contains_key(&arg.key) {
                if arg.required {
                    missing.push(arg.key.clone());
                } else {
                    self.map.insert(arg.key.clone(), arg.clone());
                }
            }
        }
        if !missing.is_empty() {
            let msg = missing
                .into_iter()
                .map(|k| format!("{} must be specified", k))
                .collect::<Vec<_>>()
                .join("; ");
            return Err(anyhow!(msg));
        }
        Ok(())
    }

    pub fn to_raw_args(&self) -> Vec<String> {
        self.raw.clone()
    }

    pub fn to_help_string(&self) -> String {
        let mut keys: Vec<&Arg> = self.map.values().collect();
        keys.sort_by(|a, b| a.key.cmp(&b.key));
        let synopsis = keys
            .iter()
            .map(|a| {
                format!(
                    "{}--{}={}{}",
                    if a.required { "" } else { "[" },
                    a.key,
                    a.val
                        .as_ref()
                        .or(a.demo.as_ref())
                        .map(|s| s.as_str())
                        .unwrap_or(""),
                    if a.required { "" } else { "]" }
                )
            })
            .collect::<Vec<_>>()
            .join(" ");
        let description = keys
            .iter()
            .map(|a| {
                format!(
                    "    --{}    {}{}",
                    a.key,
                    a.desc.clone().unwrap_or_default(),
                    if a.is_present() {
                        format!(". Default: {}", a.val.as_ref().unwrap())
                    } else {
                        format!(". Example: {}", a.demo.clone().unwrap_or_default())
                    }
                )
            })
            .collect::<Vec<_>>()
            .join("\n");
        let alias = self
            .aliases
            .iter()
            .map(|(k, v)| {
                format!(
                    "    --{}    --tool={} --{}",
                    k,
                    v.key,
                    v.val.clone().unwrap_or_default()
                )
            })
            .collect::<Vec<_>>()
            .join("\n");

        format!("Synopsis\n    {}\nDescription\n{}\nAlias\n{}", synopsis, description, alias)
    }
}

// 简化版的 LinkCovertTool
pub struct LinkCovertTool;

impl Tool for LinkCovertTool {
    fn name(&self) -> ToolName {
        ToolName::Link2Tool
    }

    fn run(&self, args: Args) -> Result<()> {
        let input = args
            .get_arg_val("input")
            .ok_or_else(|| anyhow!("input required"))?;
        let output = args
            .get_arg_val("output")
            .ok_or_else(|| anyhow!("output required"))?;

        let content = fs::read_to_string(&input)?;
        let mut out = OpenOptions::new()
            .create(true)
            .append(true)
            .open(&output)?;

        for line in content.lines() {
            if input.ends_with(".desktop") {
                if let Some(mat) = Regex::new(r"http.+").unwrap().find(line) {
                    let url = mat.as_str();
                    let filename = PathBuf::from(&input)
                        .file_stem()
                        .and_then(|s| s.to_str())
                        .unwrap_or("file");
                    writeln!(out, "* [{}]({})", filename, url)?;
                }
            } else if input.ends_with(".webloc") {
                if let Some(mat) = Regex::new(r"http[^<\\s]+").unwrap().find(line) {
                    let url = mat.as_str();
                    let filename = PathBuf::from(&input)
                        .file_stem()
                        .and_then(|s| s.to_str())
                        .unwrap_or("file");
                    writeln!(out, "* [{}]({})", filename, url)?;
                }
            }
        }

        Ok(())
    }

    fn config(&self) -> Args {
        let mut a = Args::new();
        a.arg(Arg::with_meta("input", None::<String>, Some("link file path"), true, Some("demo.desktop")));
        a.arg(Arg::with_meta("output", None::<String>, Some("out file path"), true, Some("demo.md")));
        a
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_args_parse() -> Result<()> {
        let a = Args::from_args(vec!["--input=foo.desktop", "--output=out.md"]);
        assert_eq!(a.get_arg_val("input").as_deref(), Some("foo.desktop"));
        assert_eq!(a.get_arg_val("output").as_deref(), Some("out.md"));
        Ok(())
    }
}
