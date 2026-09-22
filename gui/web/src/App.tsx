import { useEffect, useMemo, useState } from "react";
import { NavLink, Route, Routes, useLocation } from "react-router-dom";
import {
  Activity,
  Check,
  Clipboard,
  FileText,
  Home as HomeIcon,
  ListTodo,
  NotebookPen,
  Pencil,
  Plus,
  Save,
  Search,
  Settings,
  Trash2,
  Wrench,
} from "lucide-react";
import { api } from "./api/client";
import type { SystemInfo, ToolInfo } from "./api/client";

interface TodoItem {
  id: string;
  name: string;
  remark?: string | null;
  state: string;
  type?: string;
  createTime?: string;
  start?: string;
  end?: string;
}

interface NoteItem {
  id: string;
  content: string;
  date: string;
  state: string;
}

interface CommandItem {
  id: string;
  cmd: string;
  tags: string[];
}

const TODO_STORAGE_KEY = "toolbox-todos";
const NOTE_STORAGE_KEY = "toolbox-notes";
const COMMAND_STORAGE_KEY = "toolbox-commands";

function readStoredJson<T>(key: string, fallback: T): T {
  if (typeof window === "undefined") return fallback;
  try {
    const value = window.localStorage.getItem(key);
    return value ? (JSON.parse(value) as T) : fallback;
  } catch {
    return fallback;
  }
}

