<script setup lang="ts">
import { useUserStore } from '@/stores/user';
import { apiClient } from '@/utils/api-client';
import { ElMessage } from 'element-plus';

import type { UpdateUserRequest } from '@runikaros/api-client';

const userStore = useUserStore();

const profile = ref<UpdateUserRequest>({
	username: '',
	nickname: '',
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
	profile.value.introduce = userEntity?.introduce as string;
	profile.value.site = userEntity?.site as string;
	passwordReq.value.username = userEntity?.username as string;
};

onMounted(initProfileAndUsername);
</script>

<template>
	<el-form
		:model="profile"
		label-width="90"
		label-position="top"
		size="default"
	>
		<el-tabs>
			<el-tab-pane label="基本信息">
				<el-form-item label="昵称">
					<el-input
						v-model="profile.nickname"
						style="max-width: 600px"
						clearable
					/>
				</el-form-item>

				<el-form-item label="个人主页">
					<el-input v-model="profile.site" style="max-width: 600px" clearable />
				</el-form-item>

				<el-form-item label="个人介绍">
					<el-input
						v-model="profile.introduce"
						style="max-width: 600px"
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
			</el-tab-pane>
			<el-tab-pane label="更新密码">
				<el-form-item label="旧密码">
					<el-input
						v-model="passwordReq.oldPassword"
						style="max-width: 600px"
						clearable
						type="password"
					/>
				</el-form-item>
				<el-form-item label="新密码">
					<el-input
						v-model="passwordReq.newPassword"
						style="max-width: 600px"
						clearable
						type="password"
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
