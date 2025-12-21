<script setup lang="ts">
import { AttachmentDriver } from '@runikaros/api-client';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';

import {ElRow, ElCol, ElForm, ElFormItem, ElInput, ElButton, ElMessage,} from 'element-plus';
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
					<el-input v-model="driver.type" disabled />
				</el-form-item>
				<el-form-item :label="t('module.attachment.driver.table.colum.label.name')">
					<el-input v-model="driver.name" disabled />
				</el-form-item>
				<el-form-item :label="t('module.attachment.driver.table.colum.label.mount_name')">
					<el-input v-model="driver.mount_name" placeholder="LocalPlayTest"></el-input>
				</el-form-item>
				<el-form-item :label="t('module.attachment.driver.table.colum.label.mount_remote_path')">
					<el-input v-model="driver.remote_path" clearable placeholder="/mnt/playtest"></el-input>
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
