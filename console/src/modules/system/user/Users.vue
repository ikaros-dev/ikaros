<script setup lang="ts">
import { useUserStore } from '@/stores/user';
import { apiClient } from '@/utils/api-client';
import { WarningFilled } from '@element-plus/icons-vue';
import { Role, User, UserEntity } from '@runikaros/api-client';
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import {
	ElButton,
	ElCol,
	ElDialog,
	ElForm,
	ElFormItem,
	ElInput,
	ElMessage,
	ElOption,
	ElPopconfirm,
	ElRow,
	ElSelect,
	ElSwitch,
	ElTable,
	ElTableColumn,
	FormInstance,
	FormRules,
} from 'element-plus';

const router = useRouter();
const { t } = useI18n();
const userStore = useUserStore();
const users = ref<User[]>([]);
const fetchUsers = async () => {
	const { data } = await apiClient.user.getUsers();
	users.value = data;
};

const user = ref<UserEntity>({});
const userDetailsDialogVisible = ref(false);
const subitUserFrom = async (formEl: FormInstance | undefined) => {
	console.debug('formEl', formEl);
	if (!formEl) return;
	await formEl.validate(async (valid, fields) => {
		if (valid) {
			if (user.value.id) {
				await apiClient.user.updateUser({
					updateUserRequest: {
						username: user.value.username as string,
						nickname: user.value.nickname as string,
						introduce: user.value.introduce as string,
					},
				});
				ElMessage.success(
					t('module.users.message.update_success', {
						username: user.value.username,
					})
				);
			} else {
				await apiClient.user.postUser({
					createUserReqParams: {
						enabled: true,
						username: user.value.username as string,
						password: (user.value as any).password as string,
					},
				});
				ElMessage.success(
					t('module.users.message.create_success', {
						username: user.value.username,
					})
				);
			}
			user.value = {};
			userDetailsDialogVisible.value = false;
			await fetchUsers();
		} else {
			console.log('error submit!', fields);
			ElMessage.error(t('common.message.validation_failed'));
		}
	});
};
const userElFormRef = ref<FormInstance>();
const userFormRules = reactive<FormRules>({
	username: [
		{
			required: true,
			message: t('module.users.validation.username_required'),
			trigger: 'blur',
		},
		{
			min: 1,
			max: 100,
			message: t('module.users.validation.username_length'),
			trigger: 'blur',
		},
	],
});

const doEditUser = (row: User) => {
	user.value = row.entity as UserEntity;
	userDetailsDialogVisible.value = true;
};

const changeUserEnableStatus = async (userEntity: UserEntity) => {
	console.debug('userEntity', userEntity);
	await apiClient.user.updateUser({
		updateUserRequest: {
			username: userEntity.username as string,
			enable: userEntity.enable as boolean,
		},
	});
	ElMessage.success(
		t('module.users.message.enable_status_update_success', {
			username: userEntity.username,
		})
	);
	await fetchUsers();
};

const doDeleteUser = async (userId) => {
	console.debug('userId', userId);
	if (!userId) return;
	await apiClient.user.deleteById1({ id: userId });
	ElMessage.success(t('module.users.message.delete_success', { id: userId }));
	if (String(userStore.currentUser?.entity?.id) === String(userId)) {
		userStore.jwtTokenLogout();
		await router.replace({ name: 'Login' });
		return;
	}
	await fetchUsers();
};

const isCurrentUser = (userEntity: UserEntity) =>
	userEntity.id === userStore.currentUser?.entity?.id;

const userRoleDialogVisible = ref(false);
const userRoleId = ref(); // user role id
const roles = ref<Role[]>([]);
const fetchRoles = async () => {
	const { data } = await apiClient.role.getRoles();
	roles.value = data;
};

const rowUserEntity = ref<UserEntity>({});
const openRoleDialog = async (userE: UserEntity) => {
	rowUserEntity.value = userE;
	if (roles.value.length === 0) {
		await fetchRoles();
	}
	const { data } = await apiClient.userRole.getRolesForUser({
		userId: rowUserEntity.value.id + '',
	});
	if (data.length > 0) {
		userRoleId.value = data[0].id as string;
	} else {
		const masterRoleId = roles.value.find((role) => role.name === 'MASTER')?.id;
		if (masterRoleId) {
			userRoleId.value = masterRoleId;
			await apiClient.userRole.addUserRoles({
				userRoleReqParams: {
					userId: rowUserEntity.value.id,
					roleIds: [masterRoleId],
				},
			});
		}
	}
	userRoleDialogVisible.value = true;
};

const submitUserRole = async () => {
	console.debug('userRoleId', userRoleId.value);
	const { data } = await apiClient.userRole.getRolesForUser({
		userId: rowUserEntity.value.id + '',
	});
	const roleIds = data.map((role) => role.id) as string[];
	if (
		userRoleId.value &&
		!roleIds.some((roleId) => String(roleId) === String(userRoleId.value))
	) {
		await apiClient.userRole.addUserRoles({
			userRoleReqParams: {
				userId: rowUserEntity.value.id,
				roleIds: [userRoleId.value],
			},
		});
	}
	const roleIdsToDelete = userRoleId.value
		? roleIds.filter((roleId) => String(roleId) !== String(userRoleId.value))
		: roleIds;
	if (roleIdsToDelete.length > 0) {
		await apiClient.userRole.deleteUserRoles({
			userRoleReqParams: {
				userId: rowUserEntity.value.id,
				roleIds: roleIdsToDelete,
			},
		});
	}
	if (userRoleId.value) {
		ElMessage.success(
			t('module.users.message.role_update_success', {
				username: rowUserEntity.value.username,
			})
		);
	} else {
		ElMessage.success(
			t('module.users.message.role_delete_success', {
				username: rowUserEntity.value.username,
			})
		);
	}
	userRoleDialogVisible.value = false;
};

