import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const { info, push, preview, confirm, rescan } = vi.hoisted(() => ({
	info: vi.fn(),
	push: vi.fn(),
	preview: vi.fn(),
	confirm: vi.fn(),
	rescan: vi.fn(),
}));
const ButtonStub = { template: '<button><slot /></button>' };
const CardStub = { template: '<div><slot /></div>' };
const PassthroughStub = { template: '<div><slot /></div>' };
const ResultStub = {
	props: ['title'],
	template: '<div>{{ title }}<slot name="extra" /></div>',
};

vi.mock('@/utils/api-client', () => ({
	apiClient: {
		actuator: { info },
		binding: {
			previewLocalDirectoryBinding: preview,
			confirmLocalDirectoryBinding: confirm,
			rescanLocalDirectoryBinding: rescan,
		},
	},
}));
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }));
vi.mock('vue-i18n', () => ({ useI18n: () => ({ t: (key: string) => key }) }));

import Dashboard from './Dashboard.vue';

const validInfo = (overrides: Record<string, unknown> = {}) => ({
	attachment: { file: 4, folder: 2 },
	subject: { total: 3, video: 1, anime: 1, real: 1, music: 2, comic: 3 },
	subjectCollection: { total: 5, doing: 2 },
	...overrides,
});

const mountDashboard = () =>
	mount(Dashboard, {
		global: {
			stubs: {
				ElIcon: PassthroughStub,
				ElCard: CardStub,
				ElCol: PassthroughStub,
				ElRow: PassthroughStub,
				ElButton: ButtonStub,
				ElResult: ResultStub,
			},
		},
	});

describe('Dashboard', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		info.mockResolvedValue({ data: validInfo() });
	});

	it('保留收藏和在看统计，并按类型计算内容统计', async () => {
		const wrapper = mountDashboard();
		await flushPromises();
		expect(wrapper.text()).toContain('module.dashboard.label.collection');
		expect(wrapper.text()).toContain('module.dashboard.label.doing');
		expect(wrapper.text()).toContain('3');
		expect(wrapper.text()).toContain('module.dashboard.label.video');
		const cards = wrapper.findAll('.dashboard-card');
		await cards[0].trigger('click');
		await cards[1].trigger('click');
		await cards[2].trigger('click');
		await cards[3].trigger('click');
		await cards[4].trigger('click');
		expect(push).toHaveBeenNthCalledWith(1, '/sources');
		expect(push).toHaveBeenNthCalledWith(2, '/sources');
		expect(push).toHaveBeenNthCalledWith(3, '/videos');
		expect(push).toHaveBeenNthCalledWith(4, '/music');
		expect(push).toHaveBeenNthCalledWith(5, '/images');
	});

	it('每张统计卡片使用统一居中内容容器', async () => {
		const wrapper = mountDashboard();
		await flushPromises();

		expect(wrapper.findAll('.dashboard-card-layout')).toHaveLength(7);
		expect(wrapper.findAll('.dashboard-card-content')).toHaveLength(7);
	});

	it('全空时提供添加文件源入口', async () => {
		info.mockResolvedValue({
			data: validInfo({
				attachment: { file: 0, folder: 0 },
				subject: { total: 0, video: 0, anime: 0, real: 0, music: 0, comic: 0 },
			}),
		});
		const wrapper = mountDashboard();
		await flushPromises();
		await wrapper.get('button').trigger('click');
		expect(push).toHaveBeenCalledWith('/sources');
	});

	it('不显示数值为零的统计卡片', async () => {
		info.mockResolvedValue({
			data: validInfo({
				attachment: { file: 0, folder: 2 },
				subject: { total: 1, video: 1, anime: 0, real: 0, music: 0, comic: 0 },
				subjectCollection: { total: 0, doing: 0 },
			}),
		});
		const wrapper = mountDashboard();
		await flushPromises();
		expect(wrapper.text()).toContain('module.dashboard.label.folder');
		expect(wrapper.text()).toContain('module.dashboard.label.video');
		expect(wrapper.text()).not.toContain('module.dashboard.label.file');
		expect(wrapper.text()).not.toContain('module.dashboard.label.music');
		expect(wrapper.text()).not.toContain('module.dashboard.label.collection');
		expect(wrapper.findAll('.dashboard-card')).toHaveLength(2);
	});

	it('有文件无内容时不显示无效导入引导', async () => {
		info.mockResolvedValue({
			data: validInfo({
				subject: { total: 0, video: 0, anime: 0, real: 0, music: 0, comic: 0 },
			}),
		});
		const wrapper = mountDashboard();
		await flushPromises();
		expect(wrapper.text()).not.toContain('module.dashboard.guide.import.title');
		expect(wrapper.findAll('button')).toHaveLength(0);
		expect(push).not.toHaveBeenCalled();
	});

	it('接口失败后显示错误并支持重试', async () => {
		info
			.mockRejectedValueOnce(new Error('failed'))
			.mockResolvedValueOnce({ data: validInfo() });
		const wrapper = mountDashboard();
		await flushPromises();
		expect(wrapper.text()).toContain('module.dashboard.error');
		await wrapper.get('button').trigger('click');
		await flushPromises();
		expect(info).toHaveBeenCalledTimes(2);
		expect(wrapper.text()).toContain('module.dashboard.label.collection');
	});

	it('字段缺失时显示错误，不静默显示零', async () => {
		info.mockResolvedValue({
			data: { attachment: { file: 0 }, subject: { total: 0 } },
		});
		const wrapper = mountDashboard();
		await flushPromises();
		expect(wrapper.text()).toContain('module.dashboard.error');
	});

	it('任何状态都不调用扫描 API', async () => {
		const wrapper = mountDashboard();
		await flushPromises();
		expect(preview).not.toHaveBeenCalled();
		expect(confirm).not.toHaveBeenCalled();
		expect(rescan).not.toHaveBeenCalled();
		wrapper.unmount();
		expect(preview).not.toHaveBeenCalled();
	});
});
