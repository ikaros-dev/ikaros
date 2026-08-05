import { flushPromises, shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { EpisodeResource } from '@runikaros/api-client';

const apiMocks = vi.hoisted(() => ({
	findCollectionEpisode: vi.fn(),
	updateCollectionEpisode: vi.fn(),
}));

vi.mock('@/utils/api-client', () => ({
	apiClient: {
		collectionEpisode: {
			findCollectionEpisode: apiMocks.findCollectionEpisode,
			updateCollectionEpisode: apiMocks.updateCollectionEpisode,
		},
	},
}));

vi.mock('vue-i18n', () => ({
	useI18n: () => ({
		t: (key: string) => key,
	}),
}));

import ImageSequenceReader from './ImageSequenceReader.vue';

const resources: EpisodeResource[] = [
	{
		attachmentId: 'attachment-1',
		name: '第一页',
		url: 'https://example.com/image 1.jpg',
	},
	{
		attachmentId: 'attachment-2',
		name: '第二页',
		url: 'https://example.com/image 2.jpg',
	},
];

const mountReader = (
	props?: Partial<{ episodeId: string; resources: EpisodeResource[] }>
) =>
	shallowMount(ImageSequenceReader, {
		props: {
			episodeId: 'episode-id',
			resources,
			...props,
		},
	});

describe('图片序列阅读器', () => {
	beforeEach(() => {
		apiMocks.updateCollectionEpisode.mockResolvedValue({ status: 204 });
	});

	it('没有图片时不读取或保存进度', async () => {
		const wrapper = mountReader({ resources: [] });
		await flushPromises();

		expect(wrapper.find('el-empty-stub').exists()).toBe(true);
		expect(apiMocks.findCollectionEpisode).not.toHaveBeenCalled();
		expect(apiMocks.updateCollectionEpisode).not.toHaveBeenCalled();
	});

	it('恢复已有进度并显示对应图片', async () => {
		apiMocks.findCollectionEpisode.mockResolvedValue({
			data: { progress: 2, duration: 2 },
		});

		const wrapper = mountReader();
		await flushPromises();

		expect(apiMocks.findCollectionEpisode).toHaveBeenCalledWith({
			episodeId: 'episode-id',
		});
		expect(wrapper.find('img').attributes('src')).toBe(
			'https://example.com/image%202.jpg'
		);
		expect(wrapper.find('img').attributes('alt')).toBe('第二页');
		expect(apiMocks.updateCollectionEpisode).not.toHaveBeenCalled();
	});

	it('将越界进度裁剪后同步到服务端', async () => {
		apiMocks.findCollectionEpisode.mockResolvedValue({
			data: { progress: 99, duration: 99 },
		});

		mountReader();
		await flushPromises();

		expect(apiMocks.updateCollectionEpisode).toHaveBeenCalledWith({
			episodeId: 'episode-id',
			progress: 2,
			duration: 2,
		});
	});

	it('服务端没有收藏进度时创建初始进度', async () => {
		apiMocks.findCollectionEpisode.mockRejectedValue({
			isAxiosError: true,
			response: { status: 404 },
		});

		mountReader();
		await flushPromises();

		expect(apiMocks.updateCollectionEpisode).toHaveBeenCalledWith({
			episodeId: 'episode-id',
			progress: 1,
			duration: 2,
		});
	});

	it('翻页时保存新进度', async () => {
		apiMocks.findCollectionEpisode.mockResolvedValue({
			data: { progress: 1, duration: 2 },
		});
		const wrapper = mountReader();
		await flushPromises();

		wrapper
			.findComponent({ name: 'ElPagination' })
			.vm.$emit('current-change', 2);
		await flushPromises();

		expect(apiMocks.updateCollectionEpisode).toHaveBeenLastCalledWith({
			episodeId: 'episode-id',
			progress: 2,
			duration: 2,
		});
	});

	it('读取进度失败时展示错误状态', async () => {
		apiMocks.findCollectionEpisode.mockRejectedValue(
			new Error('network error')
		);
		const error = vi
			.spyOn(console, 'error')
			.mockImplementation(() => undefined);

		const wrapper = mountReader();
		await flushPromises();

		expect(error).toHaveBeenCalled();
		expect(wrapper.find('.progress-error').exists()).toBe(true);
		expect(apiMocks.updateCollectionEpisode).not.toHaveBeenCalled();
	});

	it('保存进度失败时展示错误状态', async () => {
		apiMocks.findCollectionEpisode.mockResolvedValue({
			data: { progress: 99, duration: 99 },
		});
		apiMocks.updateCollectionEpisode.mockRejectedValue(
			new Error('network error')
		);
		const error = vi
			.spyOn(console, 'error')
			.mockImplementation(() => undefined);

		const wrapper = mountReader();
		await flushPromises();

		expect(error).toHaveBeenCalled();
		expect(wrapper.find('.progress-error').exists()).toBe(true);
	});

	it('图片加载失败后可以触发重试', async () => {
		apiMocks.findCollectionEpisode.mockResolvedValue({
			data: { progress: 1, duration: 2 },
		});
		const wrapper = mountReader();
		await flushPromises();

		await wrapper.find('img').trigger('error');
		expect(wrapper.findComponent({ name: 'ElButton' }).exists()).toBe(true);

		await wrapper.findComponent({ name: 'ElButton' }).trigger('click');
		expect(wrapper.find('img').exists()).toBe(true);
	});
});
