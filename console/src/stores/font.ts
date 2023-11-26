import { apiClient } from '@/utils/api-client';
import { defineStore } from 'pinia';

// import { apiClient } from '@/utils/api-client';

interface FontStoreState {
	data: string[];
}

export const useFontStore = defineStore('font', {
	state: (): FontStoreState => ({
		data: [],
	}),
	actions: {
		async fetchStaticFonts(): Promise<string[]> {
			try {
				const { data } = await apiClient.staticRes.listStaticsFonts();
				this.data = data;
			} catch (e) {
				console.error('Failed to fetch current user', e);
			}
			return this.data;
		},
		async getStaticFonts(): Promise<string[]> {
			if (this.data.length === 0) {
				await this.fetchStaticFonts();
			}
			return this.data;
		},
	},
});
