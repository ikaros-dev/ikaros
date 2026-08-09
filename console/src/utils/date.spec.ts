import { describe, expect, it } from 'vitest';
import { formatDate } from './date';

describe('formatDate', () => {
	it('按默认格式补零输出日期时间', () => {
		const date = new Date(2024, 0, 2, 3, 4, 5);

		expect(formatDate(date)).toBe('2024-01-02 03:04:05');
	});

	it('支持字符串日期和自定义格式', () => {
		const date = new Date(2025, 10, 9, 8, 7, 6);

		expect(formatDate(date.toISOString(), 'dd/MM/yyyy HH:mm')).toBe(
			formatDate(date, 'dd/MM/yyyy HH:mm')
		);
	});
});
