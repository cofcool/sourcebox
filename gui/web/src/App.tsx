import { useEffect, useState } from "react";
import { NavLink, Route, Routes } from "react-router-dom";
import { Activity, Check, FileText, Home as HomeIcon, ListTodo, Search, Settings, Trash2, Wrench } from "lucide-react";
import { api } from "./api/client";
import type { SystemInfo, ToolInfo } from "./api/client";

function Sidebar({ tools }: { tools: ToolInfo[] }) {
  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-icon"><Wrench size={20} /></div>
        <span>Toolbox</span>
      </div>

      <nav>
        <NavLink to="/" end><HomeIcon size={17} />首页</NavLink>
        <NavLink to="/todo"><ListTodo size={17} />待办事项</NavLink>

        {Array.from(new Set(tools.map(t => t.category))).map(category => (
          <div className="nav-group" key={category}>
            <div className="nav-title">{category}</div>
            {tools.filter(t => t.category === category).map(tool => (
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
        {tools.map(tool => (
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
  if (!tool) return <div><h1>工具不存在</h1></div>;

  return (
    <div>
      <h1>{tool.name}</h1>
      <p className="muted">{tool.description}</p>
      <section className="panel tool-placeholder">
        <h2>{tool.name}</h2>
        <p>这里是工具 UI 占位区域。</p>
        <p className="muted">后续可以在这里添加文件选择、输入框、参数、执行进度和输出结果。</p>
        <button>执行</button>
      </section>
    </div>
  );
}

function SettingsPage({ system }: { system?: SystemInfo }) {
  return (
    <div>
      <h1>设置</h1>
      <section className="panel">
        <h2>后端</h2>
        <p>Java Backend: <strong>127.0.0.1:47831</strong></p>
        {system && <p className="muted">Java {system.javaVersion}</p>}
      </section>
    </div>
  );
}

interface TodoItem {
  id: number;
  text: string;
  completed: boolean;
}

const TODO_STORAGE_KEY = "toolbox-todos";

function TodoPage() {
  const [input, setInput] = useState("");
  const [search, setSearch] = useState("");
  const [todos, setTodos] = useState<TodoItem[]>(() => {
    try {
      const stored = localStorage.getItem(TODO_STORAGE_KEY);
      return stored ? JSON.parse(stored) as TodoItem[] : [];
    } catch {
      return [];
    }
  });

  useEffect(() => {
    localStorage.setItem(TODO_STORAGE_KEY, JSON.stringify(todos));
  }, [todos]);

  const filteredTodos = todos.filter(todo => todo.text.toLowerCase().includes(search.toLowerCase()));

  function saveTodo() {
    const text = input.trim();
    if (!text) return;
    setTodos(current => [{ id: Date.now(), text, completed: false }, ...current]);
    setInput("");
  }

  return (
    <div className="todo-page">
      <div className="page-heading">
        <div>
          <h1>待办事项</h1>
          <p className="muted">把要做的事情记下来，专注于下一步。</p>
        </div>
        <div className="todo-count">{todos.filter(todo => !todo.completed).length} 项待完成</div>
      </div>

      <section className="panel todo-composer">
        <label htmlFor="todo-input">新增待办</label>
        <textarea
          id="todo-input"
          value={input}
          onChange={event => setInput(event.target.value)}
          onKeyDown={event => {
            if ((event.ctrlKey || event.metaKey) && event.key === "Enter") saveTodo();
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
            onChange={event => setSearch(event.target.value)}
            placeholder="搜索待办事项"
            aria-label="搜索待办事项"
          />
        </div>
        <button className="save-button" onClick={saveTodo} disabled={!input.trim()}>
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
        ) : filteredTodos.map(todo => (
          <div className={`todo-item${todo.completed ? " completed" : ""}`} key={todo.id}>
            <button
              className="complete-button"
              onClick={() => setTodos(current => current.map(item => item.id === todo.id ? { ...item, completed: !item.completed } : item))}
              aria-label={todo.completed ? "标记为未完成" : "标记为已完成"}
            >
              {todo.completed && <Check size={15} />}
            </button>
            <span>{todo.text}</span>
            <button
              className="delete-button"
              onClick={() => setTodos(current => current.filter(item => item.id !== todo.id))}
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

export default function App() {
  const [tools, setTools] = useState<ToolInfo[]>([]);
  const [system, setSystem] = useState<SystemInfo>();
  const [error, setError] = useState<string>();

  useEffect(() => {
    Promise.all([api.systemInfo(), api.tools()])
      .then(([info, list]) => {
        setSystem(info);
        setTools(list);
      })
      .catch(e => setError(e instanceof Error ? e.message : String(e)));
  }, []);

  return (
    <div className="app">
      <Sidebar tools={tools} />
      <main className="content">
        {error && <div className="error">Java Backend 连接失败：{error}</div>}
        <Routes>
          <Route path="/" element={<Home system={system} tools={tools} />} />
          <Route path="/todo" element={<TodoPage />} />
          <Route path="/tools/:id" element={
            <ToolRoute tools={tools} />
          } />
          <Route path="/settings" element={<SettingsPage system={system} />} />
        </Routes>
      </main>
    </div>
  );
}

function ToolRoute({ tools }: { tools: ToolInfo[] }) {
  const id = location.pathname.split("/").pop();
  return <ToolPage tool={tools.find(t => t.id === id)} />;
}
