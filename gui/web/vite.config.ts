import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  clearScreen: false,
  server: {
    port: 1420,
    strictPort: true,
    proxy: {
      "/proxy": {
        target: "http://localhost:38080",
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/proxy/, "") || "/",
      },
    },
  },
});
