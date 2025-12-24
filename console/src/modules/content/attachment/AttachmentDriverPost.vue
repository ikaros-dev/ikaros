<script setup lang="ts">
import { AttachmentDriver } from '@runikaros/api-client';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';

import {ElRow, ElCol, ElForm, ElFormItem, ElInput, ElButton, ElMessage, ElSelect, ElOption,} from 'element-plus';
import { apiClient } from '@/utils/api-client';
import { useRouter } from 'vue-router';

// eslint-disable-next-line no-unused-vars
const { t } = useI18n();
const router = useRouter();

const driver = ref<AttachmentDriver>({
	type: 'LOCAL',
	name: 'DISK',
	mount_name: '',
	remote_path: '',
	comment: '',
})

const driverBtnLoading = ref(false)
const driverElFormRef = ref()
const submitForm = async (driverElForm) => {
	console.debug('submitForm', 'driverElForm', driverElForm)
	driverBtnLoading.value = true
	apiClient.attachmentDriver.saveAttachmentDriver({
		attachmentDriver: driver.value
	})
	.then(()=>{
		driverBtnLoading.value = false
		router.push('/attachment/drivers')
	})
	.catch((err)=>{
		console.error('AttachmentDriverPost', 'submitForm', 'err', err)
		ElMessage.error('New Fail.')
	})
	.finally(()=>{
		driverBtnLoading.value = false
	})
}

const attDriverFetchers = ref([])
const fetchAttachmentDriverFetchers = async() => {
	const { data } = await apiClient.attachmentDriver.listDriversFetchers()
}

const attachmentDrivers = ref<AttachmentDriver[]>()
const fetchAttachmentDrivers = async() => {
	const { data } = await apiClient.attachmentDriver.listDriversByCondition({page: 1, size: 999})
	attachmentDrivers.value = data.items as AttachmentDriver[]
}


const typeNames = ref<String[]>([])

const updateTypeNames = async() => {
	typeNames.value = (attachmentDrivers.value ?? []).filter(dr => driver.value.type === dr.type).map(dr => dr.name as string).filter(name => name) as String[]
	console.debug('updateTypeNames', 'typeNames', typeNames.value)
}

const onVisibleChage = (visible)=>{
	if (!visible) return
	updateTypeNames()
}
const onTypeSelectChange = ()=>{
	driver.value.name = ''
	updateTypeNames()
}
onMounted(fetchAttachmentDrivers)
</script>

<template>
	<el-row>
		<el-col :span="24">
			<el-form
				:ref="driverElFormRef"
				:model="driver"
				label-width="85px"
			>
				<el-form-item :label="t('module.attachment.driver.table.colum.label.type')">
					<el-select v-model="driver.type" @change="onTypeSelectChange">
						<el-option key="LOCAL" label="LOCAL" value="LOCAL" />
						<el-option key="CUSTOM" label="CUSTOM" value="CUSTOM" />
						<el-option key="WEBDAV" label="WEBDAV" value="WEBDAV" disabled/>
					</el-select>
				</el-form-item>
				<el-form-item :label="t('module.attachment.driver.table.colum.label.name')">
					<!-- <el-input v-model="driver.name" disabled /> -->
					<el-select v-model="driver.name" @visible-change="onVisibleChage" >
						<el-option v-for="typeName in typeNames" :key="typeName" :label="typeName" :value="typeName" />
					</el-select>
				</el-form-item>
				<el-form-item :label="t('module.attachment.driver.table.colum.label.mount_name')">
					<el-input v-model="driver.mount_name" placeholder="LocalPlayTest | NetPanMountAttName"></el-input>
				</el-form-item>
				<el-form-item :label="t('module.attachment.driver.table.colum.label.mount_remote_path')">
					<el-input v-model="driver.remote_path" clearable placeholder="/mnt/playtest | 2989952445870372007"></el-input>
				</el-form-item>
				<el-form-item :label="t('module.attachment.driver.table.colum.label.access_token')">
					<el-input v-model="driver.access_token" clearable placeholder="n28uf.47fe03dd47f06d07fec0e88860cc2e9e.8b2b8261cef0a0b7e38e3c96277d62e3aba29908695df66f4383fce100731ce3"></el-input>
				</el-form-item>
				<el-form-item :label="t('module.attachment.driver.table.colum.label.refresh_token')">
					<el-input v-model="driver.refresh_token" clearable placeholder="n28uf.c9a4974d851f9aa42b4edd523847dadf06d84914581d05a093e532c1eeec0ced.aec3da5a8f6c72c3623f430f23b885d906eef639b18fd1be42f9810ac1880ce2"></el-input>
				</el-form-item>
				<el-form-item :label="t('module.attachment.driver.table.colum.label.comment')">
					<el-input v-model="driver.comment" clearable placeholder="Play Test Driver"></el-input>
				</el-form-item>
				<el-form-item>
					<el-button
						plain
						:loading="driverBtnLoading"
						@click="submitForm(driverElFormRef)"
					>
						{{ t('module.subject.post.text.button.subject.create') }}
					</el-button>
				</el-form-item>
			</el-form>

		</el-col>
	</el-row>
</template>

<style lang="scss" scoped></style>
