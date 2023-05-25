import { defineConfig, loadEnv, resolveBaseUrl, resolveConfig } from 'vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import vue from '@vitejs/plugin-vue'
import VueJsx from "@vitejs/plugin-vue-jsx";
import { fileURLToPath, URL } from "url";
import Compression from "vite-compression-plugin";


export default ({ mode }: { mode: string }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const isProduction = mode === "production";

  return defineConfig({
    base: env.VITE_BASE_URL,
    plugins: [
      vue(),
      VueJsx(),
      Compression(),
      AutoImport({
        resolvers: [ElementPlusResolver()],
      }),
      Components({
        resolvers: [ElementPlusResolver()],
      }),
    ],
    server: {
      port: 3000,
    },
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url)),
      },
    },
    build: {
      outDir: fileURLToPath(
        new URL("../server/src/main/resources/console", import.meta.url)
      ),
      emptyOutDir: true,
      chunkSizeWarningLimit: 2048,
    },
  })
}

