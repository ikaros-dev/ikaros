import { shallowMount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import ContentBrowser from './ContentBrowser.vue';

const ElInputStub = {
	name: 'ElInput',
	emits: ['clear', 'keyup', 'update:modelValue'],
	template: `
		<div class="el-input-stub">
			<input @keyup="$emit('keyup', $event)" />
			<button class="clear-button" @click="$emit('clear')">清空</button>
			<slot name="append" />
		</div>
	`,
};

const ElEmptyStub = {
	name: 'ElEmpty',
	props: ['description'],
	template: '<div class="el-empty-stub"><slot /></div>',
};

const ElResultStub = {
	name: 'ElResult',
	props: ['subTitle'],
	template: '<div class="el-result-stub"><slot name="extra" /></div>',
};

const mountBrowser = (
	props: Partial<InstanceType<typeof ContentBrowser>['$props']> = {},
	slots: Record<string, string> = {}
) =>
	shallowMount(ContentBrowser, {
		props: {
			title: '视频',
			searchModelValue: '',
			searchPlaceholder: '搜索视频',
			loading: false,
			error: null,
			emptyTitle: '暂无视频',
			emptyDescription: '扫描并导入视频后会显示在这里',
			page: 1,
			size: 12,
			total: 0,
			pageSizes: [12, 24, 48],
			...props,
		},
		slots,
		global: {
			stubs: {
				ElEmpty: ElEmptyStub,
				ElInput: ElInputStub,
				ElResult: ElResultStub,
			},
		},
	});

describe('共享内容浏览外壳', () => {
	it('更新搜索词并通过回车、按钮和清空触发搜索', async () => {
		const wrapper = mountBrowser();
		const input = wrapper.findComponent({ name: 'ElInput' });

		input.vm.$emit('update:modelValue', '关键字');
		await wrapper.find('.el-input-stub input').trigger('keyup.enter');
		await wrapper.find('.content-browser__search-button').trigger('click');
		await wrapper.find('.clear-button').trigger('click');

		expect(wrapper.emitted('update:searchModelValue')).toEqual([['关键字']]);
		expect(wrapper.emitted('search')).toEqual([[], [], []]);
	});

	it('渲染标题、操作区和面包屑插槽', () => {
		const wrapper = mountBrowser(
			{},
			{
				actions: '<button class="action-slot">导入</button>',
				breadcrumb: '<nav class="breadcrumb-slot">根目录</nav>',
			}
		);

		expect(wrapper.find('.content-browser__title').text()).toBe('视频');
		expect(wrapper.find('.action-slot').exists()).toBe(true);
		expect(wrapper.find('.breadcrumb-slot').exists()).toBe(true);
	});

	it('加载状态优先于错误、空状态和内容', () => {
		const wrapper = mountBrowser(
			{ loading: true, error: '请求失败', total: 1 },
			{ default: '<div class="content-slot">内容</div>' }
		);

		expect(wrapper.findComponent({ name: 'ElSkeleton' }).exists()).toBe(true);
		expect(wrapper.findComponent({ name: 'ElResult' }).exists()).toBe(false);
		expect(wrapper.findComponent({ name: 'ElEmpty' }).exists()).toBe(false);
		expect(wrapper.find('.content-slot').exists()).toBe(false);
		expect(wrapper.findComponent({ name: 'ElPagination' }).exists()).toBe(false);
	});

	it('错误状态优先于空状态和内容并可重试', async () => {
		const wrapper = mountBrowser(
			{ error: '网络连接失败', total: 1 },
			{ default: '<div class="content-slot">内容</div>' }
		);

		expect(wrapper.findComponent({ name: 'ElResult' }).props('subTitle')).toBe(
			'网络连接失败'
		);
		expect(wrapper.findComponent({ name: 'ElEmpty' }).exists()).toBe(false);
		expect(wrapper.find('.content-slot').exists()).toBe(false);

		await wrapper
			.findComponent({ name: 'ElResult' })
			.findComponent({ name: 'ElButton' })
			.trigger('click');
		expect(wrapper.emitted('retry')).toEqual([[]]);
	});

	it('空状态渲染说明和动作插槽', () => {
		const wrapper = mountBrowser(
			{},
			{ 'empty-actions': '<button class="empty-action">扫描并导入</button>' }
		);

		expect(wrapper.find('.content-browser__empty-title').text()).toBe(
			'暂无视频'
		);
		expect(wrapper.findComponent({ name: 'ElEmpty' }).props('description')).toBe(
			'扫描并导入视频后会显示在这里'
		);
		expect(wrapper.find('.empty-action').exists()).toBe(true);
		expect(wrapper.findComponent({ name: 'ElPagination' }).exists()).toBe(false);
	});

	it('有内容时渲染默认插槽和分页', () => {
		const wrapper = mountBrowser(
			{ total: 25 },
			{ default: '<div class="content-slot">视频列表</div>' }
		);
		const pagination = wrapper.findComponent({ name: 'ElPagination' });

		expect(wrapper.find('.content-slot').text()).toBe('视频列表');
		expect(pagination.props()).toMatchObject({
			currentPage: 1,
			pageSize: 12,
			pageSizes: [12, 24, 48],
			total: 25,
		});
	});

	it('分页变化只向调用方发送双向事件', () => {
		const wrapper = mountBrowser({ total: 25 });
		const pagination = wrapper.findComponent({ name: 'ElPagination' });

		pagination.vm.$emit('current-change', 2);
		pagination.vm.$emit('size-change', 24);

		expect(wrapper.emitted('update:page')).toEqual([[2]]);
		expect(wrapper.emitted('update:size')).toEqual([[24]]);
	});
});
