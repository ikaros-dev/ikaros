import { flushPromises, shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const { listDrivers, listAttachments, refreshAttachments } = vi.hoisted(() => ({
	listDrivers: vi.fn(),
	listAttachments: vi.fn(),
	refreshAttachments: vi.fn(),
}));

vi.mock('@/utils/api-client', () => ({
	apiClient: {
		attachmentDriver: {
			listDriversByCondition: listDrivers,
			listAttachmentsByCondition: refreshAttachments,
		},
		attachment: { listAttachmentsByCondition1: listAttachments },
	},
}));
vi.mock('vue-i18n', () => ({
	useI18n: () => ({
		t: (key: string, params?: { id?: string }) =>
			key.endsWith('source-root') ? `${params?.id} 根目录` : key,
	}),
}));

import ScanDirectorySelectDialog from './ScanDirectorySelectDialog.vue';

const source = {
	id: 'source-root-id',
	name: '媒体源',
	type: 'Driver_Directory',
	driverId: '019cc123-abcd-7000-8000-000000000001',
};
const folder = {
	id: 'folder-id',
	name: '动画',
	type: 'Driver_Directory',
	driverId: source.driverId,
};

const mountDialog = () =>
	shallowMount(ScanDirectorySelectDialog, {
		props: { visible: false },
		global: {
			directives: {
				loading: () => undefined,
			},
			stubs: {
				ElButton: { template: '<button><slot /></button>' },
				ElDialog: { template: '<div><slot /><slot name="footer" /></div>' },
				ElScrollbar: { template: '<div><slot /></div>' },
				ElBreadcrumb: { template: '<div><slot /></div>' },
				ElBreadcrumbItem: { template: '<div><slot /></div>' },
				ElIcon: { template: '<i><slot /></i>' },
				ElAlert: true,
				ElEmpty: { template: '<div><slot /></div>' },
			},
		},
	});

const stateOf = (wrapper: ReturnType<typeof mountDialog>) =>
	(wrapper.vm.$ as unknown as { setupState: Record<string, any> }).setupState;

describe('扫描目录选择器', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		listDrivers.mockResolvedValue({
			data: {
				items: [{ id: source.driverId, type: 'LOCAL', enable: true }],
			},
		});
		listAttachments.mockImplementation(({ parentId }) => ({
			data: { items: parentId === source.id ? [folder] : [source] },
		}));
		refreshAttachments.mockResolvedValue({ data: { items: [folder] } });
	});

	it('单文件源直接展示源根子目录且禁止选择源根', async () => {
		const wrapper = mountDialog();
		await wrapper.setProps({ visible: true });
		await flushPromises();
		const state = stateOf(wrapper);

		expect(state.paths).toEqual([
			expect.objectContaining({
				id: source.id,
				name: '019cc123 根目录',
				isSourceRoot: true,
			}),
		]);
		expect(state.showingSystemRoot).toBe(false);
		expect(state.canSelectCurrent).toBe(false);
		expect(state.directories).toEqual([folder]);
	});

	it('目录名称和面包屑允许软换行且刷新按钮使用预设红色系', async () => {
		const wrapper = mountDialog();
		await wrapper.setProps({ visible: true });
		await flushPromises();

		expect(wrapper.get('.directory-name').text()).toBe(folder.name);
		expect(wrapper.find('.breadcrumb-path').exists()).toBe(true);
		const refreshButton = wrapper.get('.refresh-button');
		expect(refreshButton.attributes('type')).toBe('danger');
		expect(refreshButton.attributes()).toHaveProperty('plain');
	});

	it('递归进入子目录后允许选择并仅在点击刷新时扫描当前层', async () => {
		const wrapper = mountDialog();
		await wrapper.setProps({ visible: true });
		await flushPromises();
		const state = stateOf(wrapper);

		await state.enterDirectory(folder);
		expect(state.canSelectCurrent).toBe(true);
		expect(refreshAttachments).not.toHaveBeenCalled();

		await state.refreshCurrentDirectory();
		expect(refreshAttachments).toHaveBeenCalledWith({
			page: 1,
			size: 999999,
			parentId: folder.id,
			refresh: true,
		});

		state.selectCurrentDirectory();
		expect(wrapper.emitted('selected')).toEqual([
			[folder.id, '019cc123 根目录 / 动画'],
		]);
	});

	it('多文件源保留系统总根且不提供刷新', async () => {
		const secondDriverId = '019cc124-abcd-7000-8000-000000000002';
		const secondSource = {
			...source,
			id: 'source-root-2',
			driverId: secondDriverId,
		};
		listDrivers.mockResolvedValue({
			data: {
				items: [
					{ id: source.driverId, type: 'LOCAL', enable: true },
					{ id: secondDriverId, type: 'LOCAL', enable: true },
				],
			},
		});
		listAttachments.mockResolvedValue({
			data: { items: [source, secondSource] },
		});
		const wrapper = mountDialog();
		await wrapper.setProps({ visible: true });
		await flushPromises();
		const state = stateOf(wrapper);

		expect(state.paths).toEqual([]);
		expect(state.showingSystemRoot).toBe(true);
		expect(state.canSelectCurrent).toBe(false);
		expect(refreshAttachments).not.toHaveBeenCalled();
	});
});
