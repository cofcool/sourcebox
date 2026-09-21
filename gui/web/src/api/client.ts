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

let baseUrl = "http://127.0.0.1:47831/api";

export function setBackendUrl(url: string) {
  baseUrl = url.replace(/\/+$/, "") + "/api";
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

  return response.json() as Promise<T>;
}

export const api = {
  systemInfo: () => request<SystemInfo>("/system/info"),
  tools: () => request<ToolInfo[]>("/tools"),
};
