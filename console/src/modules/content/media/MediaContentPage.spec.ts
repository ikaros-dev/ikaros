import { flushPromises, shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => ({ get: vi.fn(), scan: vi.fn() }));

vi.mock('@/utils/api-client', () => ({ apiClient: { get: mocks.get } }));
vi.mock('@/utils/string-util', () => ({
	base64Encode: (value: string) => `encoded:${value}`,
}));
vi.mock('../subject/SubjectDetails.vue', () => ({
	default: { name: 'SubjectDetails' },
}));
vi.mock('../music/MusicAlbumDetail.vue', () => ({
	default: { name: 'MusicAlbumDetail' },
}));

import MediaContentPage from './MediaContentPage.vue';
import VideoContent from './VideoContent.vue';
import MusicContent from './MusicContent.vue';
import ImageContent from './ImageContent.vue';
import mediaModule from './module';

const mountPage = (props: Record<string, unknown> = {}) =>
	shallowMount(MediaContentPage, {
		props: {
			kind: 'video',
			title: '视频',
			types: ['VIDEO', 'ANIME', 'REAL'],
			scanMode: 'EPISODE',
			detailRoute: '/videos/details',
			...props,
		},
		global: {
			mocks: { $t: (key: string) => key },
			stubs: {
				ElRow: { template: '<div><slot /></div>' },
				ElCol: { template: '<div><slot /></div>' },
				ContentBrowser: {
					name: 'ContentBrowser',
					props: ['total', 'error'],
					template:
						'<div><slot name="actions"/><slot name="empty-actions"/><slot/></div>',
				},
				LocalDirectoryBindingDialog: {
					name: 'LocalDirectoryBindingDialog',
					props: ['visible', 'mode', 'directoryId'],
					setup(_props, { expose }) {
						expose({ scan: mocks.scan });
						return {};
					},
					template: '<div class="binding-dialog" />',
				},
				ScanDirectorySelectDialog: {
					name: 'ScanDirectorySelectDialog',
					template: '<div class="directory-dialog" />',
				},
				FileSourceManagerDialog: {
					name: 'FileSourceManagerDialog',
					template: '<div />',
				},
				SubjectCardLink: {
					name: 'SubjectCardLink',
					props: ['id', 'to'],
					template: '<div class="subject-card" :data-to="to" />',
				},
			},
		},
	});

const stateOf = (wrapper: ReturnType<typeof mountPage>) =>
	(wrapper.vm.$ as unknown as { setupState: Record<string, any> }).setupState;

