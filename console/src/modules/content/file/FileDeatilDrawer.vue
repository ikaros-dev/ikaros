<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { FileEntity } from '@runikaros/api-client';
import { fileTypeMap } from '@/modules/common/constants';
import { formatFileSize } from '@/utils/string-util';
import { computed, nextTick, ref } from 'vue';
import {
	ElButton,
	ElCol,
	ElDescriptions,
	ElDescriptionsItem,
	ElDrawer,
	ElInput,
	ElMessage,
	ElPopconfirm,
	ElRow,
} from 'element-plus';

const props = withDefaults(
	defineProps<{
		visible: boolean;
		defineFile: FileEntity;
	}>(),
	{
		visible: true,
		defineFile: () => ({}),
	}
);

const editable = ref(false);
const deleting = ref(false);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'update:defineFile', file: FileEntity): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'delete', file: FileEntity): void;
}>();

const drawerVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

const file = computed({
	get() {
		return props.defineFile;
	},
	set(value) {
		emit('update:defineFile', value);
	},
});

const isImage = computed(() => {
	// @ts-ignore
	if (!file.value || !file.value.type) {
		return false;
	}
	// @ts-ignore
	return file.value.type.startsWith('IMAGE');
});

const isVideo = computed(() => {
	// @ts-ignore
	if (!file.value || !file.value.type) {
		return false;
	}
	// @ts-ignore
	return file.value.type.startsWith('VIDEO');
});

const isVoice = computed(() => {
	// @ts-ignore
	if (!file.value || !file.value.type) {
		return false;
	}
	// @ts-ignore
	return file.value.type.startsWith('VOICE');
});

const handleDelete = async () => {
	try {
		deleting.value = true;
		await apiClient.file
			.deleteFile({
				id: file.value.id as number,
			})
			.then(() => {
				ElMessage.success('删除文件成功：' + file.value.name);
				emit('delete', file.value);
				drawerVisible.value = false;
			});
	} catch (err) {
		console.error(err);
	} finally {
		setTimeout(() => {
			deleting.value = false;
		}, 400);
	}
};

const nameInput = ref(null);
// eslint-disable-next-line no-unused-vars
const handleEditName = () => {
	editable.value = !editable.value;
	if (editable.value) {
		nextTick(() => {
			// @ts-ignore
			nameInput.value.focus();
		});
	}
};

const handleUpdateName = async () => {
	if (file.value.name) {
		ElMessage.error('文件名称不能为空！');
	}
	try {
		await apiClient.file.updateFile({
			fileEntity: file.value,
		});
	} catch (error) {
		console.error(error);
	} finally {
		editable.value = false;
	}
};

const getCompleteFileUrl = (reactiveUrl: string | undefined): string => {
	var curPageUrl = window.location.href;
	var pathName = window.location.pathname;
	var localhostPath = curPageUrl.substring(0, curPageUrl.indexOf(pathName));
	return localhostPath + reactiveUrl;
};

const handleClose = (done: () => void) => {
	done();
	drawerVisible.value = false;
};
</script>

<template>
	<el-drawer
		v-model="drawerVisible"
		title="文件详情"
		direction="ltr"
		:before-close="handleClose"
		size="40%"
	>
		<el-row>
			<el-col :lg="24" :md="24" :sm="24" :xl="24" :xs="24">
				<div v-if="file.canRead" class="attach-detail-img pb-3">
					<a
						v-if="isImage"
						:href="getCompleteFileUrl(file.url)"
						target="_blank"
					>
						<img
							:src="file.url"
							class="file-detail-preview-img"
							loading="lazy"
						/>
					</a>
					<video
						v-else-if="isVideo"
						style="width: 100%"
						:src="getCompleteFileUrl(file.url)"
						controls
						preload="metadata"
					>
						您的浏览器不支持这个格式的视频
					</video>
					<audio
						v-else-if="isVoice"
						controls
						:src="getCompleteFileUrl(file.url)"
					>
						您的浏览器不支持这个格式的音频
					</audio>
					<div v-else>此文件不支持预览</div>
				</div>
				<div v-else>文件不可读取，需要从远端拉取下来。</div>
			</el-col>
		</el-row>

		<br />
		<br />

		<el-row :gutter="24" type="flex">
			<el-col :lg="24" :md="24" :sm="24" :xl="24" :xs="24">
				<el-descriptions
					title="文件详情"
					:column="1"
					size="large"
					border
					direction="vertical"
				>
					<el-descriptions-item label="ID">
						{{ file.id }}
					</el-descriptions-item>
					<el-descriptions-item label="名称">
						<span v-if="editable">
							<el-input
								ref="nameInput"
								v-model="file.name"
								@blur="handleUpdateName"
								@pressEnter="handleUpdateName"
							/>
						</span>
						<span v-else>
							{{ file.name }}
						</span>
					</el-descriptions-item>
					<el-descriptions-item label="类型">
						{{ fileTypeMap.get(file.type as string) }}
					</el-descriptions-item>
					<el-descriptions-item label="大小">
						{{ formatFileSize(file.size) }}
					</el-descriptions-item>
					<el-descriptions-item label="创建时间：">
						{{ file.createTime }}
					</el-descriptions-item>
					<el-descriptions-item v-if="file.md5" label="MD5">
						{{ file.md5 }}
					</el-descriptions-item>
					<el-descriptions-item v-if="file.aesKey" label="AesKey">
						{{ file.aesKey }}
					</el-descriptions-item>
					<el-descriptions-item v-if="file.url" label="URL">
						<a :href="file.url" target="_blank">{{ file.url }}</a>
					</el-descriptions-item>
					<el-descriptions-item label="原始名称">
						{{ file.originalName }}
					</el-descriptions-item>
					<el-descriptions-item v-if="file.originalPath" label="原始路径">
						{{ file.originalPath }}
					</el-descriptions-item>
				</el-descriptions>
			</el-col>
		</el-row>

		<template #footer>
			<el-popconfirm
				title="你确定要删除该文件？"
				confirm-button-text="确定"
				cancel-button-text="取消"
				confirm-button-type="danger"
				@confirm="handleDelete"
			>
				<template #reference>
					<el-button type="danger" :loading="deleting">删除</el-button>
				</template>
			</el-popconfirm>
		</template>
	</el-drawer>
</template>

<style lang="scss" scoped>
.file-detail-preview-img {
	width: 100%;
	height: 100%;
	border-radius: 5px;
}
</style>
