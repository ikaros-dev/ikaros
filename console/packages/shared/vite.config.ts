import { fileURLToPath, URL } from 'url';
import { defineConfig } from 'vite';
import Dts from 'vite-plugin-dts';
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig({
	plugins: [
		Dts({
			entryRoot: './src',
			outputDir: './dist',
			insertTypesEntry: true,
		}),
	],
	resolve: {
		alias: {
			'@': fileURLToPath(new URL('./src', import.meta.url)),
		},
	},
	build: {
		outDir: path.resolve(__dirname, 'dist'),
		lib: {
			entry: path.resolve(__dirname, 'src/index.ts'),
			name: 'IkarosShared',
			formats: ['es', 'iife'],
			fileName: (format) => `ikaros-shared.${format}.js`,
		},
		rollupOptions: {
			external: ['vue', 'vue-router'],
			output: {
				globals: {
					vue: 'Vue',
					'vue-router': 'VueRouter',
				},
				exports: 'named',
				generatedCode: 'es5',
			},
		},
		sourcemap: true,
	},
});
