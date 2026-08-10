import { definePlugin } from '@runikaros/shared';
import { Headset, Picture, VideoCamera } from '@element-plus/icons-vue';
import { markRaw } from 'vue';
import SubjectDetails from '../subject/SubjectDetails.vue';
import MusicAlbumDetail from '../music/MusicAlbumDetail.vue';
import VideoContent from './VideoContent.vue';
import MusicContent from './MusicContent.vue';
import ImageContent from './ImageContent.vue';

export default definePlugin({
	name: 'Media',
	components: {},
	routes: [
		{
			parentName: 'Root',
			route: {
				path: '/videos',
				name: 'Videos',
				component: VideoContent,
				meta: {
					title: 'module.media.video.title',
					menu: {
						name: 'module.media.video.sidebar',
						group: 'content',
						icon: markRaw(VideoCamera),
						priority: 1,
					},
				},
			},
		},
		{
			parentName: 'Root',
			route: {
				path: '/music',
				name: 'Music',
				component: MusicContent,
				meta: {
					title: 'module.media.music.title',
					menu: {
						name: 'module.media.music.sidebar',
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
				path: '/images',
				name: 'Images',
				component: ImageContent,
				meta: {
					title: 'module.media.image.title',
					menu: {
						name: 'module.media.image.sidebar',
						group: 'content',
						icon: markRaw(Picture),
						priority: 3,
					},
				},
			},
		},
		{
			parentName: 'Root',
			route: {
				path: '/videos/details/:id',
				name: 'VideoDetails',
				component: SubjectDetails,
				meta: { title: 'module.subject.details.title', hidden: true },
			},
		},
		{
			parentName: 'Root',
			route: {
				path: '/music/details/:id',
				name: 'MusicDetails',
				component: MusicAlbumDetail,
				meta: { title: 'module.music.album.detail.title', hidden: true },
			},
		},
		{
			parentName: 'Root',
			route: {
				path: '/images/details/:id',
				name: 'ImageDetails',
				component: SubjectDetails,
				meta: { title: 'module.subject.details.title', hidden: true },
			},
		},
	],
});
