<template>
	<div class="music-albums">
		<div class="page-header">
			<h2>{{ $t('module.music.title') }}</h2>
			<el-button type="primary" @click="handleCreate">
				{{ $t('module.music.create_album') }}
			</el-button>
		</div>

		<el-table :data="albums" v-loading="loading" stripe style="width: 100%">
			<el-table-column prop="name" :label="$t('module.music.name')" min-width="200">
				<template #default="scope">
					<div class="album-info">
						<el-image
							v-if="scope.row.cover"
							:src="scope.row.cover"
							style="width: 48px; height: 48px; border-radius: 4px"
							fit="cover"
						/>
						<div class="album-meta">
							<router-link :to="`/music/album/detail/${scope.row.id}`">
								{{ scope.row.name || scope.row.nameCn }}
							</router-link>
							<small v-if="scope.row.nameCn && scope.row.name">
								{{ scope.row.nameCn }}
							</small>
						</div>
					</div>
				</template>
			</el-table-column>
			<el-table-column prop="songCount" :label="$t('module.music.song_count')" width="100" />
			<el-table-column prop="score" :label="$t('module.music.score')" width="80" />
			<el-table-column :label="$t('module.music.air_time')" width="120">
				<template #default="scope">
					{{ formatDate(scope.row.airTime) }}
				</template>
			</el-table-column>
			<el-table-column :label="$t('module.music.actions')" width="200" fixed="right">
				<template #default="scope">
					<el-button size="small" @click="handleEdit(scope.row)">
						{{ $t('module.music.edit') }}
					</el-button>
					<el-button
						size="small"
						type="danger"
						@click="handleDelete(scope.row)"
					>
						{{ $t('module.music.delete') }}
					</el-button>
				</template>
			</el-table-column>
		</el-table>

		<div class="pagination-wrapper">
			<el-pagination
				v-model:current-page="page"
				v-model:page-size="size"
				:total="total"
				:page-sizes="[10, 20, 50, 100]"
				layout="total, sizes, prev, pager, next, jumper"
				@size-change="onPageChange"
				@current-change="onPageChange"
			/>
		</div>

		<!-- Album Edit Dialog -->
		<el-dialog
			v-model="dialogVisible"
			:title="isEdit ? $t('module.music.edit_album') : $t('module.music.create_album')"
			width="600px"
		>
			<el-form :model="form" label-width="100px">
				<el-form-item :label="$t('module.music.name')" required>
					<el-input v-model="form.name" />
				</el-form-item>
				<el-form-item :label="$t('module.music.name_cn')">
					<el-input v-model="form.nameCn" />
				</el-form-item>
				<el-form-item :label="$t('module.music.description')">
					<el-input v-model="form.description" type="textarea" :rows="3" />
				</el-form-item>
				<el-form-item :label="$t('module.music.cover')">
					<el-input v-model="form.cover" placeholder="Cover URL" />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="dialogVisible = false">
					{{ $t('module.music.cancel') }}
				</el-button>
				<el-button type="primary" @click="handleSave" :loading="saving">
					{{ $t('module.music.save') }}
				</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { apiClient } from '@/utils/api-client';

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

const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const albums = ref<MusicAlbum[]>([]);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const dialogVisible = ref(false);
const isEdit = ref(false);
const form = ref<Partial<MusicAlbum>>({});

const fetchAlbums = async () => {
	loading.value = true;
	try {
		const response = await apiClient.get(
			`/api/core/music/albums/${page.value}/${size.value}`
		);
		albums.value = response.data.items || [];
		total.value = response.data.total || 0;
	} catch (e) {
		console.error('Failed to fetch music albums:', e);
	} finally {
		loading.value = false;
	}
};

const handleCreate = () => {
	isEdit.value = false;
	form.value = {};
	dialogVisible.value = true;
};

const handleEdit = (album: MusicAlbum) => {
	isEdit.value = true;
	form.value = { ...album };
	dialogVisible.value = true;
};

const handleSave = async () => {
	saving.value = true;
	try {
		if (isEdit.value && form.value.id) {
			await apiClient.put('/api/core/subject', form.value);
		} else {
			await apiClient.post('/api/core/subject', {
				...form.value,
				type: 'MUSIC',
			});
		}
		dialogVisible.value = false;
		await fetchAlbums();
	} catch (e) {
		console.error('Failed to save album:', e);
	} finally {
		saving.value = false;
	}
};

const handleDelete = async (album: MusicAlbum) => {
	try {
		await apiClient.delete(`/api/core/subject/${album.id}`);
		await fetchAlbums();
	} catch (e) {
		console.error('Failed to delete album:', e);
	}
};

const onPageChange = () => {
	fetchAlbums();
};

const formatDate = (date: string) => {
	if (!date) return '';
	return new Date(date).toLocaleDateString('zh-CN');
};

onMounted(() => {
	fetchAlbums();
});
</script>

<style scoped>
.music-albums {
	padding: 20px;
}
.page-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 20px;
}
.album-info {
	display: flex;
	align-items: center;
	gap: 12px;
}
.album-meta {
	display: flex;
	flex-direction: column;
}
.album-meta small {
	color: #999;
	font-size: 12px;
}
.pagination-wrapper {
	margin-top: 20px;
	display: flex;
	justify-content: center;
}
</style>
