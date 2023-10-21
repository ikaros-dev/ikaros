<script setup lang="ts">
import { Episode } from '@runikaros/api-client';
import { computed } from 'vue';
import {
	ElButton,
	ElDescriptions,
	ElDescriptionsItem,
	ElDialog,
	ElMessage,
	ElPopconfirm,
	ElRow,
	ElCol,
	ElCard,
} from 'element-plus';
import { base64Encode } from '@/utils/string-util';
// eslint-disable-next-line no-unused-vars
import { apiClient } from '@/utils/api-client';
import { AttachmentReferenceTypeEnum } from '@runikaros/api-client';
import { isVideo } from '@/utils/file';

const props = withDefaults(
	defineProps<{
		visible: boolean;
		episode: Episode | undefined;
		multiResource?: boolean;
	}>(),
	{
		visible: false,
		multiResource: false,
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'close'): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'removeEpisodeFileBind'): void;
}>();

const dialogVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

const removeEpisodeAttachmentRef = async () => {
	// @ts-ignore
	const resouce = props.episode.resources[0];
	if (!resouce || !resouce.episodeId || !resouce.attachmentId) {
		ElMessage.warning('操作无效，您当前剧集并未绑定资源文件');
		return;
	}
	await apiClient.attachmentRef.removeByTypeAndAttachmentIdAndReferenceId({
		attachmentReference: {
			type: 'EPISODE' as AttachmentReferenceTypeEnum,
			attachmentId: resouce.attachmentId,
			referenceId: resouce.episodeId,
		},
	});
	ElMessage.success('移除剧集和附件绑定成功');
	dialogVisible.value = false;
	emit('removeEpisodeFileBind');
};

// eslint-disable-next-line no-unused-vars
const urlIsArachivePackage = (url: string | undefined): boolean => {
	return !url || url.endsWith('zip') || url.endsWith('7z');
};
</script>

<template>
	<el-dialog v-model="dialogVisible" title="剧集详情" width="70%">
		<el-descriptions border :column="1">
			<el-descriptions-item label="原始名称">
				{{ episode?.name }}
			</el-descriptions-item>
			<el-descriptions-item label="中文名称">
				{{ episode?.name_cn }}
			</el-descriptions-item>
			<el-descriptions-item label="放送时间">
				{{ episode?.air_time }}
			</el-descriptions-item>
			<el-descriptions-item label="序列号">
				{{ episode?.sequence }}
			</el-descriptions-item>
			<el-descriptions-item label="描述">
				{{ episode?.description }}
			</el-descriptions-item>
			<el-descriptions-item label="资源">
				<div v-if="episode?.resources && episode?.resources.length > 0">
					<div v-if="!props.multiResource" align="center">
						<router-link
							:to="
								'/attachments?searchName=' +
								base64Encode(episode?.resources[0].name)
							"
							>{{ episode?.resources[0].name }}</router-link
						>
						<video
							v-if="isVideo(episode.resources[0].url as string)"
							style="width: 100%"
							:src="episode.resources[0].url"
							controls
							preload="metadata"
						>
							您的浏览器不支持这个格式的视频
						</video>
						<span v-else>
							当前资源文件非视频文件、或者不可读取，如是视频文件且需读取，请先从远端拉取。
						</span>
					</div>
					<el-row :gutter="12" :span="24">
						<el-col
							v-for="res in episode?.resources"
							:key="res.attachmentId"
							:span="8"
						>
							<router-link
								:to="'/attachments?searchName=' + base64Encode(res.name)"
							>
								<el-card shadow="hover">
									{{ res.name }}
								</el-card>
							</router-link>
						</el-col>
					</el-row>
					<!-- <el-descriptions border :column="1">
						<el-descriptions-item
							v-for="res in episode?.resources"
							:key="res.attachmentId"
							label="附件列表"
						>
							<router-link
								:to="'/attachments?searchName=' + base64Encode(res.name)"
								>{{ res.name }}</router-link
							>
						</el-descriptions-item>
					</el-descriptions> -->
				</div>
				<span v-else> 当前剧集暂未绑定资源文件 </span>
			</el-descriptions-item>
		</el-descriptions>

		<template #footer>
			<el-popconfirm
				title="确定移除绑定吗？"
				width="180"
				@confirm="removeEpisodeAttachmentRef"
			>
				<template #reference>
					<el-button plain type="danger"> 移除资源绑定</el-button>
				</template>
			</el-popconfirm>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
