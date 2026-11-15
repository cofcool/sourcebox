import { defineConfig } from "mygo-cli";

export default defineConfig({
  name: "TheSourceBox",
  slug: "thesourcebox",
  identifier: "net.cofcool.sourcebox.desktop",
  version: "0.1.0",
  copyright: "© 2026 TheSourceBox",
  devUrl: "http://localhost:5173",
  devCommand: "bun run dev:web",
  buildCommand: "bun run build:web",
  frontendDist: "dist",
  bindings: "src/mygo.ts",
  out: "build",
});
