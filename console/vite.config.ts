import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import VueJsx from '@vitejs/plugin-vue-jsx';
import { fileURLToPath, URL } from 'url';
import Compression from 'vite-plugin-compression2';
import eslintPlugin from 'vite-plugin-eslint';
// ElementPlus
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
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