onMounted(() => {
	fetchUsers();
	fetchRoles();
});
</script>
<template>
	<div>
		<el-dialog
			v-model="userDetailsDialogVisible"
			width="500"
			:title="
				user.id
					? t('module.users.dialog.edit')
					: t('module.users.dialog.create')
			"
			@closed="user = {}"
		>
			<el-form
				ref="userElFormRef"
				:model="user"
				:rules="userFormRules"
				label-width="auto"
			>
				<el-form-item v-if="user.id" :label="t('common.label.id')">
					<el-input v-model="user.id" disabled />
				</el-form-item>
				<el-form-item :label="t('common.label.username')">
					<el-input v-model="user.username" />
				</el-form-item>
				<el-form-item v-if="!user.id" :label="t('common.label.password')">
					<el-input v-model="(user as any).password" show-password />
				</el-form-item>
				<el-form-item v-if="user.id" :label="t('common.label.nickname')">
					<el-input v-model="user.nickname" />
				</el-form-item>
				<el-form-item v-if="user.id" :label="t('common.label.introduce')">
					<el-input v-model="user.introduce" type="textarea" :rows="2" />
				</el-form-item>
			</el-form>
			<template #footer>
				<div>
					<el-button @click="userDetailsDialogVisible = false">{{
						t('common.button.cancel')
					}}</el-button>
					<el-button type="primary" @click="subitUserFrom(userElFormRef)">
						{{ t('common.button.submit') }}
					</el-button>
				</div>
			</template>
		</el-dialog>

		<el-dialog
			v-model="userRoleDialogVisible"
			:title="t('module.users.dialog.role')"
			width="500"
			@closed="
				() => {
					userRoleId = undefined;
					rowUserEntity = {};
				}
			"
		>
			<el-form :model="rowUserEntity" label-width="auto">
				<el-form-item :label="t('common.label.id')">
					<el-input v-model="rowUserEntity.id" disabled />
				</el-form-item>
				<el-form-item :label="t('common.label.username')">
					<el-input v-model="rowUserEntity.username" disabled />
				</el-form-item>
				<el-form-item :label="t('common.label.role')">
					<el-select
						v-model="userRoleId"
						:placeholder="t('module.users.dialog.role')"
						size="large"
					>
						<el-option label="无" value="" />
						<el-option
							v-for="role in roles"
							:key="role.id"
							:label="role.name"
							:value="role.id as string"
						/>
					</el-select>
				</el-form-item>
			</el-form>
			<template #footer>
				<div>
					<el-button @click="userRoleDialogVisible = false">{{
						t('common.button.cancel')
					}}</el-button>
					<el-button type="primary" @click="submitUserRole">{{
						t('common.button.submit')
					}}</el-button>
				</div>
			</template>
		</el-dialog>

		<el-row>
			<el-col :span="24">
				<el-button @click="userDetailsDialogVisible = true">{{
					t('common.button.add')
				}}</el-button>
			</el-col>
		</el-row>

		<el-row>
			<el-col :span="24">
				<el-table :data="users" size="large">
					<el-table-column
						prop="entity.id"
						:label="t('common.label.id')"
						width="160"
					/>
					<el-table-column
						prop="entity.username"
						:label="t('common.label.username')"
						width="120"
					/>
					<el-table-column
						prop="entity.nickname"
						:label="t('common.label.nickname')"
					/>
					<el-table-column
						prop="entity.introduce"
						:label="t('common.label.introduce')"
					/>
					<el-table-column
						prop="entity.enable"
						:label="t('common.label.enabled')"
					>
						<template #default="scope">
							<el-switch
								v-model="scope.row.entity.enable"
								:disabled="isCurrentUser(scope.row.entity)"
								@change="changeUserEnableStatus(scope.row.entity)"
							/>
						</template>
					</el-table-column>

					<el-table-column
						fixed="right"
						:label="t('common.label.operations')"
						min-width="120"
					>
						<template #default="scope">
							<el-button type="primary" @click="doEditUser(scope.row)">
								{{ t('common.button.edit') }}
							</el-button>
							<el-popconfirm
								width="300"
								:confirm-button-text="t('common.button.delete')"
								:cancel-button-text="t('common.button.cancel')"
								confirm-button-type="danger"
								:icon="WarningFilled"
								icon-color="red"
								:title="t('module.users.message.delete_confirm')"
								@confirm="doDeleteUser(scope.row.entity.id)"
							>
								<template #reference>
									<el-button
										type="danger"
										:disabled="isCurrentUser(scope.row.entity)"
									>
										{{ t('common.button.delete') }}
									</el-button>
								</template>
							</el-popconfirm>
							<el-button
								type="primary"
								:disabled="isCurrentUser(scope.row.entity)"
								@click="openRoleDialog(scope.row.entity)"
							>
								{{ t('common.label.role') }}
							</el-button>
						</template>
					</el-table-column>
				</el-table>
			</el-col>
		</el-row>
	</div>
</template>
<style lang="scss" scoped></style>
