module.exports = {
	env: {
		browser: true,
		es2021: true,
		node: true,
	},
	extends: [
		'eslint:recommended',
		'plugin:vue/vue3-recommended',
		'prettier',
		'./.eslintrc-auto-import.json'
	],
	parser: 'vue-eslint-parser',
	parserOptions: {
		ecmaVersion: 'latest',
		parser: '@typescript-eslint/parser',
		sourceType: 'module',
		ecmaFeatures: {
			tsx: true,
			jsx: true,
		},
	},
	globals: {
		defineProps: 'readonly',
		defineEmits: 'readonly',
		defineExpose: 'readonly',
		withDefault: 'readonly',
	},
	plugins: [
		'vue',
		'@typescript-eslint'
	],
	settings: {
		'import/reslover': {
			alias: {
				map: [
					['@', './src']
				],
			},
		},
		'import/extensions': ['.js', '.jsx', '.ts', '.tsx', '.mjs'],
	},
	rules: {
		'vue/multi-word-component-names': 0,
		'import/no-extraneous-dependencies': 0,
		'no-param-reassing': 0,
		'vue/attribute-hyphenation': 0,
		'vue/v-on-event-hyphenation': 0
	},
};