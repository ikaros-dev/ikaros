<template>
	<div class="music-album-detail">
		<div class="page-header">
			<el-button @click="goBack">{{ $t('module.music.back') }}</el-button>
			<h2>
				{{
					album?.name || album?.nameCn || $t('module.music.album.detail.title')
				}}
			</h2>
		</div>

		<el-card v-if="album" class="album-info-card">
			<div class="album-header">
				<el-image
					v-if="album.cover"
					:src="album.cover"
					style="width: 120px; height: 120px; border-radius: 8px"
					fit="cover"
				/>
				<div class="album-details">
					<h3>{{ album.name }}</h3>
					<p v-if="album.nameCn">{{ album.nameCn }}</p>
					<p v-if="album.description">{{ album.description }}</p>
					<p class="meta">
						<span>{{ $t('module.music.song_count') }}: {{ songs.length }}</span>
						<span v-if="album.score"
							>{{ $t('module.music.score') }}: {{ album.score }}</span
						>
					</p>
				</div>
			</div>
		</el-card>

		<div class="songs-section">
			<div class="section-header">
				<h3>{{ $t('module.music.songs') }}</h3>
				<el-button type="primary" size="small" @click="showAddSongDialog">
					{{ $t('module.music.add_song') }}
				</el-button>
			</div>

			<el-table
				:data="songs"
				v-loading="songsLoading"
				stripe
				style="width: 100%"
			>
				<el-table-column type="index" width="50" label="#" />
				<el-table-column
					prop="name"
					:label="$t('module.music.song_name')"
					min-width="200"
				>
					<template #default="scope">
						<div>
							<strong>{{ scope?.row?.name }}</strong>
							<small v-if="scope?.row?.nameCn"> / {{ scope?.row?.nameCn }}</small>
						</div>
					</template>
				</el-table-column>
				<el-table-column
					prop="group"
					:label="$t('module.music.group')"
					width="120"
				/>
				<el-table-column
					prop="sequence"
					:label="$t('module.music.track')"
					width="80"
				/>
				<el-table-column :label="$t('module.music.actions')" width="150">
					<template #default="scope">
						<el-button size="small" @click="editSong(scope.row)">
							{{ $t('module.music.edit') }}
						</el-button>
						<el-button
							size="small"
							type="danger"
							@click="deleteSong(scope.row)"
						>
							{{ $t('module.music.delete') }}
						</el-button>
					</template>
				</el-table-column>
			</el-table>
		</div>

		<!-- Song Dialog -->
		<el-dialog
			v-model="songDialogVisible"
			:title="
				editingSong ? $t('module.music.edit_song') : $t('module.music.add_song')
			"
			width="500px"
		>
			<el-form :model="songForm" label-width="100px">
				<el-form-item :label="$t('module.music.song_name')" required>
					<el-input v-model="songForm.name" />
				</el-form-item>
				<el-form-item :label="$t('module.music.song_name_cn')">
					<el-input v-model="songForm.nameCn" />
				</el-form-item>
				<el-form-item :label="$t('module.music.group')">
					<el-select v-model="songForm.group" style="width: 100%">
						<el-option label="MAIN" value="MAIN" />
						<el-option label="OPENING_SONG" value="OPENING_SONG" />
						<el-option label="ENDING_SONG" value="ENDING_SONG" />
						<el-option
							label="ORIGINAL_SOUND_TRACK"
							value="ORIGINAL_SOUND_TRACK"
						/>
						<el-option label="MUSIC_DIST1" value="MUSIC_DIST1" />
						<el-option label="MUSIC_DIST2" value="MUSIC_DIST2" />
						<el-option label="MUSIC_DIST3" value="MUSIC_DIST3" />
						<el-option label="MUSIC_DIST4" value="MUSIC_DIST4" />
						<el-option label="MUSIC_DIST5" value="MUSIC_DIST5" />
					</el-select>
				</el-form-item>
				<el-form-item :label="$t('module.music.track')">
					<el-input-number v-model="songForm.sequence" :min="1" :step="1" />
				</el-form-item>
				<el-form-item :label="$t('module.music.description')">
					<el-input v-model="songForm.description" type="textarea" :rows="2" />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="songDialogVisible = false">
					{{ $t('module.music.cancel') }}
				</el-button>
				<el-button type="primary" @click="saveSong" :loading="savingSong">
					{{ $t('module.music.save') }}
				</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { apiClient } from '@/utils/api-client';

