import { FileStatus } from 'filepond';
import { describe, expect, it } from 'vitest';
import { detectFileExtensionType, hasActiveFileTransfers } from './file-pond';

describe('附件上传状态适配', () => {
	it('按文件扩展名生成 FilePond 校验类型', async () => {
		await expect(
			detectFileExtensionType({ name: '示例.VIDEO.MP4' })
		).resolves.toBe('.mp4');
		await expect(detectFileExtensionType({ name: 'README' })).resolves.toBe('');
	});

	it('仅将加载和上传过程识别为活动传输', () => {
		expect(
			hasActiveFileTransfers([
				{ status: FileStatus.LOADING },
				{ status: FileStatus.PROCESSING_QUEUED },
				{ status: FileStatus.PROCESSING },
			])
		).toBe(true);
		expect(
			hasActiveFileTransfers([
				{ status: FileStatus.LOAD_ERROR },
				{ status: FileStatus.PROCESSING_ERROR },
				{ status: FileStatus.PROCESSING_COMPLETE },
			])
		).toBe(false);
	});
});
