<script setup lang="ts">
// @ts-expect-error
import VueFilePond from 'vue-filepond';
import 'filepond/dist/filepond.min.css';
import FilePondPluginFileValidateType from 'filepond-plugin-file-validate-type';
import { ref } from 'vue';
import { useUserStore } from '@/stores/user';

const filePondRef = ref(null);
const userStore = useUserStore();

const filePond = VueFilePond(FilePondPluginFileValidateType);

const props = withDefaults(
	defineProps<{
		endpoint: string;
	}>(),
	{}
);

const emit = defineEmits<{
	(event: 'uploaded', response: unknown): void;
	(event: 'error', error: unknown): void;
}>();

const detectJarType = (file: File): Promise<string> =>
	new Promise((resolve) => {
		if (file.name.endsWith('.jar')) {
			resolve('application/java-archive');
		} else {
			resolve('');
		}
	});

const server = {
	process: (
		fieldName: string,
		file: File,
		metadata: Record<string, unknown>,
		load: (response: unknown) => void,
		error: (response: unknown) => void,
		progress: (computable: boolean, loaded: number, total: number) => void,
		abort: () => void
	) => {
		const formData = new FormData();
		formData.append(fieldName, file, file.name);

		const xhr = new XMLHttpRequest();
		xhr.open('POST', `${import.meta.env.VITE_API_URL}${props.endpoint}`);
		xhr.withCredentials = true;
		xhr.setRequestHeader('Authorization', 'Bearer ' + userStore.jwtToken);

		xhr.upload.onprogress = (e) => {
			progress(e.lengthComputable, e.loaded, e.total);
		};

		xhr.onload = () => {
			if (xhr.status >= 200 && xhr.status < 300) {
				load(xhr.responseText);
				emit('uploaded', xhr.responseText);
			} else {
				try {
					const body = JSON.parse(xhr.responseText);
					error(body.message || xhr.statusText);
					emit('error', body);
				} catch {
					error(xhr.statusText);
					emit('error', xhr.statusText);
				}
			}
		};

		xhr.onerror = () => {
			error('Network error');
			emit('error', 'Network error');
		};

		xhr.send(formData);

		return { abort: () => xhr.abort() };
	},
};
</script>

<template>
	<file-pond
		ref="filePondRef"
		:server="server"
		:accepted-file-types="['application/java-archive']"
		:allow-multiple="false"
		:allowImagePreview="false"
		:allowRevert="false"
		:maxFiles="1"
		:name="'file'"
		label-idle="点击选择 .jar 文件或将文件拖拽到此处"
		fileValidateTypeLabelExpectedTypes="请选择 .jar 格式的文件"
		:fileValidateTypeDetectType="detectJarType"
	/>
</template>
