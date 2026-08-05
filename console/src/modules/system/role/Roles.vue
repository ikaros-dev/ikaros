<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { Role } from '@runikaros/api-client';
import { onMounted, ref } from 'vue';
import { WarningFilled } from '@element-plus/icons-vue';

import {
	ElButton,
	ElCol,
	ElDialog,
	ElInput,
	ElMessage,
	ElPopconfirm,
	ElRow,
	ElTable,
	ElTableColumn,
} from 'element-plus';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
const roles = ref<Role[]>([]);
const router = useRouter();

const fetchRoles = async () => {
	const { data } = await apiClient.role.getRoles();
	roles.value = data;
};

const editRow = ref(false);
const editRowId = ref<string>();
const editBtn = ref({
	type: 'primary' as 'primary' | 'success',
	text: t('common.button.edit'),
	loading: false,
});

const changeEditBtnStatus = async (id: string | undefined) => {
	if (!id) return;
	if (!editRowId.value) {
		editRowId.value = id;
		editRow.value = true;
		editBtn.value.type = 'success' as const;
		editBtn.value.text = t('common.button.submit');
	} else {
		const role = roles.value.find((obj) => obj.id === editRowId.value);
		editBtn.value.loading = true;
		await apiClient.role
			.updateRole({
				role: role as Role,
			})
			.then(() => {
				ElMessage.success(
					t('module.roles.message.update_success', { id: editRowId.value })
				);
				editRowId.value = undefined;
				editBtn.value.type = 'primary';
				editBtn.value.text = t('common.button.edit');
			})
			.finally(() => {
				editBtn.value.loading = false;
			});
	}
};

const roleAddDialogVisible = ref(false);
const newRole = ref<Role>({});
const subitRoleAdd = async () => {
	if (!newRole.value.name) return;
	await apiClient.role.createRole({
		role: newRole.value,
	});
	ElMessage.success(
		t('module.roles.message.create_success', { name: newRole.value.name })
	);
	roleAddDialogVisible.value = false;
	newRole.value = {};
	await fetchRoles();
};

const deleteRole = async (id) => {
	await apiClient.role.deleteRoleById({ id });
	ElMessage.success(t('module.roles.message.delete_success', { id }));
	await fetchRoles();
};

const authorityTypes = ref<string[]>([]);
const fetchAuthorityTypes = async () => {
	const { data } = await apiClient.authority.getAuthorityTypes();
	authorityTypes.value = data;
};

const toRoleAuthiritiesPage = (roleId) => {
	router.push('/role/authorities/roleId/' + roleId);
};

onMounted(() => {
	fetchRoles();
	fetchAuthorityTypes();
});
</script>
<template>
	<div>
		<!-- 角色添加弹框 -->
		<el-dialog
			v-model="roleAddDialogVisible"
			:title="t('module.roles.dialog.create')"
			width="500"
		>
			{{ t('common.label.name') }}
			<br />
			<el-input v-model="newRole.name" />
			<br />
			<br />
			{{ t('common.label.description') }}
			<br />
			<el-input v-model="newRole.description" type="textarea" :rows="2" />
			<template #footer>
				<div>
					<el-button @click="roleAddDialogVisible = false">{{
						t('common.button.cancel')
					}}</el-button>
					<el-button type="primary" @click="subitRoleAdd">{{
						t('common.button.confirm')
					}}</el-button>
				</div>
			</template>
		</el-dialog>

		<el-row>
			<el-col :span="24">
				<el-button @click="roleAddDialogVisible = true">{{
					t('common.button.add')
				}}</el-button>
			</el-col>
		</el-row>

		<el-row>
			<el-col :span="24">
				<el-table :data="roles" size="large">
					<el-table-column
						prop="id"
						:label="t('common.label.id')"
						width="160"
					/>
					<el-table-column
						prop="name"
						:label="t('common.label.name')"
						width="120"
					>
						<template #default="scope">
							<span v-if="editRow && editRowId === scope.row.id">
								<el-input v-model="scope.row.name" />
							</span>
							<span v-else>
								{{ scope.row.name }}
							</span>
						</template>
					</el-table-column>
					<el-table-column
						prop="description"
						:label="t('common.label.description')"
					>
						<template #default="scope">
							<span v-if="editRow && editRowId === scope.row.id">
								<el-input v-model="scope.row.description" />
							</span>
							<span v-else>
								{{ scope.row.description }}
							</span>
						</template>
					</el-table-column>
					<el-table-column
						fixed="right"
						:label="t('common.label.operations')"
						min-width="120"
					>
						<template #default="scope">
							<el-button
								:loading="editBtn.loading"
								:type="scope.row.id === editRowId ? editBtn.type : 'primary'"
								@click="changeEditBtnStatus(scope.row.id)"
							>
								{{
									scope.row.id === editRowId
										? editBtn.text
										: t('common.button.edit')
								}}
							</el-button>
							<el-popconfirm
								width="300"
								:confirm-button-text="t('common.button.delete')"
								:cancel-button-text="t('common.button.cancel')"
								confirm-button-type="danger"
								:icon="WarningFilled"
								icon-color="red"
								:title="t('module.roles.message.delete_confirm')"
								@confirm="deleteRole(scope.row.id)"
							>
								<template #reference>
									<el-button
										type="danger"
										:disabled="scope.row.name === 'MASTER'"
										>{{ t('common.button.delete') }}
									</el-button>
								</template>
							</el-popconfirm>
							<el-button
								type="primary"
								:disabled="scope.row.name === 'MASTER'"
								@click="toRoleAuthiritiesPage(scope.row.id)"
							>
								{{ t('common.label.authorities') }}
							</el-button>
						</template>
					</el-table-column>
				</el-table>
			</el-col>
		</el-row>
	</div>
</template>
<style lang="scss" scoped></style>
