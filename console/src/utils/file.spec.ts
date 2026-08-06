import { describe, expect, it } from 'vitest';
import { getPostfix } from './file';

describe('文件扩展名展示', () => {
	it('提取文件扩展名并忽略查询参数', () => {
		expect(getPostfix('folder/archive.tar.gz?download=true')).toBe('gz');
	});

	it('空名称或无有效扩展名时返回空字符串', () => {
		expect(getPostfix('')).toBe('');
		expect(getPostfix('README')).toBe('');
		expect(getPostfix('filename.')).toBe('');
		expect(getPostfix('.profile')).toBe('');
	});
});
