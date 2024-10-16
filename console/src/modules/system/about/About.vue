<script setup lang="ts">
import {apiClient} from '@/utils/api-client';
import {formatDate} from '@/utils/date';
import {copyValue2Clipboard, objectToMap} from '@/utils/string-util';
import {ElButton, ElDescriptions, ElDescriptionsItem, ElImage, ElMessage,} from 'element-plus';
import {onMounted, ref} from 'vue';

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
	var map = objectToMap(actuatorInfo.value);

	let result = '';

	map.forEach((value, key) => {
		result += `${key}:${value}\n`;
	});

	copyValue2Clipboard(result).then(() => {
		ElMessage.success('Copy To Clipboard Success.');
	});
};

onMounted(fetchActuatorInfo);
</script>
<template>
	<!-- Basic -->
	<el-descriptions
		class="margin-top"
		title="Basic"
		:column="3"
		size="large"
		border
	>
		<template #extra>
			<el-button @click="onBasicInfoCopyButtonClick">Copy</el-button>
		</template>
		<!-- git -->
		<el-descriptions-item>
			<template #label> Git Branch</template>
			{{ actuatorInfo?.git.branch }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label> Git Commit</template>
			{{ actuatorInfo?.git.commit.id }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label> Commit Time</template>

			{{ airTimeDateFormatter(actuatorInfo?.git.commit.time) }}
		</el-descriptions-item>

		<!-- build -->
		<el-descriptions-item>
			<template #label> Build Version</template>
			{{ actuatorInfo?.build.version }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label> Build Name</template>
			{{ actuatorInfo?.build.name }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label> Build Time</template>
			{{ airTimeDateFormatter(actuatorInfo?.build.time) }}
		</el-descriptions-item>

		<!-- os -->
		<el-descriptions-item>
			<template #label> OS Name</template>
			{{ actuatorInfo?.os.name }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label> OS Version</template>
			{{ actuatorInfo?.os.version }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label> OS Arch</template>
			{{ actuatorInfo?.os.arch }}
		</el-descriptions-item>
	</el-descriptions>

	<br />

	<!-- Jave -->
	<el-descriptions
		class="margin-top"
		title="Java"
		:column="3"
		size="large"
		border
	>
		<!-- version -->
		<el-descriptions-item>
			<template #label> Java Version</template>
			{{ actuatorInfo?.java.version }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label> Vendor Name</template>
			{{ actuatorInfo?.java.vendor.name }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label> Vendor Version</template>
			{{ actuatorInfo?.java.vendor.version }}
		</el-descriptions-item>

		<!-- runtime -->
		<el-descriptions-item>
			<template #label> Runtime Name</template>
			{{ actuatorInfo?.java.runtime.name }}
		</el-descriptions-item>
		<el-descriptions-item :span="2">
			<template #label> Runtime Version</template>
			{{ actuatorInfo?.java.runtime.version }}
		</el-descriptions-item>

		<!-- jvm -->
		<el-descriptions-item>
			<template #label> JVM Name</template>
			{{ actuatorInfo?.java.jvm.name }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label> JVM Vendor</template>
			{{ actuatorInfo?.java.jvm.vendor }}
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label> JVM Version</template>
			{{ actuatorInfo?.java.jvm.version }}
		</el-descriptions-item>
	</el-descriptions>

	<br />

	<!-- Introduce -->
	<el-descriptions
		class="margin-top"
		title="Ikaros Project"
		:column="1"
		size="large"
		direction="vertical"
		border
	>
		<el-descriptions-item>
			<template #label> Offical</template>
			<a href="https://ikaros.run" target="_blank">https://ikaros.run</a>
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label> GitHub</template>
			<a href="https://github.com/ikaros-dev/ikaros" target="_blank"
				>https://github.com/ikaros-dev/ikaros</a
			>
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label> Open API Documentation</template>
			<a href="/webjars/swagger-ui/index.html" target="_blank">
				/webjars/swagger-ui/index.html
			</a>
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label> Contributors</template>
			<el-image
				style="width: 100%; height: 100%"
				src="https://contrib.nn.ci/api?repo=ikaros-dev/ikaros&repo=ikaros-dev/docs&repo=ikaros-dev/app"
				fit="fill"
			/>
		</el-descriptions-item>
		<el-descriptions-item>
			<template #label> Status</template>
			<el-image
				style="width: 100%; height: 100%"
				src="https://repobeats.axiom.co/api/embed/f7285853048ff09f313f6483901e2af0e638f666.svg"
				fit="fill"
			/>
		</el-descriptions-item>
	</el-descriptions>
</template>
<style lang="scss" scoped></style>
