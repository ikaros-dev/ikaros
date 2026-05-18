// @ts-nocheck
import { defineStore } from 'pinia';
import { apiClient } from '@/utils/api-client';
import { Subject } from '@runikaros/api-client';

interface SubjectStoreState {
	cacheMap: Map<number, Subject>;
}

export const useSubjectStore = defineStore('subject', {
	state: (): SubjectStoreState => ({
		cacheMap: new Map<number, Subject>(),
	}),
	actions: {
		async fetchSubjectById(id: string): Promise<Subject> {
			try {
				const { data } = await apiClient.subject.searchSubjectById({ id: id });
				return data;
			} catch (e) {
				console.error('Failed to fetch subject by id', e);
			}
		},
		async getSubjectById(id: string): Promise<Subject> {
			let sub = this.cacheMap.get(id);
			if (!sub) {
				sub = await this.fetchSubjectById(id);
				this.cacheMap.set(id, sub);
			}
			return sub;
		},
		async clearSubjectCacheById(id: string) {
			this.cacheMap.delete(id);
		},
		clearCacheMap() {
			this.cacheMap.clear();
		},
	},
});
