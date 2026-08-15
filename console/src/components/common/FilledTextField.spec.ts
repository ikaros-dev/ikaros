import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import FilledTextField from './FilledTextField.vue';

describe('浮动标签输入框', () => {
	it('渲染标签并透传输入框属性', () => {
		const wrapper = mount(FilledTextField, {
			props: {
				id: 'password',
				label: '密码',
				modelValue: '',
				type: 'password',
				required: true,
			},
			attrs: {
				autocomplete: 'current-password',
			},
		});
		const input = wrapper.get('input');

		expect(wrapper.get('label').attributes('for')).toBe('password');
		expect(input.attributes()).toMatchObject({
			id: 'password',
			type: 'password',
			required: '',
			autocomplete: 'current-password',
		});
	});

	it('通过 v-model 更新输入内容', async () => {
		const wrapper = mount(FilledTextField, {
			props: {
				id: 'username',
				label: '用户名',
				modelValue: '',
			},
		});

		await wrapper.get('input').setValue('admin');

		expect(wrapper.emitted('update:modelValue')).toEqual([['admin']]);
	});

	it('对外提供输入框聚焦方法', () => {
		const wrapper = mount(FilledTextField, {
			props: {
				id: 'username',
				label: '用户名',
				modelValue: '',
			},
			attachTo: document.body,
		});
		const input = wrapper.get('input').element as HTMLInputElement;
		const focusSpy = vi.spyOn(input, 'focus');

		wrapper.vm.focus();

		expect(focusSpy).toHaveBeenCalledOnce();
		wrapper.unmount();
	});
});
