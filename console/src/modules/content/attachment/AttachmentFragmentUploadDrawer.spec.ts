import { flushPromises, shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const formatMocks = vi.hoisted(() => ({
	loadMediaFileFormatLookup: vi.fn(),
}));

vi.mock('@/utils/media-file-format', () => ({
	loadMediaFileFormatLookup: formatMocks.loadMediaFileFormatLookup,
}));

vi.mock('vue-i18n', () => ({
	useI18n: () => ({
		t: (key: string) => key,
	}),
}));

vi.mock('@/components/dialog/DialogMessage.vue', () => ({
	default: {
		name: 'DialogMessage',
		template: '<div />',
	},
}));

vi.mock('@/components/upload/AttachmentPondUpload.vue', () => ({
	default: {
		name: 'AttachmentPondUpload',
		template: '<div />',
	},
}));

import AttachmentFragmentUploadDrawer from './AttachmentFragmentUploadDrawer.vue';

const lookup = {
	accepts: ['.mp4'],
	categoryOf: vi.fn(),
	formatOf: vi.fn(),
	mimeTypeOf: vi.fn(),
};

const mountDrawer = () =>
	shallowMount(AttachmentFragmentUploadDrawer, {
		props: {
			visible: false,
		},
		global: {
			stubs: {
				ElDrawer: {
					template: '<div><slot name="header"/><slot/></div>',
				},
			},
		},
	});

describe('附件分片上传抽屉', () => {
	beforeEach(() => {
		formatMocks.loadMediaFileFormatLookup.mockReset();
	});

	it('白名单加载完成前不挂载上传组件', async () => {
		let resolveLookup: (value: typeof lookup) => void = () => undefined;
		formatMocks.loadMediaFileFormatLookup.mockReturnValue(
			new Promise((resolve) => {
				resolveLookup = resolve;
			})
		);
		const wrapper = mountDrawer();

		await wrapper.setProps({ visible: true });
		expect(
			wrapper.findComponent({ name: 'AttachmentPondUpload' }).exists()
		).toBe(false);

		resolveLookup(lookup);
		await flushPromises();
		expect(
			wrapper.findComponent({ name: 'AttachmentPondUpload' }).exists()
		).toBe(true);
	});

	it('白名单加载失败时阻止上传并允许重试', async () => {
		formatMocks.loadMediaFileFormatLookup
			.mockRejectedValueOnce(new Error('network error'))
			.mockResolvedValueOnce(lookup);
		const wrapper = mountDrawer();

		await wrapper.setProps({ visible: true });
		await flushPromises();
		expect(
			wrapper.findComponent({ name: 'AttachmentPondUpload' }).exists()
		).toBe(false);
		expect(wrapper.findComponent({ name: 'ElAlert' }).exists()).toBe(true);

		await wrapper.findComponent({ name: 'ElButton' }).trigger('click');
		await flushPromises();
		expect(formatMocks.loadMediaFileFormatLookup).toHaveBeenCalledTimes(2);
		expect(
			wrapper.findComponent({ name: 'AttachmentPondUpload' }).exists()
		).toBe(true);
	});

	it('服务端返回空白名单时阻止上传', async () => {
		formatMocks.loadMediaFileFormatLookup.mockResolvedValue({
			...lookup,
			accepts: [],
		});
		const wrapper = mountDrawer();

		await wrapper.setProps({ visible: true });
		await flushPromises();
		expect(
			wrapper.findComponent({ name: 'AttachmentPondUpload' }).exists()
		).toBe(false);
		expect(wrapper.findComponent({ name: 'ElAlert' }).exists()).toBe(true);
	});
});
