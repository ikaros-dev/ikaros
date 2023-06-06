import { defineConfig, loadEnv } from 'vite';
import path from 'path';
import vue from '@vitejs/plugin-vue';
import VueJsx from '@vitejs/plugin-vue-jsx';
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite';
import { fileURLToPath, URL } from 'url';
import Compression from 'vite-plugin-compression2';
import eslintPlugin from 'vite-plugin-eslint';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import Icons from 'unplugin-icons/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';

export default ({ mode }: { mode: string }) => {
	const env = loadEnv(mode, process.cwd(), '');

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
					enabled: true,
				},
				imports: ['vue', 'vue-router', '@vueuse/core'],
				resolvers: [ElementPlusResolver()],
			}),
			Components({
				resolvers: [ElementPlusResolver()],
			}),
			VueI18nPlugin({
				include: [path.resolve(__dirname, './src/locales/*.yaml')],
			}),
		],
		server: {
			port: 3000,
			proxy: {
				// 匹配所有URL路径不包含 /console/的路径的请求，代理到服务端
				'^(?!^/console/).*': env.VITE_SERVER_URL,
			},
		},
		resolve: {
			alias: {
				'@': fileURLToPath(new URL('./src', import.meta.url)),
			},
		},
		build: {
			outDir: fileURLToPath(
				new URL('../server/src/main/resources/console', import.meta.url)
			),
			emptyOutDir: true,
			chunkSizeWarningLimit: 2048,
		},
	});
};