function generateId() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `id-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function Sidebar({ tools }: { tools: ToolInfo[] }) {
  const categories = useMemo(() => Array.from(new Set(tools.map((tool) => tool.category))), [tools]);

  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-icon"><Wrench size={20} /></div>
        <span>Toolbox</span>
      </div>

      <nav>
        <NavLink to="/" end><HomeIcon size={17} />首页</NavLink>

        {categories.map((category) => (
          <div className="nav-group" key={category}>
            <div className="nav-title">{category}</div>
            {tools.filter((tool) => tool.category === category).map((tool) => (
              <NavLink key={tool.id} to={`/tools/${tool.id}`}>
                <Wrench size={15} />{tool.name}
              </NavLink>
            ))}
          </div>
        ))}

        <NavLink to="/settings"><Settings size={17} />设置</NavLink>
      </nav>
    </aside>
  );
}

function Home({ system, tools }: { system?: SystemInfo; tools: ToolInfo[] }) {
  return (
    <div>
      <h1>工具箱</h1>
      <p className="muted">一个基于 Tauri + React + Java 的桌面工具箱。</p>

      <div className="cards">
        {tools.map((tool) => (
          <NavLink className="card" key={tool.id} to={`/tools/${tool.id}`}>
            <Wrench size={22} />
            <strong>{tool.name}</strong>
            <span>{tool.description}</span>
          </NavLink>
        ))}
      </div>

      <section className="panel">
        <h2><Activity size={18} /> 后端状态</h2>
        <div className="status"><span className="dot" /> Java Backend Connected</div>
        {system && <div className="muted">Java {system.javaVersion} · Backend {system.version}</div>}
      </section>
    </div>
  );
}

function ToolPage({ tool }: { tool?: ToolInfo }) {
  if (!tool) {
    return (
      <div>
        <h1>工具不存在</h1>
        <p className="muted">该工具在当前列表中不存在。</p>
      </div>
    );
  }

  switch (tool.id) {
    case "note":
      return <NotePanel />;
    case "todo":
      return <TodoPage />;
    case "cHelper":
      return <CommandHelperPanel />;
    case "clipboard":
      return <ClipboardPanel />;
    case "htmlDown":
      return <HtmlDownPanel />;
    case "converts":
      return <ConvertersPanel />;
    default:
      return (
        <div>
          <h1>{tool.name}</h1>
          <p className="muted">{tool.description}</p>
          <section className="panel tool-placeholder">
            <h2>{tool.name}</h2>
            <p>这里是工具 UI 占位区域。</p>
            <p className="muted">当前已按 KMP 端的布局对齐，后续可继续填充各工具的字段与执行逻辑。</p>
            <button>执行</button>
          </section>
        </div>
      );
  }
}

function SettingsPage({ system }: { system?: SystemInfo }) {
  return (
    <div>
      <h1>设置</h1>
      <section className="panel">
        <h2>后端</h2>
        <p>Java Backend: <strong>localhost:38080</strong></p>
        {system && <p className="muted">Java {system.javaVersion}</p>}
      </section>
    </div>
  );
}

function TodoPage() {
  const [input, setInput] = useState("");
  const [search, setSearch] = useState("");
  const [todos, setTodos] = useState<TodoItem[]>([]);
  const [error, setError] = useState<string>();

  async function loadTodos(query = "") {
    try {
      setError(undefined);
      const list = await api.todoList(query);
      setTodos(list.map((item) => ({
        ...item,
        remark: item.remark ?? "",
        state: item.state ?? "todo",
      })));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  useEffect(() => {
    void loadTodos(search);
  }, [search]);

  async function saveTodo() {
    const text = input.trim();
    if (!text) return;

    try {
      await api.todoCreate({
        name: text,
        remark: "",
        state: "todo",
        type: "todo",
      });
      setInput("");
      await loadTodos(search);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  async function toggleTodo(todo: TodoItem) {
    try {
      const nextState = todo.state === "done" ? "todo" : "done";
      await api.todoUpdate({
        ...todo,
        state: nextState,
        type: "todo",
        end: new Date().toISOString().replace("T", " ").slice(0, 19),
      });
      await loadTodos(search);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  async function deleteTodo(todo: TodoItem) {
    try {
      await api.todoDelete(todo.id);
      await loadTodos(search);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  const filteredTodos = todos;

  return (
    <div className="todo-page">
      <div className="page-heading">
        <div>
          <h1>待办事项</h1>
          <p className="muted">把要做的事情记下来，专注于下一步。</p>
        </div>
        <div className="todo-count">{todos.filter((todo) => todo.state !== "done").length} 项待完成</div>
      </div>

      {error && <div className="error">{error}</div>}

      <section className="panel todo-composer">
        <label htmlFor="todo-input">新增待办</label>
        <textarea
          id="todo-input"
          value={input}
          onChange={(event) => setInput(event.target.value)}
          onKeyDown={(event) => {
            if ((event.ctrlKey || event.metaKey) && event.key === "Enter") void saveTodo();
          }}
          placeholder="今天想完成什么？"
          rows={3}
        />
      </section>

      <div className="todo-toolbar">
        <div className="search-field">
          <Search size={17} />
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="搜索待办事项"
            aria-label="搜索待办事项"
          />
        </div>
        <button className="save-button" onClick={() => void saveTodo()} disabled={!input.trim()}>
          <FileText size={16} />保存待办
        </button>
      </div>

      <section className="todo-list" aria-live="polite">
        {filteredTodos.length === 0 ? (
          <div className="empty-todos">
            <ListTodo size={30} />
            <strong>{search ? "没有找到匹配的待办" : "还没有待办事项"}</strong>
            <span>{search ? "试试其他关键词" : "在上方写下第一件要做的事"}</span>
          </div>
        ) : filteredTodos.map((todo) => (
          <div className={`todo-item${todo.state === "done" ? " completed" : ""}`} key={todo.id}>
            <button
              className="complete-button"
              onClick={() => void toggleTodo(todo)}
              aria-label={todo.state === "done" ? "标记为未完成" : "标记为已完成"}
            >
              {todo.state === "done" && <Check size={15} />}
            </button>
            <span>{todo.name}</span>
            <button
              className="delete-button"
              onClick={() => void deleteTodo(todo)}
              aria-label="删除待办"
            >
              <Trash2 size={16} />
            </button>
          </div>
        ))}
      </section>
    </div>
  );
}

function NotePanel() {
  const [notes, setNotes] = useState<NoteItem[]>([]);
  const [draft, setDraft] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [error, setError] = useState<string>();

  async function loadNotes() {
    try {
      setError(undefined);
      const list = await api.noteList();
      setNotes(list.map((item) => ({
        id: item.id,
        content: item.content,
        date: item.date ?? new Date().toISOString(),
        state: item.state,
      })));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  useEffect(() => {
    void loadNotes();
  }, []);

  async function saveNote() {
    const content = draft.trim();
    if (!content) return;

    try {
      await api.noteSave({
        id: selectedId ?? undefined,
        content,
        date: new Date().toISOString().replace("T", " ").slice(0, 19),
        state: "NORMAL",
      });
      setDraft("");
      setSelectedId(null);
      await loadNotes();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  async function deleteNote() {
    if (!selectedId) return;
    try {
      await api.noteDelete(selectedId);
      setDraft("");
      setSelectedId(null);
      await loadNotes();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  return (
    <div className="tool-page">
      <div className="page-heading">
        <div>
          <h1>便签</h1>
          <p className="muted">对齐 KMP 的记事本交互，快速记录灵感和备忘。</p>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      <section className="panel note-panel">
        <textarea
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="写下一条笔记…"
          rows={6}
        />

        <div className="action-row">
          <button onClick={() => void saveNote()}><Save size={16} />保存</button>
          <button className="secondary" onClick={() => { setDraft(""); setSelectedId(null); }}><Plus size={16} />新建</button>
          <button className="danger" onClick={() => void deleteNote()} disabled={!selectedId}><Trash2 size={16} />删除</button>
        </div>
      </section>

      <section className="panel note-list-section">
        <div className="panel-headline"><NotebookPen size={18} /> 已保存</div>
        <div className="note-list">
          {notes.length === 0 ? (
            <div className="empty-todos"><strong>没有笔记</strong><span>在上方添加第一条便签内容</span></div>
          ) : (
            notes.map((note) => (
              <div
                key={note.id}
                className={`note-item${selectedId === note.id ? " selected" : ""}`}
                onClick={() => { setSelectedId(note.id); setDraft(note.content); }}
              >
                <div className="note-item-head">
                  <span>{new Date(note.date).toLocaleString()}</span>
                  {selectedId === note.id && <Pencil size={14} />}
                </div>
                <p>{note.content}</p>
              </div>
            ))
          )}
        </div>
      </section>
    </div>
  );
}

function CommandHelperPanel() {
  const [commands, setCommands] = useState<CommandItem[]>([]);
  const [query, setQuery] = useState("");
  const [draftId, setDraftId] = useState("");
  const [draftCmd, setDraftCmd] = useState("");
  const [draftTags, setDraftTags] = useState("");
  const [error, setError] = useState<string>();

  async function loadCommands(search = "") {
    try {
      setError(undefined);
      const list = await api.commandSearch(search);
      setCommands(list.map((item) => ({
        id: item.id,
        cmd: item.cmd,
        tags: item.tags ?? [],
      })));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  useEffect(() => {
    void loadCommands(query);
  }, [query]);

  async function saveCommand() {
    const cmd = draftCmd.trim();
    if (!cmd) return;

    try {
      await api.commandSave({
        id: draftId || undefined,
        cmd,
        tags: draftTags.split(",").map((tag) => tag.trim()).filter(Boolean),
        alias: null,
        remark: null,
        frequency: 0,
      });
      setDraftId("");
      setDraftCmd("");
      setDraftTags("");
      await loadCommands(query);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  async function removeCommand(id: string) {
    try {
      await api.commandDelete(id);
      await loadCommands(query);
      if (draftId === id) {
        setDraftId("");
        setDraftCmd("");
        setDraftTags("");
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  function fillEdit(item: CommandItem) {
    setDraftId(item.id);
    setDraftCmd(item.cmd);
    setDraftTags(item.tags.join(", "));
  }

  return (
    <div className="tool-page">
      <div className="page-heading">
        <div>
          <h1>命令助手</h1>
          <p className="muted">参考 KMP 的命令入口，快速管理常用命令与标签。</p>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      <section className="panel helper-panel">
        <div className="helper-search">
          <Search size={17} />
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索命令或标签" aria-label="搜索命令" />
        </div>

        <div className="helper-form">
          <input value={draftCmd} onChange={(event) => setDraftCmd(event.target.value)} placeholder="命令内容" />
          <input value={draftTags} onChange={(event) => setDraftTags(event.target.value)} placeholder="标签, 以逗号分隔" />
          <div className="action-row compact">
            <button onClick={() => void saveCommand()}><Save size={16} />{draftId ? "保存" : "新增"}</button>
            <button className="secondary" onClick={() => { setDraftId(""); setDraftCmd(""); setDraftTags(""); }}><Plus size={16} />重置</button>
          </div>
        </div>
      </section>

      <section className="panel helper-table-wrap">
        <table className="helper-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Command</th>
              <th>Tags</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {commands.length === 0 ? (
              <tr>
                <td colSpan={4} className="empty-row">没有匹配命令</td>
              </tr>
            ) : commands.map((item) => (
              <tr key={item.id}>
                <td>{item.id.slice(0, 8)}</td>
                <td>{item.cmd}</td>
                <td>{item.tags.length ? item.tags.join(", ") : "-"}</td>
                <td>
                  <div className="table-actions">
                    <button className="secondary small" onClick={() => fillEdit(item)}><Pencil size={14} />编辑</button>
                    <button className="danger small" onClick={() => void removeCommand(item.id)}><Trash2 size={14} />删除</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

function ClipboardPanel() {
  const [text, setText] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!navigator.clipboard) return;
    navigator.clipboard.readText().then((value) => setText(value)).catch(() => setText(""));
  }, []);

  async function copyText() {
    if (!text) return;
    try {
      await navigator.clipboard.writeText(text);
      setMessage("已复制到剪贴板");
    } catch {
      setMessage("当前浏览器不支持写入剪贴板");
    }
  }

  return (
    <div className="tool-page">
      <div className="page-heading">
        <div>
          <h1>剪贴板</h1>
          <p className="muted">对齐 KMP 的剪贴板查看逻辑，快速读取和复制文本。</p>
        </div>
      </div>

      <section className="panel clipboard-panel">
        <div className="clipboard-head">
          <Clipboard size={18} />
          <strong>剪贴板内容</strong>
        </div>
        <textarea value={text} onChange={(event) => setText(event.target.value)} rows={8} />
        <div className="action-row">
          <button onClick={copyText}>复制</button>
          <button className="secondary" onClick={() => navigator.clipboard?.readText().then(setText).catch(() => setText(""))}>刷新</button>
        </div>
        {message && <div className="helper-message">{message}</div>}
      </section>
    </div>
  );
}

function HtmlDownPanel() {
  const [form, setForm] = useState({
    url: "",
    proxy: "",
    filter: "",
    depth: "1",
    out: "/tmp/html",
    clean: "true",
    cleanexp: "",
    webDriver: "/usr/local/bin/chromedriver",
    hrefFilter: "",
  });
  const [result, setResult] = useState<string[]>([]);
  const [error, setError] = useState<string>();
  const [loading, setLoading] = useState(false);

  function updateField(field: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function submit() {
    if (!form.url.trim()) {
      setError("请输入页面地址");
      return;
    }

    setLoading(true);
    setError(undefined);

    try {
      const payload = {
        ...form,
        debug: "true",
      };
      const res = await api.htmlDownload(payload);
      setResult((current) => [res, ...current].slice(0, 20));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="tool-page">
      <div className="page-heading">
        <div>
          <h1>网页下载</h1>
          <p className="muted">参考 KMP 的 HtmlDown，按 URL 和过滤条件抓取页面内容。</p>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      <section className="panel helper-panel">
        <div className="helper-form">
          <input value={form.url} onChange={(event) => updateField("url", event.target.value)} placeholder="URL" />
          <div className="two-column-grid">
            <input value={form.depth} onChange={(event) => updateField("depth", event.target.value)} placeholder="depth" />
            <input value={form.out} onChange={(event) => updateField("out", event.target.value)} placeholder="output" />
          </div>
          <div className="two-column-grid">
            <input value={form.proxy} onChange={(event) => updateField("proxy", event.target.value)} placeholder="proxy" />
            <input value={form.filter} onChange={(event) => updateField("filter", event.target.value)} placeholder="filter" />
          </div>
          <div className="two-column-grid">
            <input value={form.clean} onChange={(event) => updateField("clean", event.target.value)} placeholder="clean" />
            <input value={form.cleanexp} onChange={(event) => updateField("cleanexp", event.target.value)} placeholder="cleanexp" />
          </div>
          <div className="two-column-grid">
            <input value={form.hrefFilter} onChange={(event) => updateField("hrefFilter", event.target.value)} placeholder="hrefFilter" />
            <input value={form.webDriver} onChange={(event) => updateField("webDriver", event.target.value)} placeholder="webDriver" />
          </div>
          <div className="action-row compact">
            <button onClick={() => void submit()} disabled={loading}>{loading ? "下载中..." : "Download"}</button>
          </div>
        </div>
      </section>

      <section className="panel">
        <h2>结果</h2>
        {result.length === 0 ? (
          <div className="empty-todos"><strong>暂无下载结果</strong><span>执行后会显示返回信息</span></div>
        ) : (
          <pre className="result-box">{result.join("\n")}</pre>
        )}
      </section>
    </div>
  );
}

function ConvertersPanel() {
  return <ConvsPanelFromBackend />;
}

function ConvsOperationList({ selected, onToggle }: { selected: string[]; onToggle: (name: string) => void }) {
  const options = ["md5", "now", "hdate", "timesp", "upper", "lower", "replace", "hex", "dataunit", "base64", "urlEncode", "random"];

  return (
    <div className="chip-list">
      {options.map((option) => (
        <button
          key={option}
          className={`chip ${selected.includes(option) ? "selected" : ""}`}
          onClick={() => onToggle(option)}
          type="button"
        >
          {option}
        </button>
      ))}
    </div>
  );
}

function ConversionsResult({ text, setText, onConvert }: { text: string; setText: (value: string) => void; onConvert: () => void }) {
  return (
    <section className="panel">
      <h2>转换</h2>
      <textarea value={text} onChange={(event) => setText(event.target.value)} rows={6} placeholder="输入文本" />
      <div className="action-row compact">
        <button onClick={onConvert}>Convert</button>
      </div>
    </section>
  );
}

function ConvsPanelFromBackend() {
  const [input, setInput] = useState("");
  const [output, setOutput] = useState("");
  const [selected, setSelected] = useState<string[]>(["upper"]);
  const [oldText, setOldText] = useState("");
  const [newText, setNewText] = useState("");
  const [mode, setMode] = useState("en");
  const [error, setError] = useState<string>();
  const [loading, setLoading] = useState(false);

  function toggleOperation(name: string) {
    setSelected((current) => current.includes(name)
      ? current.filter((item) => item !== name)
      : [...current, name]);
  }

  async function runConvert() {
    const first = selected[0];
    if (!first) {
      setError("请选择至少一个转换操作");
      return;
    }

    const body: Record<string, string> = {
      cmd: first,
      in: input,
      pipeline: selected.slice(1).join(" | "),
    };

    if (first === "replace" || selected.includes("replace")) {
      body.old = oldText;
      body.new = newText;
    }
    if (selected.includes("urlEncode")) {
      body.utype = mode;
    }
    if (selected.includes("base64")) {
      body.btype = mode;
    }

    setLoading(true);
    setError(undefined);

    try {
      const res = await api.convert(body);
      setOutput(res);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="tool-page">
      <div className="page-heading">
        <div>
          <h1>文本转换</h1>
          <p className="muted">对应 KMP 的 converts 工具，支持大小写、编码、时间、URL 等处理。</p>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      <section className="panel">
        <ConvsOperationList selected={selected} onToggle={toggleOperation} />
      </section>

      <section className="panel helper-panel">
        <div className="helper-form">
          {(selected.includes("replace") || selected[0] === "replace") && (
            <div className="two-column-grid">
              <input value={oldText} onChange={(event) => setOldText(event.target.value)} placeholder="old" />
              <input value={newText} onChange={(event) => setNewText(event.target.value)} placeholder="new" />
            </div>
          )}
          {(selected.includes("urlEncode") || selected.includes("base64")) && (
            <select value={mode} onChange={(event) => setMode(event.target.value)}>
              <option value="en">en</option>
              <option value="de">de</option>
            </select>
          )}
          <textarea value={input} onChange={(event) => setInput(event.target.value)} rows={6} placeholder="输入待处理文本" />
          <div className="action-row compact">
            <button onClick={() => void runConvert()} disabled={loading}>{loading ? "处理中..." : "Convert"}</button>
          </div>
        </div>
      </section>

      <section className="panel">
        <h2>输出</h2>
        <pre className="result-box">{output || "暂无输出"}</pre>
      </section>
    </div>
  );
}

function ConversionsPanel() {
  return <ConvsPanelFromBackend />;
}

export default function App() {
  const [tools, setTools] = useState<ToolInfo[]>([]);
  const [system, setSystem] = useState<SystemInfo>();
  const [error, setError] = useState<string>();
  const location = useLocation();

  useEffect(() => {
    Promise.all([api.systemInfo(), api.tools()])
      .then(([info, list]) => {
        setSystem(info);
        setTools(list);
      })
      .catch((e) => setError(e instanceof Error ? e.message : String(e)));
  }, []);

  return (
    <div className="app">
      <Sidebar tools={tools} />
      <main className="content">
        {error && <div className="error">Java Backend 连接失败：{error}</div>}
        <Routes>
          <Route path="/" element={<Home system={system} tools={tools} />} />
          <Route path="/todo" element={<TodoPage />} />
          <Route path="/tools/:id" element={<ToolRoute tools={tools} path={location.pathname} />} />
          <Route path="/settings" element={<SettingsPage system={system} />} />
        </Routes>
      </main>
    </div>
  );
}

function ToolRoute({ tools, path }: { tools: ToolInfo[]; path: string }) {
  const current = path.split("/").pop();
  return <ToolPage tool={tools.find((tool) => tool.id === current)} />;
}
