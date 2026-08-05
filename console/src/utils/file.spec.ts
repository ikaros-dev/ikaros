import { describe, expect, it } from 'vitest';
import { getPostfix, isDoucment, isImage, isVideo, isVoice } from './file';

describe('文件类型判断', () => {
	it('提取文件扩展名并忽略查询参数', () => {
		expect(getPostfix('folder/archive.tar.gz?download=true')).toBe('gz');
		expect(getPostfix('')).toBe('');
	});

	it('识别图片、视频、音频和文档', () => {
		expect(isImage('cover.webp')).toBe(true);
		expect(isVideo('episode.m3u8')).toBe(true);
		expect(isVoice('theme.flac')).toBe(true);
		expect(isDoucment('subtitle.ass')).toBe(true);
		expect(isImage('archive.zip')).toBe(false);
	});
});
