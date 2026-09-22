/// <reference types="vite/client" />

export interface SystemInfo {
  name: string;
  version: string;
  javaVersion: string;
}

export interface ToolInfo {
  id: string;
  name: string;
  category: string;
  description: string;
}

export interface TodoRecord {
  id: string;
  name: string;
  state: string;
  type?: string;
  remark?: string | null;
  createTime?: string;
  start?: string;
  end?: string;
}

export interface NoteRecord {
  id: string;
  content: string;
  date?: string | null;
  state: string;
}

export interface CommandRecord {
  id: string;
  cmd: string;
  tags?: string[] | null;
  alias?: string | null;
  remark?: string | null;
  frequency?: number | null;
  createTime?: string | null;
  updateTime?: string | null;
}

let baseUrl = import.meta.env.DEV ? "/proxy" : "http://localhost:38080";

export function setBackendUrl(url: string) {
  baseUrl = import.meta.env.DEV
    ? url.replace(/\/+$/, "").replace(/^http:\/\/localhost:38080$/, "/proxy")
    : url.replace(/\/+$/, "");
}

export function getBackendUrl() {
  return baseUrl;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    ...init,
  });

  if (!response.ok) {
    throw new Error(`Backend HTTP ${response.status}`);
  }

  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    return response.json() as Promise<T>;
  }

  return response.text() as unknown as T;
}

function normalizeTools(raw: string[]): ToolInfo[] {
  return raw.map((toolId) => ({
    id: toolId,
    name: toolId,
    category: "Tool",
    description: `The ${toolId} tool`,
  }));
}

export const api = {
  systemInfo: async (): Promise<SystemInfo> => {
    await request<string[]>("/");
    return {
      name: "The Source Box",
      version: "local",
      javaVersion: "Java backend ready",
    };
  },
  tools: async (): Promise<ToolInfo[]> => {
    const raw = await request<string[]>("/");
    return normalizeTools(raw);
  },
  todoList: async (query = ""): Promise<TodoRecord[]> => {
    const params = new URLSearchParams({
      state: "todo",
      type: "todo",
    });

    if (query.trim()) {
      params.set("name", query.trim());
    }

    return request<TodoRecord[]>(`/action?${params.toString()}`);
  },
  todoCreate: async (todo: Partial<TodoRecord>): Promise<TodoRecord> => {
    const payload = {
      ...todo,
      state: todo.state ?? "todo",
      type: todo.type ?? "todo",
      start: todo.start ?? formatDateTime(new Date()),
      end: todo.end ?? formatDateTime(new Date()),
    };

    return request<TodoRecord>("/action", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  todoUpdate: async (todo: TodoRecord): Promise<TodoRecord> => {
    return request<TodoRecord>("/action", {
      method: "POST",
      body: JSON.stringify({
        ...todo,
        type: todo.type ?? "todo",
        state: todo.state,
        start: todo.start ?? formatDateTime(new Date()),
        end: todo.end ?? formatDateTime(new Date()),
      }),
    });
  },
  todoDelete: async (id: string): Promise<void> => {
    await request<void>(`/action/${id}`, {
      method: "DELETE",
    });
  },
  noteList: async (): Promise<NoteRecord[]> => {
    return request<NoteRecord[]>("/note/list");
  },
  noteSave: async (note: Partial<NoteRecord>): Promise<NoteRecord> => {
    return request<NoteRecord>("/note/note", {
      method: "POST",
      body: JSON.stringify({
        ...note,
        date: note.date ?? formatDateTime(new Date()),
        state: note.state ?? "NORMAL",
      }),
    });
  },
  noteDelete: async (id: string): Promise<void> => {
    await request<void>(`/note/${id}`, {
      method: "DELETE",
    });
  },
  commandSearch: async (query = ""): Promise<CommandRecord[]> => {
    const params = new URLSearchParams();
    if (query.trim()) {
      params.set("q", query.trim());
    }
    const suffix = params.toString() ? `?${params.toString()}` : "";
    return request<CommandRecord[]>(`/cmd/quick${suffix}`);
  },
  commandSave: async (command: Partial<CommandRecord>): Promise<CommandRecord> => {
    return request<CommandRecord>("/cmd", {
      method: "POST",
      body: JSON.stringify({
        ...command,
        id: command.id ?? undefined,
        alias: command.alias ?? null,
        tags: command.tags ?? [],
        remark: command.remark ?? null,
        frequency: command.frequency ?? 0,
        createTime: command.createTime ?? formatDateTime(new Date()),
        updateTime: command.updateTime ?? formatDateTime(new Date()),
      }),
    });
  },
  commandDelete: async (id: string): Promise<void> => {
    await request<void>(`/cmd/${id}`, {
      method: "DELETE",
    });
  },
  htmlDownload: async (payload: Record<string, string>): Promise<string> => {
    return request<string>("/htmlDown", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  convert: async (payload: Record<string, string>): Promise<string> => {
    return request<string>("/converts", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
};

function formatDateTime(date: Date) {
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}
