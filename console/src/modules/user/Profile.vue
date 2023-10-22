<script setup lang="ts">
import type { Attachment, UpdateUserRequest } from '@runikaros/api-client';
import { useUserStore } from '@/stores/user';
import { apiClient } from '@/utils/api-client';
import { onMounted, ref } from 'vue';
import { Picture } from '@element-plus/icons-vue';
import {
	ElMessage,
	ElForm,
	ElFormItem,
	ElTabs,
	ElTabPane,
	ElInput,
	ElButton,
	ElRow,
	ElCol,
	ElImage,
} from 'element-plus';
import AttachmentSelectDialog from '../content/attachment/AttachmentSelectDialog.vue';

const userStore = useUserStore();

const profile = ref<UpdateUserRequest>({
	username: '',
	nickname: '',
	avatar: '',
	introduce: '',
	site: '',
});
const passwordReq = ref({
	username: '',
	oldPassword: '',
	newPassword: '',
});
const updateProfile = async () => {
	await apiClient.user
		.updateUser({
			updateUserRequest: profile.value,
		})
		.then(() => {
			ElMessage.success('更新基本信息成功');
			userStore.fetchCurrentUser();
		});
};
const updatePassword = async () => {
	await apiClient.user
		.changeUserPassword({
			username: passwordReq.value.username,
			oldPassword: passwordReq.value.oldPassword,
			newPassword: passwordReq.value.newPassword,
		})
		.then(() => {
			ElMessage.success('更新密码成功');
			userStore.fetchCurrentUser();
		});
};

const initProfileAndUsername = async () => {
	const userEntity = userStore.currentUser?.entity;
	profile.value.username = userEntity?.username as string;
	profile.value.nickname = userEntity?.nickname as string;
	profile.value.avatar = userEntity?.avatar as string;
	profile.value.introduce = userEntity?.introduce as string;
	profile.value.site = userEntity?.site as string;
	passwordReq.value.username = userEntity?.username as string;
};

const attachmentSelectDialogVisible = ref(false);
const onAttachmentSelectDialogColseWithAttachment = (
	attachment: Attachment
) => {
	// console.log('receive file entity: ', file);
	profile.value.avatar = attachment.url as string;
	attachmentSelectDialogVisible.value = false;
};

onMounted(initProfileAndUsername);
</script>

<template>
	<attachment-select-dialog
		v-model:visible="attachmentSelectDialogVisible"
		@close-with-attachment="onAttachmentSelectDialogColseWithAttachment"
	/>

	<el-form
		:model="profile"
		label-width="90"
		label-position="top"
		size="default"
	>
		<el-tabs>
			<el-tab-pane label="基本信息">
				<ElRow :gutter="12">
					<ElCol :xs="24" :sm="24" :md="24" :lg="16" :xl="16">
						<el-form-item label="昵称">
							<el-input v-model="profile.nickname" clearable />
						</el-form-item>

						<el-form-item label="头像">
							<el-input v-model="profile.avatar" clearable>
								<template #prepend>
									<el-button
										:icon="Picture"
										plain
										@click="attachmentSelectDialogVisible = true"
									/>
								</template>
							</el-input>
						</el-form-item>

						<el-form-item label="个人主页">
							<el-input v-model="profile.site" clearable />
						</el-form-item>

						<el-form-item label="个人介绍">
							<el-input
								v-model="profile.introduce"
								:autosize="{ minRows: 2 }"
								maxlength="2000"
								rows="10"
								show-word-limit
								type="textarea"
							/>
						</el-form-item>

						<el-form-item>
							<el-button type="primary" @click="updateProfile">保存</el-button>
						</el-form-item>
					</ElCol>
					<ElCol :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
						<span v-if="profile.avatar">
							<el-image
								style="width: 100%; border-radius: 5px"
								:src="profile.avatar"
								:zoom-rate="1.2"
								:preview-src-list="new Array(profile.avatar)"
								:initial-index="4"
								fit="cover"
							/>
						</span>
					</ElCol>
				</ElRow>
			</el-tab-pane>
			<el-tab-pane label="更新密码">
				<el-form-item label="旧密码">
					<el-input
						v-model="passwordReq.oldPassword"
						type="password"
						show-password
					/>
				</el-form-item>
				<el-form-item label="新密码">
					<el-input
						v-model="passwordReq.newPassword"
						type="password"
						show-password
					/>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="updatePassword">保存</el-button>
				</el-form-item>
			</el-tab-pane>
		</el-tabs>
	</el-form>
</template>

<style lang="scss" scoped></style>
