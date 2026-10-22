import {useEffect, useMemo, useState} from "react";
import {NavLink, Route, Routes, useLocation} from "react-router-dom";
import {
    Activity,
    Check,
    Clipboard,
    FileText,
    Home as HomeIcon,
    ListTodo,
    MessageCircle,
    NotebookPen,
    Pencil,
    Plus,
    Save,
    Search,
    Settings,
    Trash2,
    Wrench,
    X,
} from "lucide-react";
import type {ActionComment, ActionRecord, SystemInfo, TimerStatistics, ToolInfo} from "./api/client";
import {api} from "./api/client";
import {invoke, isTauri} from "@tauri-apps/api/core";

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
        <span>TheSourceBox</span>
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
    case "hisRecord":
      return <RecordBoard />;
    case "cHelper":
      return <CommandHelperPanel />;
    case "clipboard":
      return <ClipboardPanel />;
    case "timer":
      return <TimerPage />;
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

function RecordBoard() {
  const [records, setRecords] = useState<ActionRecord[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [title, setTitle] = useState("");
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<ActionRecord>();
  const [comments, setComments] = useState<ActionComment[]>([]);
  const [reply, setReply] = useState("");
  const [error, setError] = useState<string>();
  const [saving, setSaving] = useState(false);

  async function loadRecords() {
    try {
      setRecords(await api.recordList());
      setError(undefined);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  useEffect(() => {
    void loadRecords();
    api.actionTypes()
      .then((result) => setCategories(result.types.filter((item) => item.type === "category").map((item) => item.name)))
      .catch(() => setCategories([]));
  }, []);

  async function addRecord(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const name = title.trim();
    if (!name) return;
    setSaving(true);
    try {
      const now = formatRecordDateTime(new Date());
      await api.recordSave({ id: generateId().replace(/-/g, ""), name, state: "todo", start: now, end: null, duration: 0 });
      setTitle("");
      await loadRecords();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  }

  async function openRecord(record: ActionRecord) {
    setSelected(record);
    setReply("");
    try {
      setComments(await api.recordComments(record.id));
    } catch (e) {
      setComments([]);
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  async function saveRecord(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected) return;
    setSaving(true);
    try {
      const saved = await api.recordSave(selected);
      setSelected(saved);
      await loadRecords();
      setError(undefined);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  }

  async function addReply(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected || !reply.trim()) return;
    setSaving(true);
    try {
      const saved = await api.recordCommentAdd(selected.id, reply.trim());
      setComments((current) => [...current, saved]);
      setReply("");
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  }

  const filtered = records.filter((record) => {
    const text = [record.name, record.remark, record.labels, record.category].filter(Boolean).join(" ").toLocaleLowerCase();
    return text.includes(query.trim().toLocaleLowerCase());
  });

  return (
    <div className="tool-page record-board">
      <div className="page-heading">
        <div>
          <h1>历史记录</h1>
          <p className="muted">记录事项、整理进度，并在卡片下持续补充回复。</p>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      <form className="panel record-create" onSubmit={(event) => void addRecord(event)}>
        <input
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="卡片名称"
          aria-label="卡片名称"
        />
        <button type="submit" disabled={!title.trim() || saving}>添加卡片</button>
      </form>

      <div className="record-search">
        <Search size={17} />
        <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索卡片名称、备注、标签或类别" aria-label="搜索卡片" />
        <span>{filtered.length}</span>
      </div>

      <section className="record-grid" aria-live="polite">
        {filtered.length === 0 ? (
          <div className="record-empty">{query ? "没有匹配的卡片" : "还没有卡片，添加一条记录开始使用"}</div>
        ) : filtered.map((record) => (
          <button className="record-card" key={record.id} onClick={() => void openRecord(record)}>
            <strong>{record.name}</strong>
            {record.remark && <span className="record-card-remark">{record.remark}</span>}
            <div className="record-card-tags">
              {record.category && <span className="record-category">{record.category}</span>}
              {(record.labels ?? "").split(",").map((label) => label.trim()).filter(Boolean).map((label) => <span className="record-label" key={label}>{label}</span>)}
            </div>
            <span className="record-card-date">{record.start ? record.start.slice(0, 10) : "未设置日期"}</span>
          </button>
        ))}
      </section>

      {selected && (
        <div className="record-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setSelected(undefined); }}>
          <section className="record-modal" role="dialog" aria-modal="true" aria-labelledby="record-modal-title">
            <header className="record-modal-header">
              <div>
                <h2 id="record-modal-title">编辑卡片</h2>
                <span className="muted">更新信息并查看回复</span>
              </div>
              <button className="icon-button" type="button" onClick={() => setSelected(undefined)} aria-label="关闭"><X size={19} /></button>
            </header>

            <div className="record-modal-columns">
              <form className="record-details" onSubmit={(event) => void saveRecord(event)}>
                <label>卡片名称
                  <input value={selected.name ?? ""} onChange={(event) => setSelected({ ...selected, name: event.target.value })} required />
                </label>
                <label>备注信息
                  <textarea value={selected.remark ?? ""} onChange={(event) => setSelected({ ...selected, remark: event.target.value })} rows={4} />
                </label>
                <label>标签
                  <input value={selected.labels ?? ""} onChange={(event) => setSelected({ ...selected, labels: event.target.value })} placeholder="多个标签以逗号分隔" />
                </label>
                <label>类别
                  <input list="record-category-options" value={selected.category ?? ""} onChange={(event) => setSelected({ ...selected, category: event.target.value })} />
                  <datalist id="record-category-options">{categories.map((category) => <option value={category} key={category} />)}</datalist>
                </label>
                <div className="record-date-fields">
                  <label>开始时间
                    <input type="datetime-local" required value={dateTimeInput(selected.start)} onChange={(event) => setSelected({ ...selected, start: fromDateTimeInput(event.target.value) })} />
                  </label>
                  <label>结束时间
                    <input type="datetime-local" value={dateTimeInput(selected.end)} onChange={(event) => setSelected({ ...selected, end: fromDateTimeInput(event.target.value) })} />
                  </label>
                </div>
                <label>持续时长（分钟）
                  <input
                    type="number"
                    min="0"
                    value={selected.duration == null ? "" : Math.round(selected.duration / 60)}
                    onChange={(event) => setSelected({ ...selected, duration: event.target.value === "" ? null : Number(event.target.value) * 60 })}
                  />
                </label>
                <div className="record-detail-actions">
                  <button type="submit" disabled={saving || !selected.name.trim()}>{saving ? "保存中..." : "保存信息"}</button>
                </div>
              </form>

              <section className="record-replies">
                <h3><MessageCircle size={17} /> 回复</h3>
                <form className="record-reply-form" onSubmit={(event) => void addReply(event)}>
                  <textarea value={reply} onChange={(event) => setReply(event.target.value)} rows={3} placeholder="添加回复..." />
                  <button type="submit" disabled={saving || !reply.trim()}>添加回复</button>
                </form>
                <div className="record-reply-list">
                  {comments.length === 0 ? <div className="record-reply-empty">还没有回复</div> : comments.map((comment) => (
                    <article className="record-reply" key={comment.id}>
                      <p>{comment.content}</p>
                      <time>{comment.createTime ?? ""}</time>
                    </article>
                  ))}
                </div>
              </section>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

function dateTimeInput(value?: string | null) {
  return value ? value.replace(" ", "T").slice(0, 16) : "";
}

function fromDateTimeInput(value: string) {
  return value ? `${value.replace("T", " ")}:00` : null;
}

function formatRecordDateTime(date: Date) {
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function TimerPage() {
  const savedDurations = readStoredJson("toolbox-timer-settings", { workMinutes: 40, breakMinutes: 5 });
  const [workMinutes, setWorkMinutes] = useState(String(savedDurations.workMinutes));
  const [breakMinutes, setBreakMinutes] = useState(String(savedDurations.breakMinutes));
  const [durations, setDurations] = useState(savedDurations);
  const [phase, setPhase] = useState<"work" | "break">("work");
  const [timerState, setTimerState] = useState<"stopped" | "running" | "paused">("stopped");
  const [remaining, setRemaining] = useState(durations.workMinutes * 60);
  const [deadline, setDeadline] = useState<number>();
  const [breakOverlay, setBreakOverlay] = useState(false);
  const [statistics, setStatistics] = useState<TimerStatistics[]>([]);
  const [error, setError] = useState<string>();
  const [loadingStats, setLoadingStats] = useState(false);

  useEffect(() => {
    if (timerState !== "running" || deadline === undefined) return;

    const tick = () => {
      const secondsLeft = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
      setRemaining(secondsLeft);
      if (secondsLeft > 0) return;

      if (phase === "work") {
        void saveTimerRecord(durations.workMinutes * 60, "finish");
        setPhase("break");
        setBreakOverlay(true);
        setRemaining(durations.breakMinutes * 60);
        setDeadline(Date.now() + durations.breakMinutes * 60_000);
      } else {
        setPhase("work");
        setRemaining(durations.workMinutes * 60);
        setDeadline(Date.now() + durations.workMinutes * 60_000);
      }
    };

    tick();
    const interval = window.setInterval(tick, 250);
    return () => window.clearInterval(interval);
  }, [deadline, durations, phase, timerState]);

  function saveTimerRecord(durationSeconds: number, remark: string) {
    if (durationSeconds <= 0) return;
    const end = new Date();
    const start = new Date(end.getTime() - durationSeconds * 1000);
    const format = (date: Date) => {
      const pad = (value: number) => String(value).padStart(2, "0");
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
    };

    void api.timerRecord({
      name: `workTime-${Math.floor(end.getTime() / 1000)}`,
      state: "done",
      type: "timer",
      remark,
      start: format(start),
      end: format(end),
      duration: durationSeconds,
    }).catch((e) => setError(e instanceof Error ? e.message : String(e)));
  }

  function startOrToggle() {
    if (timerState === "stopped") {
      setPhase("work");
      setRemaining(durations.workMinutes * 60);
      setDeadline(Date.now() + durations.workMinutes * 60_000);
      setTimerState("running");
      return;
    }

    if (timerState === "running") {
      setRemaining(Math.max(0, Math.ceil(((deadline ?? Date.now()) - Date.now()) / 1000)));
      setDeadline(undefined);
      setTimerState("paused");
      return;
    }

    setDeadline(Date.now() + remaining * 1000);
    setTimerState("running");
  }

  function resetTimer() {
    if (phase === "work" && timerState !== "stopped") {
      saveTimerRecord(durations.workMinutes * 60 - remaining, "reset");
    }
    setTimerState("stopped");
    setPhase("work");
    setDeadline(undefined);
    setRemaining(durations.workMinutes * 60);
    setBreakOverlay(false);
  }

  function applyDurations() {
    const work = Number.parseInt(workMinutes, 10);
    const rest = Number.parseInt(breakMinutes, 10);
    if (!Number.isInteger(work) || !Number.isInteger(rest) || work < 1 || rest < 1) {
      setError("工作和休息时长必须是大于 0 的整数分钟");
      return;
    }
    const next = { workMinutes: work, breakMinutes: rest };
    setDurations(next);
    window.localStorage.setItem("toolbox-timer-settings", JSON.stringify(next));
    setRemaining(work * 60);
    setError(undefined);
  }

  async function loadStatistics() {
    setLoadingStats(true);
    setError(undefined);
    try {
      setStatistics(await api.timerStatistics());
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoadingStats(false);
    }
  }

  const displayTime = `${String(Math.floor(remaining / 60)).padStart(2, "0")}:${String(remaining % 60).padStart(2, "0")}`;

  useEffect(() => {
    if (!isTauri()) return;
    const phaseLabel = phase === "work" ? "Work" : "Break";
    const stateLabel = timerState === "running" ? "" : timerState === "paused" ? " (paused)" : " (stopped)";
    void invoke("set_tray_timer", { text: `${phaseLabel}: ${displayTime}${stateLabel}` }).catch((e) => {
      console.error("Failed to update timer tray tooltip", e);
    });
  }, [displayTime, phase, timerState]);

  return (
    <div className="tool-page timer-page">
      <div className="page-heading">
        <div>
          <h1>番茄计时</h1>
          <p className="muted">专注工作，按时休息。</p>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      <section className={`panel timer-panel ${phase === "break" ? "on-break" : ""}`}>
        <div className="timer-phase">{phase === "work" ? "Work Time" : "Break Time"}</div>
        <div className="timer-clock" aria-live="polite">{displayTime}</div>
        <div className="timer-durations">
          <label>工作时间（分钟）
            <input type="number" min="1" step="1" value={workMinutes} disabled={timerState !== "stopped"} onChange={(event) => setWorkMinutes(event.target.value)} />
          </label>
          <label>休息时间（分钟）
            <input type="number" min="1" step="1" value={breakMinutes} disabled={timerState !== "stopped"} onChange={(event) => setBreakMinutes(event.target.value)} />
          </label>
          <button className="secondary" onClick={applyDurations} disabled={timerState !== "stopped"}>应用</button>
        </div>
        <div className="action-row timer-actions">
          <button onClick={startOrToggle}>
            {timerState === "stopped" ? "开始" : timerState === "running" ? "暂停" : "继续"}
          </button>
          <button className="secondary" onClick={resetTimer}>重置</button>
        </div>
      </section>

      <section className="panel timer-statistics">
        <div className="timer-statistics-heading">
          <h2>专注统计</h2>
          <button className="secondary" onClick={() => void loadStatistics()} disabled={loadingStats}>
            {loadingStats ? "加载中..." : "Statistics"}
          </button>
        </div>
        <div className="timer-table-wrap">
          <table className="helper-table">
            <thead><tr><th>日期</th><th>次数</th><th>工作时长</th></tr></thead>
            <tbody>
              {statistics.length === 0 ? (
                <tr><td colSpan={3} className="empty-row">暂无统计数据</td></tr>
              ) : statistics.map((item) => (
                <tr key={item.day}>
                  <td>{item.day}</td>
                  <td>{item.cnt}</td>
                  <td>{formatDuration(item.total)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {breakOverlay && (
        <div className="timer-break-overlay">
          <div className="timer-break-content">
            <span>休息时间</span>
            <strong>{displayTime}</strong>
            <button onClick={() => setBreakOverlay(false)} disabled={phase === "break"}>开始工作</button>
          </div>
        </div>
      )}
    </div>
  );
}

function formatDuration(totalSeconds: number) {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return [hours && `${hours}小时`, minutes && `${minutes}分钟`, seconds && `${seconds}秒`].filter(Boolean).join(" ") || "0秒";
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
