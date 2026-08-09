import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vitest/config';

export default defineConfig({
	plugins: [vue()],
	resolve: {
		alias: {
			'@': fileURLToPath(new URL('./src', import.meta.url)),
		},
	},
	test: {
		clearMocks: true,
		environment: 'happy-dom',
		include: ['src/**/*.spec.ts', 'packages/*/src/**/*.spec.ts'],
		restoreMocks: true,
		unstubGlobals: true,
		coverage: {
			provider: 'v8',
			reporter: ['text', 'html', 'lcov'],
			include: [
				'packages/api-client/src/common.ts',
				'src/modules/content/subject/ImageSequenceReader.vue',
				'src/router/guards/auth-check.ts',
				'src/stores/{layout,plugin,subject,user}.ts',
				'src/utils/{date,file,string-util}.ts',
			],
			thresholds: {
				branches: 80,
				functions: 90,
				lines: 90,
				statements: 90,
			},
		},
	},
});
