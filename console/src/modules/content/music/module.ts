import { definePlugin } from '@runikaros/shared';
import { Headset } from '@element-plus/icons-vue';
import MusicAlbums from './MusicAlbums.vue';
import MusicAlbumDetail from './MusicAlbumDetail.vue';
import { markRaw } from 'vue';

export default definePlugin({
	name: 'Music',
	components: {},
	routes: [
		{
			parentName: 'Root',
			route: {
				path: '/music/albums',
				name: 'MusicAlbums',
				component: MusicAlbums,
				meta: {
					title: 'module.music.title',
					menu: {
						name: 'module.music.sidebar',
						group: 'content',
						icon: markRaw(Headset),
						priority: 2,
					},
				},
			},
		},
		{
			parentName: 'Root',
			route: {
				path: '/music/album/detail/:id',
				name: 'MusicAlbumDetail',
				component: MusicAlbumDetail,
				meta: {
					title: 'module.music.album.detail.title',
					hidden: true,
				},
			},
		},
	],
});
