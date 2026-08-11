import { shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => ({
	t: vi.fn((key: string) => {
		const messages: Record<string, string> = {
			'view.exception.exception.title': '应用异常',
			'view.exception.exception.back': '返回',
			'view.exception.exception.dashboard': '仪表盘',
			'view.exception.notfound.message': '404 没有找到',
		};
		return messages[key] || key;
	}),
	back: vi.fn(),
	push: vi.fn(),
}));

vi.mock('@/locales', () => ({ i18n: { global: { t: mocks.t } } }));
vi.mock('vue-router', () => ({
	useRouter: () => ({ back: mocks.back, push: mocks.push }),
}));

import Exception from './Exception.vue';
import NotFound from './NotFound.vue';

describe('异常页面', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('404 页面使用已存在的国际化文案', () => {
		const wrapper = shallowMount(NotFound);

		expect(wrapper.findComponent(Exception).props('message')).toBe('404 没有找到');
		expect(mocks.t).toHaveBeenCalledWith('view.exception.notfound.message');
	});

	it('通用异常页不再显示错误的国际化键', () => {
		const wrapper = shallowMount(Exception, {
			props: { code: 404 },
			global: {
				stubs: { ElButton: { template: '<button><slot /></button>' } },
			},
		});

		expect(wrapper.text()).toContain('Title: 应用异常');
		expect(wrapper.text()).toContain('返回');
		expect(wrapper.text()).toContain('仪表盘');
		expect(mocks.t.mock.calls.flat().join(' ')).not.toContain('views.exception');
	});
});
