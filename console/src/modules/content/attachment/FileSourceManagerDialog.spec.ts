import { flushPromises, shallowMount } from '@vue/test-utils';
import { defineComponent, h, nextTick } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => ({
	list: vi.fn(),
	save: vi.fn(),
	enable: vi.fn(),
	disable: vi.fn(),
	remove: vi.fn(),
	messageSuccess: vi.fn(),
	messageError: vi.fn(),
	confirm: vi.fn(),
}));

vi.mock('@/utils/api-client', () => ({
	apiClient: {
		attachmentDriver: {
			listDriversByCondition: mocks.list,
			saveAttachmentDriver: mocks.save,
			enableDriver1: mocks.enable,
			enableDriver: mocks.disable,
			deleteAttachmentDriverById: mocks.remove,
		},
	},
}));

vi.mock('vue-i18n', () => ({
	useI18n: () => ({ t: (key: string) => key }),
}));

vi.mock('element-plus', async () => {
	const actual =
		await vi.importActual<typeof import('element-plus')>('element-plus');
	return {
		...actual,
		ElMessage: {
			success: mocks.messageSuccess,
			error: mocks.messageError,
		},
		ElMessageBox: { confirm: mocks.confirm },
	};
});

import FileSourceManagerDialog from './FileSourceManagerDialog.vue';

const localDriver = {
	id: 'source-1',
	type: 'LOCAL' as const,
	name: 'DISK',
	mount_name: '媒体目录',
	remote_path: '/media',
	comment: '本地媒体',
	enable: true,
};

const FormStub = defineComponent({
	setup(_, { expose, slots }) {
		expose({
			validate: () => Promise.resolve(true),
			clearValidate: vi.fn(),
		});
		return () => h('form', slots.default?.());
	},
});

const mountDialog = (visible = false) =>
	shallowMount(FileSourceManagerDialog, {
		props: { visible },
		global: {
			directives: {
				loading: () => undefined,
			},
			stubs: {
				ElDialog: { template: '<div><slot/><slot name="footer"/></div>' },
				ElAlert: { template: '<div><slot/></div>' },
				ElButton: {
					template: '<button @click="$emit(\'click\')"><slot/></button>',
				},
				ElForm: FormStub,
				ElFormItem: { template: '<label><slot/></label>' },
				ElInput: true,
				ElTable: { template: '<div><slot/></div>' },
				ElTableColumn: true,
				ElSwitch: true,
			},
		},
	});

const stateOf = (wrapper: ReturnType<typeof mountDialog>) =>
	(wrapper.vm.$ as unknown as { setupState: Record<string, any> }).setupState;

