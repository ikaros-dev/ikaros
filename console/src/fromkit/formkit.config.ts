import { zh } from '@formkit/i18n';
import { DefaultConfigOptions } from '@formkit/vue';
import { generateClasses } from '@formkit/themes';
import { genesisIcons } from '@formkit/icons';
// @ts-ignore
import genesis from '@formkit/themes/tailwindcss/genesis';

import '@formkit/themes/genesis';

const config: DefaultConfigOptions = {
	locales: { zh },
	locale: 'zh',
	icons: {
		...genesisIcons,
	},
	config: {
		classes: generateClasses(genesis),
	},
};

export default config;
