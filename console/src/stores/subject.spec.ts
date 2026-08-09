import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const searchSubjectById = vi.hoisted(() => vi.fn());

vi.mock('@/utils/api-client', () => ({
	apiClient: {
		subject: { searchSubjectById },
	},
}));

import { useSubjectStore } from './subject';

describe('条目缓存状态管理', () => {
	beforeEach(() => {
		setActivePinia(createPinia());
	});

	it('首次回源并缓存，后续直接返回缓存', async () => {
		const subject = { id: 'subject-id', name: 'Ikaros' };
		searchSubjectById.mockResolvedValue({ data: subject });
		const store = useSubjectStore();

		await expect(store.getSubjectById('subject-id')).resolves.toEqual(subject);
		await expect(store.getSubjectById('subject-id')).resolves.toEqual(subject);

		expect(searchSubjectById).toHaveBeenCalledTimes(1);
	});

	it('支持按 ID 和整体清除缓存', async () => {
		searchSubjectById.mockImplementation(({ id }) =>
			Promise.resolve({ data: { id, name: id } })
		);
		const store = useSubjectStore();
		await store.getSubjectById('first');
		await store.getSubjectById('second');

		await store.clearSubjectCacheById('first');
		expect([...store.cacheMap.keys()]).toEqual(['second']);

		store.clearCacheMap();
		expect(store.cacheMap.size).toBe(0);
	});

	it('接口异常时记录错误且不写入缓存', async () => {
		searchSubjectById.mockRejectedValue(new Error('network error'));
		const error = vi
			.spyOn(console, 'error')
			.mockImplementation(() => undefined);
		const store = useSubjectStore();

		await expect(store.fetchSubjectById('missing')).resolves.toBeUndefined();

		expect(error).toHaveBeenCalled();
		expect(store.cacheMap.size).toBe(0);
	});
});
