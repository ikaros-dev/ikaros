/** @type {import('tailwindcss').Config} */
const FormKitVariants = require('@formkit/themes/tailwindcss');
export default {
	content: [
		'./src/**/*.{html,js,ts,vue}',
		'./node_modules/@formkit/themes/dist/tailwindcss/genesis/index.cjs',
	],
	theme: {
		extend: {},
	},
	plugins: [FormKitVariants],
};
