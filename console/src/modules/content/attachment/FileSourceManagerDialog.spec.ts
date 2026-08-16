import { flushPromises, shallowMount } from '@vue/test-utils';
import { defineComponent, h, nextTick } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => ({
	list: vi.fn(),
	listFetchers: vi.fn(),
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
			listDriversFetchers: mocks.listFetchers,
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
const customDriver = {
	id: 'source-2',
	type: 'CUSTOM' as const,
	name: 'ALIYUN',
	mount_name: '云盘',
	remote_path: 'root',
	access_token: 'access',
	refresh_token: 'refresh',
	comment: '远端媒体',
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

const TableColumnStub = defineComponent({
	setup(_, { slots }) {
		return () => h('div', slots.default?.({ row: localDriver }));
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
				ElDialog: {
					props: ['width'],
					template: '<div :data-width="width"><slot/></div>',
				},
				ElAlert: { template: '<div><slot/></div>' },
				ElButton: {
					template: '<button @click="$emit(\'click\')"><slot/></button>',
				},
				ElForm: FormStub,
				ElFormItem: { template: '<label><slot/></label>' },
				ElInput: true,
				ElOption: true,
				ElRadioButton: { template: '<span><slot/></span>' },
				ElRadioGroup: { template: '<div><slot/></div>' },
				ElSelect: { template: '<div><slot/></div>' },
				ElTable: { template: '<div><slot/></div>' },
				ElTableColumn: TableColumnStub,
				ElSwitch: true,
			},
		},
	});

const stateOf = (wrapper: ReturnType<typeof mountDialog>) =>
	(wrapper.vm.$ as unknown as { setupState: Record<string, any> }).setupState;

describe('文件源管理对话框', () => {
	beforeEach(() => {
		mocks.list.mockResolvedValue({
			data: { items: [localDriver, customDriver] },
		});
		mocks.listFetchers.mockResolvedValue({
			data: [{ type: 'LOCAL', name: 'DISK' }],
		});
		mocks.save.mockResolvedValue({ data: localDriver });
		mocks.enable.mockResolvedValue({});
		mocks.disable.mockResolvedValue({});
		mocks.remove.mockResolvedValue({});
		mocks.confirm.mockResolvedValue('confirm');
	});

	it('使用视口内自适应宽度和双栏内容容器', () => {
		const wrapper = mountDialog();

		expect(wrapper.attributes('data-width')).toBe(
			'min(1100px, calc(100vw - 32px))'
		);
		expect(wrapper.find('.manager-layout').exists()).toBe(true);
		expect(wrapper.find('.source-form-panel').exists()).toBe(true);
		expect(wrapper.find('.source-list-panel').exists()).toBe(true);
	});

	it('文件源名称和服务器路径使用软换行单元格', () => {
		const wrapper = mountDialog();
		const cells = wrapper.findAll('.source-cell-text');

		expect(cells).toHaveLength(2);
		expect(cells[0].text()).toBe(localDriver.mount_name);
		expect(cells[1].text()).toBe(localDriver.remote_path);
	});

	it('打开时加载全部文件源和可用驱动实现', async () => {
		const wrapper = mountDialog();
		await wrapper.setProps({ visible: true });
		await flushPromises();

		expect(mocks.list).toHaveBeenCalledWith({ page: 1, size: 1000 });
		expect(mocks.listFetchers).toHaveBeenCalledOnce();
		expect(stateOf(wrapper).drivers).toEqual([localDriver, customDriver]);
	});

	it('没有自定义驱动插件时隐藏类型并固定为本地驱动', async () => {
		const wrapper = mountDialog(true);
		await flushPromises();
		const state = stateOf(wrapper);

		expect(state.customAvailable).toBe(false);
		expect(state.selectedType).toBe('LOCAL');
		expect(state.form.name).toBe('DISK');
		expect(wrapper.findComponent({ name: 'ElRadioGroup' }).exists()).toBe(
			false
		);
	});

	it('插件提供自定义驱动时显示类型并保存插件字段', async () => {
		mocks.listFetchers.mockResolvedValue({
			data: [
				{ type: 'LOCAL', name: 'DISK' },
				{ type: 'CUSTOM', name: 'ALIYUN' },
			],
		});
		mocks.save.mockResolvedValue({ data: customDriver });
		const wrapper = mountDialog(true);
		await flushPromises();
		const state = stateOf(wrapper);

		expect(state.customAvailable).toBe(true);
		state.selectedType = 'CUSTOM';
		state.changeType();
		state.form.mount_name = '云盘';
		state.form.remote_path = 'root';
		state.form.access_token = 'access';
		state.form.refresh_token = 'refresh';
		state.form.comment = '远端媒体';
		await nextTick();
		await state.saveForm();

		expect(mocks.save).toHaveBeenCalledWith({
			attachmentDriver: {
				type: 'CUSTOM',
				name: 'ALIYUN',
				mount_name: '云盘',
				remote_path: 'root',
				access_token: 'access',
				refresh_token: 'refresh',
				comment: '远端媒体',
			},
		});
	});

	it('切换类型时分别保留本地和自定义表单数据', async () => {
		mocks.listFetchers.mockResolvedValue({
			data: [
				{ type: 'LOCAL', name: 'DISK' },
				{ type: 'CUSTOM', name: 'ALIYUN' },
			],
		});
		const wrapper = mountDialog(true);
		await flushPromises();
		const state = stateOf(wrapper);

		state.form.mount_name = '本地目录';
		state.form.remote_path = '/media/local';
		state.form.comment = '本地备注';
		state.selectedType = 'CUSTOM';
		state.changeType();
		await nextTick();

		expect(state.form.mount_name).toBe('');
		expect(state.form.name).toBe('ALIYUN');
		state.form.mount_name = '自定义目录';
		state.form.remote_path = 'custom-root';
		state.form.access_token = 'custom-access';
		state.form.refresh_token = 'custom-refresh';
		state.form.comment = '自定义备注';

		state.selectedType = 'LOCAL';
		state.changeType();
		await nextTick();
		expect(state.form).toMatchObject({
			name: 'DISK',
			mount_name: '本地目录',
			remote_path: '/media/local',
			comment: '本地备注',
		});

		state.selectedType = 'CUSTOM';
		state.changeType();
		await nextTick();
		expect(state.form).toMatchObject({
			name: 'ALIYUN',
			mount_name: '自定义目录',
			remote_path: 'custom-root',
			access_token: 'custom-access',
			refresh_token: 'custom-refresh',
			comment: '自定义备注',
		});
	});

	it('类型选择项使用独立的居中样式类', async () => {
		mocks.listFetchers.mockResolvedValue({
			data: [{ type: 'CUSTOM', name: 'ALIYUN' }],
		});
		const wrapper = mountDialog(true);
		await flushPromises();

		expect(wrapper.find('.type-form-item').exists()).toBe(true);
	});

	it('本地驱动保存时不提交自定义驱动字段', async () => {
		const wrapper = mountDialog();
		const state = stateOf(wrapper);
		state.form.mount_name = '新文件源';
		state.form.remote_path = '/srv/media';
		state.form.access_token = 'ignored-access';
		state.form.refresh_token = 'ignored-refresh';
		await nextTick();

		await state.saveForm();

		expect(mocks.save).toHaveBeenCalledWith({
			attachmentDriver: {
				type: 'LOCAL',
				name: 'DISK',
				mount_name: '新文件源',
				remote_path: '/srv/media',
				access_token: undefined,
				refresh_token: undefined,
				comment: '',
			},
		});
		expect(mocks.enable).toHaveBeenCalledWith({ id: 'source-1' });
	});

	it('编辑时保留内部字段并复用左侧表单', async () => {
		const wrapper = mountDialog();
		const state = stateOf(wrapper);
		state.openEdit(localDriver);
		state.form.remote_path = '/new-path';
		await nextTick();

		await state.saveForm();

		expect(mocks.save).toHaveBeenCalledWith({
			attachmentDriver: {
				...localDriver,
				remote_path: '/new-path',
				access_token: undefined,
				refresh_token: undefined,
			},
		});
		expect(mocks.enable).not.toHaveBeenCalled();
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
});
