package git

import (
	"io"
	"os/exec"
	"path/filepath"
	"slices"
	"sourcebox/tool"
	"strings"
	"text/template"
)

const latestTag = "latest"

const defaultOutputTemplate = `
{{range $k, $v := .}}
{{titleMsg $k}} 
{{range $i, $e := $v}}
{{commitMgs $e}}
{{end}}
{{end}}
`

var customFunMap template.FuncMap = map[string]any{
	"titleMsg":  tag2title,
	"commitMgs": messageStr,
}

func messageStr(c commit) string {
	return "* " + c.message + "(" + c.hash + ")"
}

func tag2title(tag string) string {
	if !(tag == latestTag || strings.HasPrefix(tag, "v") || strings.HasPrefix(tag, "V")) {
		tag = "v" + tag
	}

	return "## " + tag
}

type CommitLog struct {
	config *tool.Config
}

type style string

const (
	simple  style = ""
	angular style = "^(fix|feat|docs|style|refactor|pref|test|chore)\\(.+\\): .+"
)

type commit struct {
	username string
	ref      string
	hash     string
	message  string
}

func (c commit) parseTag() (string, bool) {
	if strings.Contains(c.ref, "tag") {
		for _, t := range strings.Split(
			strings.ReplaceAll(strings.ReplaceAll(c.ref, ")", ""), "(", ""),
			",") {
			if strings.HasPrefix(strings.TrimSpace(t), "tag") {
				return strings.TrimSpace(strings.Split(t, ":")[1]), true
			}
		}
	}
	return "", false
}

func (c *CommitLog) Run() error {

	pathArg, err := c.Config().ReadArg("path")
	outArg, _ := c.Config().ReadArg("out")
	noTag := c.Config().TestArg("noTag", func(arg tool.Arg) bool {
		return arg.Val == "true"
	})
	logId, _ := c.Config().ReadArg("logId")
	user, _ := c.Config().ReadArg("user")
	//tag, _ := c.Config().ReadArg("tag")
	if err != nil {
		return err
	}

	realPath, err := filepath.Abs(pathArg.Val)
	if err != nil {
		return err
	}

	cmd := exec.Command("git", "log", "--format=%an;%d;%h;%s")
	cmd.Dir = realPath

	log, err := cmd.Output()
	if err != nil {
		return err
	}

	commits := parseCommitLog(log, noTag, logId.Val)

	var commitGroup = make(map[string][]commit)
	for _, c2 := range commits {
		if v := user.Val; v != "" && user.Val != c2.username {
			continue
		}

		l, ok := commitGroup[c2.ref]
		if ok {
			commitGroup[c2.ref] = append(l, c2)
		} else {
			commitGroup[c2.ref] = []commit{c2}
		}
	}

	tmpl, err := template.New("default").Funcs(customFunMap).Parse(defaultOutputTemplate)
	if err != nil {
		return err
	}

	return c.config.Context.Write(
		outArg.Val,
		tool.ContextWriteAction{
			Action: func(writer io.Writer) error {
				return tmpl.Execute(writer, commitGroup)
			},
		},
	)
}

func parseCommitLog(log []byte, noTag bool, logId string) []commit {
	var commits []commit
	currentTag := latestTag
	for _, line := range strings.Split(string(log), "\n") {
		cols := strings.Split(line, ";")
		if len(cols) > 3 {
			cm := commit{
				username: cols[0],
				ref:      cols[1],
				hash:     cols[2],
				message:  strings.Join(cols[3:], ""),
			}
			if logId != "" {
				if strings.Contains(cm.message, logId) {
					msg, _ := strings.CutSuffix(cm.message, logId)
					cm.message = msg
				} else {
					continue
				}
			}
			tmpTag, ok := cm.parseTag()
			if !noTag && ok {
				currentTag = tmpTag
			}
			cm.ref = currentTag
			commits = append(commits, cm)
		}
	}
	slices.SortFunc(commits, func(a, b commit) int {
		return strings.Compare(a.ref, b.ref) * -1
	})
	return commits
}

func (c *CommitLog) Init() {
	c.config = &tool.Config{
		Name: "gitCommits2Log",
		Desc: "generate changelog file from git commit log",
		Args: map[string]*tool.Arg{
			"path": {
				Key:      "path",
				Desc:     "project directory, must set this or log",
				Val:      "./",
				Required: false,
				Demo:     "./demo/",
			},
			"log": {
				Key:      "log",
				Desc:     "Git commit log fil",
				Required: false,
				Demo:     "./git-commit-log.txt",
			},
			"noTag": {
				Key:      "noTag",
				Val:      "false",
				Desc:     "if true, read all commit log and write into file",
				Required: false,
			},
			"out": {
				Key:      "out",
				Val:      "./changelog.md",
				Desc:     "generate file output path",
				Required: false,
			},
			"logId": {
				Key:      "logId",
				Demo:     "#log",
				Desc:     "mark the commit history as the changelog",
				Required: false,
			},
			"user": {
				Key:      "user",
				Demo:     "cofcool",
				Desc:     "username filter",
				Required: false,
			},
		},
	}
}

func (c *CommitLog) Config() *tool.Config {
	return c.config
}
