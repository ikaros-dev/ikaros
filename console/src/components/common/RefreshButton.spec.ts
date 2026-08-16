import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import RefreshButton from './RefreshButton.vue';

describe('RefreshButton', () => {
	it('统一使用红色浅色刷新样式并透传按钮属性', () => {
		const wrapper = mount(RefreshButton, {
			props: { loading: true },
			slots: { default: '刷新目录' },
		});
		const button = wrapper.getComponent({ name: 'ElButton' });

		expect(button.props('type')).toBe('danger');
		expect(button.props('plain')).toBe(true);
		expect(button.props('loading')).toBe(true);
		expect(wrapper.text()).toContain('刷新目录');
	});
});
