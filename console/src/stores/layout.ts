import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useLayoutStore = defineStore('layout', () => {
	const asideIsExtend = ref(true);
	const currentActivePath = ref('/dashboard');
	const i18nCode = ref('zh-CN');

	function switchLayoutAsideExtendState() {
		asideIsExtend.value = !asideIsExtend.value;
	}

	function updatecurrentActivePathByRoutePath(path: string) {
		currentActivePath.value = path;
	}

	function setI18nCode(code: string) {
		i18nCode.value = code;
	}

	return {
		asideIsExtend,
		currentActivePath,
		i18nCode,
		switchLayoutAsideExtendState,
		updatecurrentActivePathByRoutePath,
		setI18nCode,
	};
}, {
	// In order to config pinia-plugin-persist, please see https://github.com/Seb-L/pinia-plugin-persist
	persist: {
		enabled: true,
		strategies: [
			{
				key: 'ikaros-store-layout',
				storage: localStorage,
			},
		],
	},
});