describe('共享媒体列表页面', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		mocks.get.mockResolvedValue({
			data: {
				page: 1,
				size: 12,
				total: 1,
				items: [{ id: 'subject-1', name: 'Video', name_cn: '视频' }],
			},
		});
	});

	it.each([
		{ types: ['VIDEO', 'ANIME', 'REAL'], scanMode: 'EPISODE' },
		{ types: ['MUSIC'], scanMode: 'AUDIO' },
		{ types: ['COMIC'], scanMode: 'IMAGE' },
	])('按内容类型查询并固定扫描模式 %o', async ({ types, scanMode }) => {
		const wrapper = mountPage({ types, scanMode });
		await flushPromises();
		expect(mocks.get).toHaveBeenCalledWith('/api/v1/subjects/condition', {
			params: {
				page: 1,
				size: 12,
				keyword: 'encoded:',
				types: types.join(','),
			},
		});
		expect(
			wrapper
				.findComponent({ name: 'LocalDirectoryBindingDialog' })
				.props('mode')
		).toBe(scanMode);
	});

	it('搜索和清空都重置到第一页并编码 keyword', async () => {
		const wrapper = mountPage();
		const state = stateOf(wrapper);
		await flushPromises();
		state.searchInput = '  关键词  ';
		await state.search();
		expect(mocks.get).toHaveBeenLastCalledWith('/api/v1/subjects/condition', {
			params: {
				page: 1,
				size: 12,
				keyword: 'encoded:关键词',
				types: 'VIDEO,ANIME,REAL',
			},
		});
	});

	it('分页和每页数量变化会重新查询', async () => {
		const wrapper = mountPage();
		const state = stateOf(wrapper);
		await flushPromises();
		await state.changePage(3);
		expect(mocks.get).toHaveBeenLastCalledWith('/api/v1/subjects/condition', {
			params: {
				page: 3,
				size: 12,
				keyword: 'encoded:',
				types: 'VIDEO,ANIME,REAL',
			},
		});
		await state.changeSize(24);
		expect(mocks.get).toHaveBeenLastCalledWith('/api/v1/subjects/condition', {
			params: {
				page: 1,
				size: 24,
				keyword: 'encoded:',
				types: 'VIDEO,ANIME,REAL',
			},
		});
	});

	it('请求失败时进入错误状态而不是空数据', async () => {
		mocks.get.mockRejectedValueOnce(new Error('网络失败'));
		const wrapper = mountPage();
		await flushPromises();
		expect(stateOf(wrapper).error).toBe('网络失败');
		expect(stateOf(wrapper).total).toBe(0);
	});

	it('选择目录不扫描，点击开始扫描后才发起扫描', async () => {
		const wrapper = mountPage();
		await flushPromises();
		const buttons = wrapper.findAllComponents({ name: 'ElButton' });
		await buttons[0].trigger('click');
		expect(stateOf(wrapper).directorySelectVisible).toBe(true);
		expect(mocks.scan).not.toHaveBeenCalled();
		wrapper
			.findComponent({ name: 'ScanDirectorySelectDialog' })
			.vm.$emit('selected', 'directory-id', '019cc123 根目录 / 动画');
		await flushPromises();
		expect(mocks.scan).not.toHaveBeenCalled();
		await buttons[1].trigger('click');
		await flushPromises();
		expect(stateOf(wrapper).importVisible).toBe(true);
		expect(mocks.scan).toHaveBeenCalledTimes(1);
		expect(
			wrapper.findComponent({ name: 'LocalDirectoryBindingDialog' }).props('directoryId')
		).toBe('directory-id');
	});

	it('将卡片链接到传入的详情路由', async () => {
		const wrapper = mountPage({ detailRoute: '/music/details' });
		await flushPromises();
		expect(wrapper.find('.subject-card').attributes('data-to')).toBe(
			'/music/details/subject-1'
		);
	});

	it.each([
		[VideoContent, ['VIDEO', 'ANIME', 'REAL'], 'EPISODE', '/videos/details'],
		[MusicContent, ['MUSIC'], 'AUDIO', '/music/details'],
		[ImageContent, ['COMIC'], 'IMAGE', '/images/details'],
	])(
		'薄页面只装配类型、扫描模式和详情路由',
		(component, types, scanMode, detailRoute) => {
			const wrapper = shallowMount(component, {
				global: {
					mocks: { $t: (key: string) => key },
					stubs: {
						MediaContentPage: {
							name: 'MediaContentPage',
							props: ['types', 'scanMode', 'detailRoute'],
							template: '<div />',
						},
					},
				},
			});
			const page = wrapper.findComponent({ name: 'MediaContentPage' });
			expect(page.props()).toMatchObject({ types, scanMode, detailRoute });
		}
	);

	it('注册三类内容页及对应隐藏详情路由', () => {
		const routes = (mediaModule.routes ?? []).map((configuredRoute) => {
			const route =
				'route' in configuredRoute ? configuredRoute.route : configuredRoute;
			return { path: route.path, hidden: route.meta?.hidden };
		});
		expect(routes).toEqual([
			{ path: '/videos', hidden: undefined },
			{ path: '/music', hidden: undefined },
			{ path: '/images', hidden: undefined },
			{ path: '/videos/details/:id', hidden: true },
			{ path: '/music/details/:id', hidden: true },
			{ path: '/images/details/:id', hidden: true },
		]);
	});
});
