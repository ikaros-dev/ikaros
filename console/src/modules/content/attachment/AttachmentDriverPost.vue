<script setup lang="ts">
import { AttachmentDriver, AttachmentDriverFetcherVo, AttachmentDriverTypeEnum } from '@runikaros/api-client';
import { onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';

import {ElRow, ElCol, ElForm, ElFormItem, ElInput, ElButton, ElMessage, ElSelect, ElOption, FormRules, FormInstance,} from 'element-plus';
import { apiClient } from '@/utils/api-client';
import { useRouter } from 'vue-router';

// eslint-disable-next-line no-unused-vars
const { t } = useI18n();
const router = useRouter();

const driver = ref<AttachmentDriver>({
	type: AttachmentDriverTypeEnum.Local,
	name: '',
	mount_name: '',
	remote_path: '',
	comment: '',
})

const driverBtnLoading = ref(false)
const driverElFormRef = ref<FormInstance>()
const driverFormRules = reactive<FormRules>({
	type: [
		{
			required: true,
			message: t('module.attachment.driver.post.message.form-rule.type.required'),
			trigger: 'blur',
		}
	],
	name: [
		{
			required: true,
			message: t('module.attachment.driver.post.message.form-rule.name.required'),
			trigger: 'blur',
		}
	],
	mount_name: [
		{
			required: true,
			message: t('module.attachment.driver.post.message.form-rule.mount_name.required'),
			trigger: 'blur',
		}
	],
	remote_path: [
		{
			required: true,
			message: t('module.attachment.driver.post.message.form-rule.remote_path.required'),
			trigger: 'blur',
		}
	],
})
const submitForm = async () => {
	console.debug('submitForm', 'driverElFormRef', driverElFormRef.value)
	if (!driverElFormRef.value) return;
	await driverElFormRef.value.validate(async (valid, fields) => {
		if (valid) {
			console.debug('submitForm', 'driverElFormRef', driverElFormRef.value)
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
		} else {
			console.log('error submit!', fields);
			ElMessage.error(t('module.subject.post.message.form-rule.validate-fail'));
		}
	})
	
}

const attDriverFetchers = ref<AttachmentDriverFetcherVo[]>([])
const fetchAttachmentDriverFetchers = async() => {
	const { data } = await apiClient.attachmentDriver.listDriversFetchers()
	attDriverFetchers.value = data
}

const typeNames = ref<String[]>([])

const updateTypeNames = async() => {
	typeNames.value = attDriverFetchers.value.filter(dr => driver.value.type === dr.type).map(dr => dr.name as string).filter(name => name) as String[]
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
onMounted(fetchAttachmentDriverFetchers)
</script>

<template>
	<el-row>
		<el-col :span="24">
			<el-form
				ref="driverElFormRef"
				:rules="driverFormRules"
				:model="driver"
				label-width="85px"
			>
				<el-form-item :label="t('module.attachment.driver.table.colum.label.type')" prop="type">
					<el-select v-model="driver.type" @change="onTypeSelectChange">
						<el-option key="LOCAL" label="LOCAL" value="LOCAL" />
						<el-option key="CUSTOM" label="CUSTOM" value="CUSTOM" />
						<el-option key="WEBDAV" label="WEBDAV" value="WEBDAV" disabled/>
					</el-select>
				</el-form-item>
				<el-form-item :label="t('module.attachment.driver.table.colum.label.name')" prop="name">
					<!-- <el-input v-model="driver.name" disabled /> -->
					<el-select v-model="driver.name" @visible-change="onVisibleChage" >
						<el-option v-for="(index, typeName) in typeNames" :key="typeName" :label="typeName" :value="typeName" />
					</el-select>
				</el-form-item>
				<el-form-item :label="t('module.attachment.driver.table.colum.label.mount_name')" prop="mount_name">
					<el-input v-model="driver.mount_name" placeholder="LocalPlayTest | NetPanMountAttName"></el-input>
				</el-form-item>
				<el-form-item :label="t('module.attachment.driver.table.colum.label.mount_remote_path')" prop="remote_path">
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
						@click="submitForm()"
					>
						{{ t('module.subject.post.text.button.subject.create') }}
					</el-button>
				</el-form-item>
			</el-form>

		</el-col>
	</el-row>
</template>

<style lang="scss" scoped></style>