describe('文件源管理对话框', () => {
	beforeEach(() => {
		mocks.list.mockResolvedValue({
			data: {
				items: [
					localDriver,
					{ id: 'custom-1', type: 'CUSTOM', name: 'PLUGIN' },
				],
			},
		});
		mocks.save.mockResolvedValue({ data: localDriver });
		mocks.enable.mockResolvedValue({});
		mocks.disable.mockResolvedValue({});
		mocks.remove.mockResolvedValue({});
		mocks.confirm.mockResolvedValue('confirm');
	});

	it('打开时加载并仅展示本地文件源', async () => {
		const wrapper = mountDialog();
		await wrapper.setProps({ visible: true });
		await flushPromises();

		expect(mocks.list).toHaveBeenCalledWith({ page: 1, size: 1000 });
		expect(stateOf(wrapper).drivers).toEqual([localDriver]);
	});

	it('新建后使用保存响应中的标识启用并发出变更事件', async () => {
		const created = { ...localDriver, id: 'created-1', enable: false };
		mocks.save.mockResolvedValue({ data: created });
		const wrapper = mountDialog();
		const state = stateOf(wrapper);
		state.openCreate();
		await nextTick();
		state.form.mount_name = '新文件源';
		state.form.remote_path = '/srv/media';
		state.form.comment = '备注';

		await state.saveForm();

		expect(mocks.save).toHaveBeenCalledWith({
			attachmentDriver: {
				type: 'LOCAL',
				name: 'DISK',
				mount_name: '新文件源',
				remote_path: '/srv/media',
				comment: '备注',
			},
		});
		expect(mocks.enable).toHaveBeenCalledWith({ id: 'created-1' });
		expect(wrapper.emitted('changed')).toHaveLength(1);
	});

	it('编辑时保留启用状态和内部字段', async () => {
		const wrapper = mountDialog();
		const state = stateOf(wrapper);
		state.openEdit(localDriver);
		await nextTick();
		state.form.remote_path = '/new-path';

		await state.saveForm();

		expect(mocks.save).toHaveBeenCalledWith({
			attachmentDriver: { ...localDriver, remote_path: '/new-path' },
		});
		expect(mocks.enable).not.toHaveBeenCalled();
		expect(wrapper.emitted('changed')).toHaveLength(1);
	});

	it('分别调用启用和停用接口', async () => {
		const wrapper = mountDialog();
		const state = stateOf(wrapper);

		await state.changeEnabled(localDriver, true);
		await state.changeEnabled(localDriver, false);

		expect(mocks.enable).toHaveBeenCalledWith({ id: 'source-1' });
		expect(mocks.disable).toHaveBeenCalledWith({ id: 'source-1' });
		expect(wrapper.emitted('changed')).toHaveLength(2);
	});

	it('取消删除时不调用删除接口', async () => {
		mocks.confirm.mockRejectedValue('cancel');
		const wrapper = mountDialog();

		await stateOf(wrapper).deleteDriver(localDriver);

		expect(mocks.remove).not.toHaveBeenCalled();
	});

	it('确认删除后调用删除接口并发出变更事件', async () => {
		const wrapper = mountDialog();

		await stateOf(wrapper).deleteDriver(localDriver);

		expect(mocks.remove).toHaveBeenCalledWith({ id: 'source-1' });
		expect(wrapper.emitted('changed')).toHaveLength(1);
	});

	it.each([
		['列表加载', 'list', 'errors.load'],
		['编辑保存', 'save', 'errors.save'],
		['启停', 'enable', 'errors.toggle'],
		['删除', 'remove', 'errors.delete'],
	] as const)('%s失败时显示可见错误', async (_, request, errorKey) => {
		mocks[request].mockRejectedValueOnce(new Error('request failed'));
		const wrapper = mountDialog();
		const state = stateOf(wrapper);

		if (request === 'list') {
			await wrapper.setProps({ visible: true });
			await flushPromises();
			expect(state.errorMessage).toContain(errorKey);
			return;
		}
		if (request === 'save') {
			state.openEdit(localDriver);
			await nextTick();
			await state.saveForm();
		} else if (request === 'enable') {
			await state.changeEnabled(localDriver, true);
		} else {
			await state.deleteDriver(localDriver);
		}

		expect(mocks.messageError).toHaveBeenCalledWith(
			expect.stringContaining(errorKey)
		);
		expect(wrapper.emitted('changed')).toBeUndefined();
	});

	it('保存响应缺少标识时不启用且不报告完整成功', async () => {
		mocks.save.mockResolvedValue({ data: { ...localDriver, id: undefined } });
		const wrapper = mountDialog();
		const state = stateOf(wrapper);
		state.openCreate();
		await nextTick();

		await state.saveForm();

		expect(mocks.enable).not.toHaveBeenCalled();
		expect(mocks.messageError).toHaveBeenCalledWith(
			expect.stringContaining('errors.missing-id')
		);
		expect(wrapper.emitted('changed')).toBeUndefined();
	});

	it('新建保存后启用失败时刷新列表且不报告完整成功', async () => {
		mocks.enable.mockRejectedValueOnce(new Error('enable failed'));
		const wrapper = mountDialog();
		const state = stateOf(wrapper);
		state.openCreate();
		await nextTick();

		await state.saveForm();

		expect(mocks.list).toHaveBeenCalledTimes(1);
		expect(mocks.messageError).toHaveBeenCalledWith(
			expect.stringContaining('errors.enable-after-save')
		);
		expect(wrapper.emitted('changed')).toBeUndefined();
	});
});
