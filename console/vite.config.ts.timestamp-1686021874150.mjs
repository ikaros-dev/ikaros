// vite.config.ts
import { defineConfig, loadEnv } from "file:///C:/Develop/GitRepos/ikaros-dev/ikaros/console/node_modules/.pnpm/vite@4.1.1_@types+node@18.11.19_sass@1.56.2/node_modules/vite/dist/node/index.js";
import path from "path";
import vue from "file:///C:/Develop/GitRepos/ikaros-dev/ikaros/console/node_modules/.pnpm/@vitejs+plugin-vue@4.0.0_vite@4.1.1_vue@3.2.47/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import VueJsx from "file:///C:/Develop/GitRepos/ikaros-dev/ikaros/console/node_modules/.pnpm/@vitejs+plugin-vue-jsx@3.0.0_vite@4.1.1_vue@3.2.47/node_modules/@vitejs/plugin-vue-jsx/dist/index.mjs";
import VueI18nPlugin from "file:///C:/Develop/GitRepos/ikaros-dev/ikaros/console/node_modules/.pnpm/@intlify+unplugin-vue-i18n@0.11.0_vue-i18n@9.2.2/node_modules/@intlify/unplugin-vue-i18n/lib/vite.mjs";
import { fileURLToPath, URL } from "url";
import Compression from "file:///C:/Develop/GitRepos/ikaros-dev/ikaros/console/node_modules/.pnpm/vite-plugin-compression2@0.9.1/node_modules/vite-plugin-compression2/dist/index.mjs";
import eslintPlugin from "file:///C:/Develop/GitRepos/ikaros-dev/ikaros/console/node_modules/.pnpm/vite-plugin-eslint@1.8.1_eslint@8.28.0_vite@4.1.1/node_modules/vite-plugin-eslint/dist/index.mjs";
import AutoImport from "file:///C:/Develop/GitRepos/ikaros-dev/ikaros/console/node_modules/.pnpm/unplugin-auto-import@0.16.2_@vueuse+core@9.13.0/node_modules/unplugin-auto-import/dist/vite.js";
import Components from "file:///C:/Develop/GitRepos/ikaros-dev/ikaros/console/node_modules/.pnpm/unplugin-vue-components@0.24.1_vue@3.2.47/node_modules/unplugin-vue-components/dist/vite.mjs";
import Icons from "file:///C:/Develop/GitRepos/ikaros-dev/ikaros/console/node_modules/.pnpm/unplugin-icons@0.16.2/node_modules/unplugin-icons/dist/vite.mjs";
import { ElementPlusResolver } from "file:///C:/Develop/GitRepos/ikaros-dev/ikaros/console/node_modules/.pnpm/unplugin-vue-components@0.24.1_vue@3.2.47/node_modules/unplugin-vue-components/dist/resolvers.mjs";
var __vite_injected_original_dirname = "C:\\Develop\\GitRepos\\ikaros-dev\\ikaros\\console";
var __vite_injected_original_import_meta_url = "file:///C:/Develop/GitRepos/ikaros-dev/ikaros/console/vite.config.ts";
var vite_config_default = ({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  return defineConfig({
    base: env.VITE_BASE_URL,
    plugins: [
      vue(),
      VueJsx(),
      eslintPlugin(),
      Compression(),
      Icons(),
      AutoImport({
        dts: true,
        eslintrc: {
          enabled: true
        },
        imports: ["vue", "vue-router", "@vueuse/core"],
        resolvers: [ElementPlusResolver()]
      }),
      Components({
        resolvers: [ElementPlusResolver()]
      }),
      VueI18nPlugin({
        include: [path.resolve(__vite_injected_original_dirname, "./src/locales/*.yaml")]
      })
    ],
    server: {
      port: 3e3,
      proxy: {
        // 匹配所有URL路径不包含 /console/的路径的请求，代理到服务端
        "^(?!^/console/).*": env.VITE_SERVER_URL
      }
    },
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", __vite_injected_original_import_meta_url))
      }
    },
    build: {
      outDir: fileURLToPath(
        new URL("../server/src/main/resources/console", __vite_injected_original_import_meta_url)
      ),
      emptyOutDir: true,
      chunkSizeWarningLimit: 2048
    }
  });
};
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcudHMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJDOlxcXFxEZXZlbG9wXFxcXEdpdFJlcG9zXFxcXGlrYXJvcy1kZXZcXFxcaWthcm9zXFxcXGNvbnNvbGVcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkM6XFxcXERldmVsb3BcXFxcR2l0UmVwb3NcXFxcaWthcm9zLWRldlxcXFxpa2Fyb3NcXFxcY29uc29sZVxcXFx2aXRlLmNvbmZpZy50c1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vQzovRGV2ZWxvcC9HaXRSZXBvcy9pa2Fyb3MtZGV2L2lrYXJvcy9jb25zb2xlL3ZpdGUuY29uZmlnLnRzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnLCBsb2FkRW52IH0gZnJvbSAndml0ZSc7XHJcbmltcG9ydCBwYXRoIGZyb20gJ3BhdGgnO1xyXG5pbXBvcnQgdnVlIGZyb20gJ0B2aXRlanMvcGx1Z2luLXZ1ZSc7XHJcbmltcG9ydCBWdWVKc3ggZnJvbSAnQHZpdGVqcy9wbHVnaW4tdnVlLWpzeCc7XHJcbmltcG9ydCBWdWVJMThuUGx1Z2luIGZyb20gJ0BpbnRsaWZ5L3VucGx1Z2luLXZ1ZS1pMThuL3ZpdGUnO1xyXG5pbXBvcnQgeyBmaWxlVVJMVG9QYXRoLCBVUkwgfSBmcm9tICd1cmwnO1xyXG5pbXBvcnQgQ29tcHJlc3Npb24gZnJvbSAndml0ZS1wbHVnaW4tY29tcHJlc3Npb24yJztcclxuaW1wb3J0IGVzbGludFBsdWdpbiBmcm9tICd2aXRlLXBsdWdpbi1lc2xpbnQnO1xyXG5pbXBvcnQgQXV0b0ltcG9ydCBmcm9tICd1bnBsdWdpbi1hdXRvLWltcG9ydC92aXRlJztcclxuaW1wb3J0IENvbXBvbmVudHMgZnJvbSAndW5wbHVnaW4tdnVlLWNvbXBvbmVudHMvdml0ZSc7XHJcbmltcG9ydCBJY29ucyBmcm9tICd1bnBsdWdpbi1pY29ucy92aXRlJztcclxuaW1wb3J0IHsgRWxlbWVudFBsdXNSZXNvbHZlciB9IGZyb20gJ3VucGx1Z2luLXZ1ZS1jb21wb25lbnRzL3Jlc29sdmVycyc7XHJcblxyXG5leHBvcnQgZGVmYXVsdCAoeyBtb2RlIH06IHsgbW9kZTogc3RyaW5nIH0pID0+IHtcclxuXHRjb25zdCBlbnYgPSBsb2FkRW52KG1vZGUsIHByb2Nlc3MuY3dkKCksICcnKTtcclxuXHJcblx0cmV0dXJuIGRlZmluZUNvbmZpZyh7XHJcblx0XHRiYXNlOiBlbnYuVklURV9CQVNFX1VSTCxcclxuXHRcdHBsdWdpbnM6IFtcclxuXHRcdFx0dnVlKCksXHJcblx0XHRcdFZ1ZUpzeCgpLFxyXG5cdFx0XHRlc2xpbnRQbHVnaW4oKSxcclxuXHRcdFx0Q29tcHJlc3Npb24oKSxcclxuXHRcdFx0SWNvbnMoKSxcclxuXHRcdFx0QXV0b0ltcG9ydCh7XHJcblx0XHRcdFx0ZHRzOiB0cnVlLFxyXG5cdFx0XHRcdGVzbGludHJjOiB7XHJcblx0XHRcdFx0XHRlbmFibGVkOiB0cnVlLFxyXG5cdFx0XHRcdH0sXHJcblx0XHRcdFx0aW1wb3J0czogWyd2dWUnLCAndnVlLXJvdXRlcicsICdAdnVldXNlL2NvcmUnXSxcclxuXHRcdFx0XHRyZXNvbHZlcnM6IFtFbGVtZW50UGx1c1Jlc29sdmVyKCldLFxyXG5cdFx0XHR9KSxcclxuXHRcdFx0Q29tcG9uZW50cyh7XHJcblx0XHRcdFx0cmVzb2x2ZXJzOiBbRWxlbWVudFBsdXNSZXNvbHZlcigpXSxcclxuXHRcdFx0fSksXHJcblx0XHRcdFZ1ZUkxOG5QbHVnaW4oe1xyXG5cdFx0XHRcdGluY2x1ZGU6IFtwYXRoLnJlc29sdmUoX19kaXJuYW1lLCAnLi9zcmMvbG9jYWxlcy8qLnlhbWwnKV0sXHJcblx0XHRcdH0pLFxyXG5cdFx0XSxcclxuXHRcdHNlcnZlcjoge1xyXG5cdFx0XHRwb3J0OiAzMDAwLFxyXG5cdFx0XHRwcm94eToge1xyXG5cdFx0XHRcdC8vIFx1NTMzOVx1OTE0RFx1NjI0MFx1NjcwOVVSTFx1OERFRlx1NUY4NFx1NEUwRFx1NTMwNVx1NTQyQiAvY29uc29sZS9cdTc2ODRcdThERUZcdTVGODRcdTc2ODRcdThCRjdcdTZDNDJcdUZGMENcdTRFRTNcdTc0MDZcdTUyMzBcdTY3MERcdTUyQTFcdTdBRUZcclxuXHRcdFx0XHQnXig/IV4vY29uc29sZS8pLionOiBlbnYuVklURV9TRVJWRVJfVVJMLFxyXG5cdFx0XHR9XHJcblx0XHR9LFxyXG5cdFx0cmVzb2x2ZToge1xyXG5cdFx0XHRhbGlhczoge1xyXG5cdFx0XHRcdCdAJzogZmlsZVVSTFRvUGF0aChuZXcgVVJMKCcuL3NyYycsIGltcG9ydC5tZXRhLnVybCkpLFxyXG5cdFx0XHR9LFxyXG5cdFx0fSxcclxuXHRcdGJ1aWxkOiB7XHJcblx0XHRcdG91dERpcjogZmlsZVVSTFRvUGF0aChcclxuXHRcdFx0XHRuZXcgVVJMKCcuLi9zZXJ2ZXIvc3JjL21haW4vcmVzb3VyY2VzL2NvbnNvbGUnLCBpbXBvcnQubWV0YS51cmwpXHJcblx0XHRcdCksXHJcblx0XHRcdGVtcHR5T3V0RGlyOiB0cnVlLFxyXG5cdFx0XHRjaHVua1NpemVXYXJuaW5nTGltaXQ6IDIwNDgsXHJcblx0XHR9LFxyXG5cdH0pO1xyXG59O1xyXG4iXSwKICAibWFwcGluZ3MiOiAiO0FBQXFVLFNBQVMsY0FBYyxlQUFlO0FBQzNXLE9BQU8sVUFBVTtBQUNqQixPQUFPLFNBQVM7QUFDaEIsT0FBTyxZQUFZO0FBQ25CLE9BQU8sbUJBQW1CO0FBQzFCLFNBQVMsZUFBZSxXQUFXO0FBQ25DLE9BQU8saUJBQWlCO0FBQ3hCLE9BQU8sa0JBQWtCO0FBQ3pCLE9BQU8sZ0JBQWdCO0FBQ3ZCLE9BQU8sZ0JBQWdCO0FBQ3ZCLE9BQU8sV0FBVztBQUNsQixTQUFTLDJCQUEyQjtBQVhwQyxJQUFNLG1DQUFtQztBQUFvSyxJQUFNLDJDQUEyQztBQWE5UCxJQUFPLHNCQUFRLENBQUMsRUFBRSxLQUFLLE1BQXdCO0FBQzlDLFFBQU0sTUFBTSxRQUFRLE1BQU0sUUFBUSxJQUFJLEdBQUcsRUFBRTtBQUUzQyxTQUFPLGFBQWE7QUFBQSxJQUNuQixNQUFNLElBQUk7QUFBQSxJQUNWLFNBQVM7QUFBQSxNQUNSLElBQUk7QUFBQSxNQUNKLE9BQU87QUFBQSxNQUNQLGFBQWE7QUFBQSxNQUNiLFlBQVk7QUFBQSxNQUNaLE1BQU07QUFBQSxNQUNOLFdBQVc7QUFBQSxRQUNWLEtBQUs7QUFBQSxRQUNMLFVBQVU7QUFBQSxVQUNULFNBQVM7QUFBQSxRQUNWO0FBQUEsUUFDQSxTQUFTLENBQUMsT0FBTyxjQUFjLGNBQWM7QUFBQSxRQUM3QyxXQUFXLENBQUMsb0JBQW9CLENBQUM7QUFBQSxNQUNsQyxDQUFDO0FBQUEsTUFDRCxXQUFXO0FBQUEsUUFDVixXQUFXLENBQUMsb0JBQW9CLENBQUM7QUFBQSxNQUNsQyxDQUFDO0FBQUEsTUFDRCxjQUFjO0FBQUEsUUFDYixTQUFTLENBQUMsS0FBSyxRQUFRLGtDQUFXLHNCQUFzQixDQUFDO0FBQUEsTUFDMUQsQ0FBQztBQUFBLElBQ0Y7QUFBQSxJQUNBLFFBQVE7QUFBQSxNQUNQLE1BQU07QUFBQSxNQUNOLE9BQU87QUFBQTtBQUFBLFFBRU4scUJBQXFCLElBQUk7QUFBQSxNQUMxQjtBQUFBLElBQ0Q7QUFBQSxJQUNBLFNBQVM7QUFBQSxNQUNSLE9BQU87QUFBQSxRQUNOLEtBQUssY0FBYyxJQUFJLElBQUksU0FBUyx3Q0FBZSxDQUFDO0FBQUEsTUFDckQ7QUFBQSxJQUNEO0FBQUEsSUFDQSxPQUFPO0FBQUEsTUFDTixRQUFRO0FBQUEsUUFDUCxJQUFJLElBQUksd0NBQXdDLHdDQUFlO0FBQUEsTUFDaEU7QUFBQSxNQUNBLGFBQWE7QUFBQSxNQUNiLHVCQUF1QjtBQUFBLElBQ3hCO0FBQUEsRUFDRCxDQUFDO0FBQ0Y7IiwKICAibmFtZXMiOiBbXQp9Cg==
