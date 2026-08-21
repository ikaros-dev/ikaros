import pluginVue from 'eslint-plugin-vue';
import {
	vueTsConfigs,
	defineConfigWithVueTs,
} from '@vue/eslint-config-typescript';
import vuePrettierConfig from '@vue/eslint-config-prettier';
import autoImportGlobals from './.eslintrc-auto-import.json' with { type: 'json' };

export default defineConfigWithVueTs(
	{
		name: 'app/files-to-lint',
		files: ['**/*.{ts,mts,tsx,vue}'],
	},
	{
		name: 'app/files-to-ignore',
		ignores: [
			'**/dist/**',
			'**/dist-ssr/**',
			'**/coverage/**',
			'**/*.sh',
			'**/*.md',
			'**/*.woff',
			'**/*.ttf',
			'**/node_modules/**',
			'**/.vscode/**',
			'**/.idea/**',
			'**/public/**',
			'**/docs/**',
			'**/.husky/**',
			'**/bin/**',
			'**/prettier.config.js',
			'**/src/mock/*',
			'**/logs/**',
			'**/*.log',
			'**/cypress/videos/**',
			'**/cypress/screenshots/**',
			'**/components.d.ts',
			'*.js',
		],
	},

	pluginVue.configs['flat/essential'],
	vueTsConfigs.recommended,
	vuePrettierConfig,

	{
		name: 'app/custom-rules',
		languageOptions: {
			globals: {
				...autoImportGlobals.globals,
				defineProps: 'readonly',
				defineEmits: 'readonly',
				defineExpose: 'readonly',
				withDefault: 'readonly',
			},
		},
		rules: {
			'vue/multi-word-component-names': 'off',
			'import/no-extraneous-dependencies': 'off',
			'vue/attribute-hyphenation': 'off',
			'vue/v-on-event-hyphenation': 'off',
			'vue/first-attribute-linebreak': [
				'error',
				{
					singleline: 'ignore',
					multiline: 'ignore',
				},
			],
			'@typescript-eslint/no-explicit-any': 'off',
			'@typescript-eslint/no-unused-vars': 'off',
			'@typescript-eslint/no-empty-object-type': 'off',
			'@typescript-eslint/no-this-alias': 'off',
			'@typescript-eslint/no-unused-expressions': 'off',
			'@typescript-eslint/ban-ts-comment': 'off',
		},
	},
);