import {
	ElInput,
	ElForm,
	ElFormItem,
	ElButton,
	ElTable,
	ElTableColumn,
	ElDialog,
	ElOption,
	ElSelect,
} from 'element-plus';

interface MusicAlbum {
	id: string;
	name: string;
	nameCn: string;
	cover: string;
	description: string;
	airTime: string;
	score: number;
	songCount: number;
}

interface Song {
	id: string;
	subjectId: string;
	name: string;
	nameCn: string;
	description: string;
	airTime: string;
	sequence: number;
	group: string;
	attachmentId: string;
	duration: number;
}

const route = useRoute();
const router = useRouter();
const albumId = route.params.id as string;

const album = ref<MusicAlbum | null>(null);
const songs = ref<Song[]>([]);
const songsLoading = ref(false);
const songDialogVisible = ref(false);
const editingSong = ref(false);
const savingSong = ref(false);
const songForm = ref<Partial<Song>>({});

const fetchAlbum = async () => {
	try {
		const response = await apiClient.get(`/api/core/music/album/${albumId}`);
		album.value = response.data;
	} catch (e) {
		console.error('Failed to fetch album:', e);
	}
};

const fetchSongs = async () => {
	songsLoading.value = true;
	try {
		const response = await apiClient.get(
			`/api/core/music/album/${albumId}/songs`
		);
		songs.value = response.data || [];
	} catch (e) {
		console.error('Failed to fetch songs:', e);
	} finally {
		songsLoading.value = false;
	}
};

const showAddSongDialog = () => {
	editingSong.value = false;
	songForm.value = {
		subjectId: albumId,
		group: 'MAIN',
		sequence: songs.value.length + 1,
	};
	songDialogVisible.value = true;
};

const editSong = (song: Song) => {
	editingSong.value = true;
	songForm.value = { ...song };
	songDialogVisible.value = true;
};

const saveSong = async () => {
	savingSong.value = true;
	try {
		if (editingSong.value && songForm.value.id) {
			await apiClient.put('/api/core/music/song', songForm.value);
		} else {
			await apiClient.post('/api/core/music/song', songForm.value);
		}
		songDialogVisible.value = false;
		await fetchSongs();
	} catch (e) {
		console.error('Failed to save song:', e);
	} finally {
		savingSong.value = false;
	}
};

const deleteSong = async (song: Song) => {
	try {
		await apiClient.delete(`/api/core/music/song/${song.id}`);
		await fetchSongs();
	} catch (e) {
		console.error('Failed to delete song:', e);
	}
};

const goBack = () => {
	router.push('/music/albums');
};

onMounted(() => {
	fetchAlbum();
	fetchSongs();
});
</script>

<style scoped>
.music-album-detail {
	padding: 20px;
}
.page-header {
	display: flex;
	align-items: center;
	gap: 16px;
	margin-bottom: 20px;
}
.album-info-card {
	margin-bottom: 20px;
}
.album-header {
	display: flex;
	gap: 20px;
	align-items: flex-start;
}
.album-details h3 {
	margin: 0 0 8px;
}
.album-details p {
	margin: 4px 0;
	color: #666;
}
.album-details .meta {
	margin-top: 12px;
	display: flex;
	gap: 16px;
	font-size: 13px;
	color: #999;
}
.songs-section {
	margin-top: 20px;
}
.section-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 16px;
}
</style>
