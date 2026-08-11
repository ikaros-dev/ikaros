import { flushPromises, shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => ({
	preview: vi.fn(),
	get: vi.fn(),
}));

vi.mock('@/utils/api-client', () => ({
	apiClient: {
		binding: { previewLocalDirectoryBinding: mocks.preview },
		get: mocks.get,
	},
}));

vi.mock('@/utils/string-util', () => ({ base64Encode: (value: string) => value }));
vi.mock('vue-i18n', () => ({ useI18n: () => ({ t: (key: string) => key }) }));

import LocalDirectoryBindingDialog from './LocalDirectoryBindingDialog.vue';

const mountDialog = (props: Record<string, unknown> = {}) =>
	shallowMount(LocalDirectoryBindingDialog, {
		props: { visible: true, ...props },
		global: {
			stubs: {
				ElDialog: { template: '<div><slot/><slot name="footer"/></div>' },
				FileSourceManagerDialog: true,
				ScanDirectorySelectDialog: true,
			},
		},
	});

const stateOf = (wrapper: ReturnType<typeof mountDialog>) =>
	(wrapper.vm.$ as unknown as { setupState: Record<string, any> }).setupState;

describe('统一显式扫描导入对话框', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		mocks.preview.mockResolvedValue({ data: { items: [] } });
		mocks.get.mockResolvedValue({ data: { items: [] } });
	});

	it.each([
		{},
		{ directoryId: 'dir-1' },
		{ mode: 'AUDIO' },
		{ directoryId: 'dir-1', mode: 'IMAGE' },
	])('入口 %o 打开时不自动扫描', async (props) => {
		mountDialog(props);
		await flushPromises();
		expect(mocks.preview).not.toHaveBeenCalled();
		expect(mocks.get).not.toHaveBeenCalled();
	});

	it('用户点击扫描后才预览，并使用显式选择的目录和模式', async () => {
		const wrapper = mountDialog({ mode: 'AUDIO' });
		const state = stateOf(wrapper);
		state.chooseDirectory('dir-1');
		await state.scan();

		expect(mocks.preview).toHaveBeenCalledTimes(1);
		expect(mocks.preview).toHaveBeenCalledWith({
			localScanPreviewRequest: { directory_id: 'dir-1', mode: 'AUDIO' },
		});
	});

	it('没有文件源时可以进入管理弹窗并在变更后返回目录选择', () => {
		const state = stateOf(mountDialog());
		state.directorySelectVisible = true;
		state.manageSources();

		expect(state.directorySelectVisible).toBe(false);
		expect(state.fileSourceManagerVisible).toBe(true);

		state.onFileSourcesChanged();
		expect(state.fileSourceManagerVisible).toBe(false);
		expect(state.directorySelectVisible).toBe(true);
	});

	it('搜索已有条目使用 keyword/types 且按模式收紧类型', async () => {
		const wrapper = mountDialog({ directoryId: 'dir-1', mode: 'EPISODE' });
		await stateOf(wrapper).searchSubjects('片名');

		expect(mocks.get).toHaveBeenCalledWith('/api/v1/subjects/condition', {
			params: { page: 1, size: 20, keyword: '片名', types: 'VIDEO,ANIME,REAL' },
		});
		expect(mocks.preview).not.toHaveBeenCalled();
	});

	it('视频新建条目默认为 VIDEO，音乐和图片固定类型', () => {
		const video = stateOf(mountDialog({ mode: 'EPISODE' }));
		expect(video.newSubject.type).toBe('VIDEO');
		expect(video.subjectTypes).toEqual(['VIDEO', 'ANIME', 'REAL']);
		expect(stateOf(mountDialog({ mode: 'AUDIO' })).subjectTypes).toEqual(['MUSIC']);
		expect(stateOf(mountDialog({ mode: 'IMAGE' })).subjectTypes).toEqual(['COMIC']);
	});

	it('预览接口失败时保留可见错误状态', async () => {
		mocks.preview.mockRejectedValue(new Error('扫描失败'));
		const state = stateOf(mountDialog({ directoryId: 'dir-1', mode: 'IMAGE' }));
		await state.scan();
		expect(state.errorMessage).toBe('扫描失败');
	});

	it('新建条目类型不符合模式时禁止确认', async () => {
		mocks.preview.mockResolvedValue({
			data: { items: [{ attachment_id: 'video-1', role: 'PRIMARY' }] },
		});
		const state = stateOf(
			mountDialog({ directoryId: 'dir-1', mode: 'EPISODE' })
		);
		await state.scan();
		state.subjectSource = 'new';
		state.newSubject.name = '示例';
		state.newSubject.type = 'MUSIC';
		expect(state.canConfirm).toBe(false);
	});
});
