<script setup lang="ts">
import { useRoute } from 'vue-router';
import { onMounted, ref, watch } from 'vue';
import { Authority } from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';
import { WarningFilled } from '@element-plus/icons-vue';
import {
	ElButton,
	ElDialog,
	ElForm,
	ElFormItem,
	ElInput,
	ElMessage,
	ElOption,
	ElPagination,
	ElPopconfirm,
	ElSelect,
	ElTable,
	ElTableColumn,
} from 'element-plus';

const route = useRoute();

const roleId = ref<number>();

watch(route, async () => {
	fetchRoleIdFromRoute();
});

const fetchRoleIdFromRoute = () => {
	if (!route.params?.roleId && route.params?.roleId === undefined) {
		return;
	}
	// console.log(route.params.id);
	roleId.value = route.params?.roleId as any as number;
};

const roleAutiorities = ref<Authority[]>([]);

const fetchRoleAutiorities = async () => {
	if (!roleId.value) return;
	const { data } = await apiClient.roleAuthority.getAuthoritiesForRole({
		roleId: roleId.value + '',
	});
	roleAutiorities.value = data;
};

const authorityTypes = ref<string[]>([]);
const fetchAuthorityTypes = async () => {
	const { data } = await apiClient.authority.getAuthorityTypes();
	authorityTypes.value = data;
};

const authorityDialogVisible = ref(false);
const autiorities = ref<Authority[]>([]);
const authorityCondition = ref({
	allow: true,
	type: 'ALL',
	target: undefined,
	authority: undefined,
	page: 1,
	size: 10,
	total: 0,
});
const fetchAuthorities = async () => {
	const { data } = await apiClient.authority.getAuthoritiesByCondition({
		allow: authorityCondition.value.allow as boolean,
		type: authorityCondition.value.type as
			| 'ALL'
			| 'API'
			| 'APIS'
			| 'MENU'
			| 'URL'
			| 'OTHERS',
		target: authorityCondition.value.target,
		authority: authorityCondition.value.authority,
		page: authorityCondition.value.page,
		size: authorityCondition.value.size,
	});
	autiorities.value = data.items;
	authorityCondition.value.total = data.total;
	authorityCondition.value.page = data.page;
	authorityCondition.value.size = data.size;
};

const onCurrentPageChange = async (val: number) => {
	authorityCondition.value.page = val;
	await fetchAuthorities();
};

const onSizeChange = async (val: number) => {
	authorityCondition.value.size = val;
	await fetchAuthorities();
};

const authorityIds = ref<number[]>([]);
const onAuthTabSelectChange = (newSelection) => {
	console.debug('newSelection', newSelection);
	authorityIds.value = newSelection.map((auth) => auth.id as number);
};

const submitRoleAuthoritiesAdd = async () => {
	await apiClient.roleAuthority.addAuthoritiesForRole({
		roleAuthorityReqParams: {
			roleId: roleId.value,
			authorityIds: authorityIds.value,
		},
	});
	ElMessage.success('Add role authorities success for roleId=' + roleId.value);
	authorityDialogVisible.value = false;
	await fetchRoleAutiorities();
};

const deleteRoleAuthority = async (authId) => {
	if (!authId) return;
	await apiClient.roleAuthority.deleteAuthoritiesForRole({
		roleAuthorityReqParams: {
			roleId: roleId.value,
			authorityIds: [authId],
		},
	});
	ElMessage.success('Delete role authority success.');
	await fetchRoleAutiorities();
};

const fetchAttachmentsWithPageOne = () => {
	authorityCondition.value.page = 1;
	fetchAuthorities();
};

onMounted(() => {
	fetchRoleIdFromRoute();
	fetchRoleAutiorities();
	fetchAuthorityTypes();
});
</script>
<template>
	<div>
		<el-dialog
			v-model="authorityDialogVisible"
			title="Role Authorities Multi Select Dialog"
			width="70%"
			@open="fetchAuthorities"
		>
			<el-form :inline="true" :model="authorityCondition">
				<el-form-item label="Type">
					<el-select
						v-model="authorityCondition.type"
						placeholder="Select Authiroty Type"
						@change="fetchAttachmentsWithPageOne"
					>
						<el-option
							v-for="auth in authorityTypes"
							:key="auth"
							:label="auth"
							:value="auth"
						/>
					</el-select>
				</el-form-item>
				<el-form-item label="Target">
					<el-input
						v-model="authorityCondition.target"
						clearable
						@change="fetchAttachmentsWithPageOne"
					/>
				</el-form-item>
				<el-form-item label="Authority">
					<el-input
						v-model="authorityCondition.authority"
						clearable
						@change="fetchAttachmentsWithPageOne"
					/>
				</el-form-item>
			</el-form>

			<el-pagination
				v-model:page-size="authorityCondition.size"
				v-model:current-page="authorityCondition.page"
				background
				:total="authorityCondition.total"
				:pager-count="5"
				layout="total, sizes, prev, pager, next, jumper"
				@current-change="onCurrentPageChange"
				@size-change="onSizeChange"
			/>

			<el-table
				:data="autiorities"
				style="width: 100%"
				@selection-change="onAuthTabSelectChange"
			>
				<el-table-column type="selection" width="55" />
				<el-table-column prop="id" label="ID" width="60" />
				<el-table-column prop="type" label="Type" width="180" />
				<el-table-column prop="target" label="Target" />
				<el-table-column prop="authority" label="Authority" />
			</el-table>
			<template #footer>
				<div class="dialog-footer">
					<el-button @click="authorityDialogVisible = false">Cancel</el-button>
					<el-button type="primary" @click="submitRoleAuthoritiesAdd">
						Submit
					</el-button>
				</div>
			</template>
		</el-dialog>

		<el-button @click="authorityDialogVisible = true">Add</el-button>

		<el-table :data="roleAutiorities" style="width: 100%">
			<el-table-column prop="id" label="ID" width="60" />
			<el-table-column prop="type" label="Type" width="180" />
			<el-table-column prop="target" label="Target" />
			<el-table-column prop="authority" label="Authority" />
			<el-table-column fixed="right" label="Operations" min-width="120">
				<template #default="scope">
					<el-popconfirm
						width="300"
						confirm-button-text="Delete"
						cancel-button-text="Cancel"
						confirm-button-type="danger"
						:icon="WarningFilled"
						icon-color="red"
						title="Are you sure to delete this authority?"
						@confirm="deleteRoleAuthority(scope.row.id)"
					>
						<template #reference>
							<el-button type="danger" :disabled="scope.row.id === 1"
								>Delete
							</el-button>
						</template>
					</el-popconfirm>
				</template>
			</el-table-column>
		</el-table>
	</div>
</template>
<style lang="scss" scoped></style>
