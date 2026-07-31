<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { formatDate } from '@/utils/date';
import { copyValue2Clipboard, objectToMap } from '@/utils/string-util';
import {
	ElButton,
	ElDescriptions,
	ElDescriptionsItem,
	ElImage,
	ElMessage,
} from 'element-plus';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
const actuatorInfo = ref();
const fetchActuatorInfo = async () => {
	const { data } = await apiClient.actuator.info();
	actuatorInfo.value = data;
};

const airTimeDateFormatter = (time) => {
	// console.log('row', row);
	return formatDate(new Date(time), 'yyyy-MM-dd');
};

const onBasicInfoCopyButtonClick = () => {
	const map = objectToMap(actuatorInfo.value);

	let result = '';

	map.forEach((value, key) => {
		result += `${key}:${value}\n`;
	});

	copyValue2Clipboard(result).then(() => {
		ElMessage.success(t('module.about.copy_success'));
	});
};

onMounted(fetchActuatorInfo);
</script>
<template>
	<!-- Basic -->
	<el-descriptions
		class="margin-top"
		:title="t('module.about.basic')"
		:column="3"
		size="large"
		border
	>
		<template #extra>
			<el-button @click="onBasicInfoCopyButtonClick">{{ t('common.button.copy') }}</el-button>
		</template>
		<!-- git -->
		<el-descriptions-item>
			<template #label>{{ t('module.about.git_branch') }}</template>
			{{ actuatorInfo?.git.branch }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label>{{ t('module.about.git_commit') }}</template>
			<a
				target="_blank"
				:href="
					'https://github.com/ikaros-dev/ikaros/tree/' +
					actuatorInfo?.git.commit.id
				"
				>{{ actuatorInfo?.git.commit.id }}</a
			>
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label>{{ t('module.about.commit_time') }}</template>

			{{ airTimeDateFormatter(actuatorInfo?.git.commit.time) }}
		</el-descriptions-item>

		<!-- build -->
		<el-descriptions-item>
			<template #label>{{ t('module.about.build_version') }}</template>
			{{ actuatorInfo?.build.version }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label>{{ t('module.about.build_name') }}</template>
			{{ actuatorInfo?.build.name }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label>{{ t('module.about.build_time') }}</template>
			{{ airTimeDateFormatter(actuatorInfo?.build.time) }}
		</el-descriptions-item>

		<!-- os -->
		<el-descriptions-item>
			<template #label>{{ t('module.about.os_name') }}</template>
			{{ actuatorInfo?.os.name }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label>{{ t('module.about.os_version') }}</template>
			{{ actuatorInfo?.os.version }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label>{{ t('module.about.os_arch') }}</template>
			{{ actuatorInfo?.os.arch }}
		</el-descriptions-item>
	</el-descriptions>

	<br />

	<!-- Jave -->
	<el-descriptions
		class="margin-top"
		:title="t('module.about.java')"
		:column="3"
		size="large"
		border
	>
		<!-- version -->
		<el-descriptions-item>
			<template #label>{{ t('module.about.java_version') }}</template>
			{{ actuatorInfo?.java.version }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label>{{ t('module.about.vendor_name') }}</template>
			{{ actuatorInfo?.java.vendor.name }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label>{{ t('module.about.vendor_version') }}</template>
			{{ actuatorInfo?.java.vendor.version }}
		</el-descriptions-item>

		<!-- runtime -->
		<el-descriptions-item>
			<template #label>{{ t('module.about.runtime_name') }}</template>
			{{ actuatorInfo?.java.runtime.name }}
		</el-descriptions-item>
		<el-descriptions-item :span="2">
			<template #label>{{ t('module.about.runtime_version') }}</template>
			{{ actuatorInfo?.java.runtime.version }}
		</el-descriptions-item>

		<!-- jvm -->
		<el-descriptions-item>
			<template #label>{{ t('module.about.jvm_name') }}</template>
			{{ actuatorInfo?.java.jvm.name }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label>{{ t('module.about.jvm_vendor') }}</template>
			{{ actuatorInfo?.java.jvm.vendor }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label>{{ t('module.about.jvm_version') }}</template>
			{{ actuatorInfo?.java.jvm.version }}
		</el-descriptions-item>
	</el-descriptions>

	<br />

	<!-- Introduce -->
	<el-descriptions
		class="margin-top"
		:title="t('module.about.project')"
		:column="1"
		size="large"
		direction="vertical"
		border
	>
		<el-descriptions-item>
			<template #label>{{ t('module.about.official') }}</template>
			<a href="https://ikaros.run" target="_blank">https://ikaros.run</a>
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label> GitHub</template>
			<a href="https://github.com/ikaros-dev/ikaros" target="_blank"
				>https://github.com/ikaros-dev/ikaros</a
			>
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label>{{ t('module.about.api_documentation') }}</template>
			<a href="/webjars/swagger-ui/index.html" target="_blank">
				/webjars/swagger-ui/index.html
			</a>
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label>{{ t('module.about.contributors') }}</template>
			<el-image
				style="width: 100%; height: 100%"
				src="https://contrib.nn.ci/api?repo=ikaros-dev/ikaros&repo=ikaros-dev/docs&repo=ikaros-dev/app"
				fit="fill"
			/>
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label>{{ t('module.about.status') }}</template>
			<el-image
				style="width: 100%; height: 100%"
				src="https://repobeats.axiom.co/api/embed/f7285853048ff09f313f6483901e2af0e638f666.svg"
				fit="fill"
			/>
		</el-descriptions-item>
	</el-descriptions>
</template>
<style lang="scss" scoped></style>
