module.exports = {
	extends: [
        "stylelint-config-standard",
        "stylelint-config-recommended-less",
        "stylelint-config-recommended-scss",
        "stylelint-config-recommended-vue",
        "stylelint-config-standard-scss",
	    "stylelint-config-recommended-vue/scss"
    ],
    overrides: [
		{
			files: ['*.scss', '**/*.scss'],
			customSyntax: 'postcss-scss',
			extends: ['stylelint-config-recommended-scss'],
		},
		{
			files: ['*.less', '**/*.less'],
			customSyntax: 'postcss-less',
			extends: ['stylelint-config-recommended-less'],
		},
		{
			files: ['*.vue', '**/*.vue'],
			customSyntax: 'postcss-html',
			extends: ['stylelint-config-recommended-vue'],
		},
	],
};