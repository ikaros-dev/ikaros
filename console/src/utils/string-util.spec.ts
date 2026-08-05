import { describe, expect, it, vi } from 'vitest';
import {
	base64Decode,
	base64Encode,
	copyValue2Clipboard,
	formatFileSize,
	objectToMap,
} from './string-util';

describe('字符串工具', () => {
	it('对 Unicode 文本进行 Base64 编解码', () => {
		const raw = 'Ikaros 测试';

		expect(base64Decode(base64Encode(raw))).toBe(raw);
		expect(base64Encode(undefined)).toBe('');
		expect(base64Decode(undefined)).toBe('');
	});

	it('格式化文件大小', () => {
		expect(formatFileSize(0)).toBe('0 Bytes');
		expect(formatFileSize(1024)).toBe('1.00 KB');
		expect(formatFileSize(1536)).toBe('1.50 KB');
	});

	it('将嵌套对象转换为扁平 Map', () => {
		const result = objectToMap({
			user: { name: 'Ikaros', profile: { active: true } },
			tags: ['anime'],
			nullable: null,
		});

		expect(Object.fromEntries(result)).toEqual({
			'user.name': 'Ikaros',
			'user.profile.active': true,
			tags: ['anime'],
			nullable: null,
		});
	});

	it('优先使用 Clipboard API', async () => {
		const writeText = vi.fn().mockResolvedValue(undefined);
		Object.defineProperty(navigator, 'clipboard', {
			configurable: true,
			value: { writeText },
		});
		Object.defineProperty(window, 'isSecureContext', {
			configurable: true,
			value: true,
		});

		await copyValue2Clipboard('copied');

		expect(writeText).toHaveBeenCalledWith('copied');
	});

	it('非安全环境使用文本域复制', async () => {
		Object.defineProperty(navigator, 'clipboard', {
			configurable: true,
			value: undefined,
		});
		Object.defineProperty(window, 'isSecureContext', {
			configurable: true,
			value: false,
		});
		Object.defineProperty(document, 'execCommand', {
			configurable: true,
			value: vi.fn().mockReturnValue(true),
		});

		await expect(copyValue2Clipboard('fallback')).resolves.toBe('fallback');

		expect(document.execCommand).toHaveBeenCalledWith('copy');
		expect(document.querySelector('textarea')).toBeNull();
	});
});
