<script setup lang="ts">
import {apiClient} from '@/utils/api-client';
import {Role} from '@runikaros/api-client';
import {onMounted, ref} from 'vue';
import {WarningFilled} from '@element-plus/icons-vue';

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
import {useRouter} from 'vue-router';

const roles = ref<Role[]>([]);
const router = useRouter();

const fetchRoles = async () => {
	const { data } = await apiClient.role.getRoles();
	roles.value = data;
};

const editRow = ref(false);
const editRowId = ref(-1);
const editBtn = ref({
	type: 'primary' as 'primary' | 'success',
	text: 'Edit',
	loading: false,
});

const changeEditBtnStatus = async (id) => {
	if (editRowId.value === -1) {
		editRowId.value = id;
		editRow.value = true;
		editBtn.value.type = 'success' as 'success';
		editBtn.value.text = 'Submit';
	} else {
		var role = roles.value.find((obj) => obj.id === editRowId.value);
		editBtn.value.loading = true;
		await apiClient.role
			.updateRole({
				role: role as Role,
			})
			.then(() => {
				ElMessage.success('Update Role success for id=' + editRowId.value);
				editRowId.value = -1;
				editBtn.value.type = 'primary';
				editBtn.value.text = 'Edit';
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
	ElMessage.success('Add new role success for name=' + newRole.value.name);
	roleAddDialogVisible.value = false;
	newRole.value = {};
	await fetchRoles();
};

const deleteRole = async (id) => {
	await apiClient.role.deleteRoleById({ id });
	ElMessage.success('Delete role success for id=' + id);
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
		<el-dialog v-model="roleAddDialogVisible" title="Role Add" width="500">
			Name
			<br />
			<el-input v-model="newRole.name" />
			<br />
			<br />
			Description
			<br />
			<el-input v-model="newRole.description" type="textarea" :rows="2" />
			<template #footer>
				<div>
					<el-button @click="roleAddDialogVisible = false">Cancel</el-button>
					<el-button type="primary" @click="subitRoleAdd"> Confirm</el-button>
				</div>
			</template>
		</el-dialog>

		<el-row>
			<el-col :span="24">
				<el-button @click="roleAddDialogVisible = true">Add</el-button>
			</el-col>
		</el-row>

		<el-row>
			<el-col :span="24">
				<el-table :data="roles" size="large">
					<el-table-column prop="id" label="ID" width="80" />
					<el-table-column prop="name" label="Name" width="120">
						<template #default="scope">
							<span v-if="editRow && editRowId === scope.row.id">
								<el-input v-model="scope.row.name" />
							</span>
							<span v-else>
								{{ scope.row.name }}
							</span>
						</template>
					</el-table-column>
					<el-table-column prop="description" label="Description">
						<template #default="scope">
							<span v-if="editRow && editRowId === scope.row.id">
								<el-input v-model="scope.row.description" />
							</span>
							<span v-else>
								{{ scope.row.description }}
							</span>
						</template>
					</el-table-column>
					<el-table-column fixed="right" label="Operations" min-width="120">
						<template #default="scope">
							<el-button
								:loading="editBtn.loading"
								:type="scope.row.id === editRowId ? editBtn.type : 'primary'"
								@click="changeEditBtnStatus(scope.row.id)"
							>
								{{ scope.row.id === editRowId ? editBtn.text : 'Edit' }}
							</el-button>
							<el-popconfirm
								width="300"
								confirm-button-text="Delete"
								cancel-button-text="Cancel"
								confirm-button-type="danger"
								:icon="WarningFilled"
								icon-color="red"
								title="Are you sure to delete this role?"
								@confirm="deleteRole(scope.row.id)"
							>
								<template #reference>
									<el-button type="danger" :disabled="scope.row.id === 1"
										>Delete
									</el-button>
								</template>
							</el-popconfirm>
							<el-button
								type="primary"
								:disabled="scope.row.id === 1"
								@click="toRoleAuthiritiesPage(scope.row.id)"
							>
								Authorities
							</el-button>
						</template>
					</el-table-column>
				</el-table>
			</el-col>
		</el-row>
	</div>
</template>
<style lang="scss" scoped></style>
