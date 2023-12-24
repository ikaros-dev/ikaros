import axios from 'axios';
import { i18n } from '../locales';
import type { AxiosError, AxiosInstance } from 'axios';
import {
	PluginIkarosRunV1alpha1PluginApi,
	SettingIkarosRunV1alpha1ConfigmapApi,
	V1alpha1EpisodeCollectionApi,
	V1alpha1SubjectCollectionApi,
	V1alpha1IndicesApi,
	V1alpha1PluginApi,
	V1alpha1SubjectApi,
	V1alpha1SubjectSyncPlatformApi,
	V1alpha1TaskApi,
	V1alpha1UserApi,
	V1alpha1SubjectRelationApi,
	V1alpha1AttachmentApi,
	V1alpha1AttachmentReferenceApi,
	ActuatorApi,
	V1alpha1TagApi,
	V1alpha1AttachmentRelationApi,
	V1alpha1StaticApi,
	V1alpha1EpisodeApi,
} from '@runikaros/api-client';
import { ElMessage } from 'element-plus';

const baseURL = import.meta.env.VITE_API_URL;

const axiosInstance = axios.create({
	baseURL,
	withCredentials: true,
});

export interface ProblemDetail {
	detail: string;
	instance: string;
	status: number;
	title: string;
	type?: string;
}

axiosInstance.interceptors.response.use(
	(response) => response,
	async (error: AxiosError<ProblemDetail>) => {
		// @ts-ignore
		let msg = error?.response?.data?.message;
		if (!msg) {
			msg = error.message;
		}

		if (/Network Error/.test(msg)) {
			console.error(
				i18n.global.t('core.common.exception.network_error') + ': ' + msg,
				error
			);
			ElMessage.error(
				i18n.global.t('core.common.exception.network_error') + ': ' + msg
			);
			return Promise.reject(error);
		}

		const errorResponse = error.response;

		if (!errorResponse) {
			console.error(
				i18n.global.t('core.common.exception.network_error') + ': ' + msg,
				error
			);
			ElMessage.error(
				i18n.global.t('core.common.exception.network_error') + ': ' + msg
			);
			return Promise.reject(error);
		}

		const { status } = errorResponse;

		const { title } = errorResponse.data;

		if (status === 400) {
			console.error(
				i18n.global.t('core.common.exception.request_parameter_error', {
					title,
				}),
				error
			);
			ElMessage.error(
				i18n.global.t('core.common.exception.request_parameter_error') +
					': ' +
					msg
			);
		} else if (status === 401) {
			console.error(
				i18n.global.t('core.common.exception.unauthorized') + ': ' + msg,
				error
			);
			ElMessage.error(
				i18n.global.t('core.common.exception.unauthorized') + ': ' + msg
			);
		} else if (status === 403) {
			console.error(
				i18n.global.t('core.common.exception.forbidden') + ': ' + msg,
				error
			);
			ElMessage.error(
				i18n.global.t('core.common.exception.forbidden') + ': ' + msg
			);
		} else if (status === 404) {
			return Promise.resolve();
			// console.error(
			// 	i18n.global.t('core.common.exception.not_found') + ': ' + msg,
			// 	error
			// );
			// ElMessage.error(
			// 	i18n.global.t('core.common.exception.not_found') + ': ' + msg
			// );
		} else if (status === 500) {
			return Promise.reject(error);
			// console.error(
			// 	i18n.global.t(
			// 		'core.common.exception.server_internal_error_with_title'
			// 	) +
			// 		': ' +
			// 		msg,
			// 	error
			// );
			// ElMessage.error(
			// 	i18n.global.t(
			// 		'core.common.exception.server_internal_error_with_title'
			// 	) +
			// 		': ' +
			// 		msg
			// );
		} else {
			console.error(
				i18n.global.t('core.common.exception.unknown_error_with_title', {
					title,
				}) +
					': ' +
					msg,
				error
			);
			ElMessage.error(
				i18n.global.t('core.common.exception.unknown_error_with_title') +
					': ' +
					msg
			);
		}

		return Promise.reject(error);
	}
);

axiosInstance.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';

const apiClient = setupApiClient(axiosInstance);

// eslint-disable-next-line no-shadow, no-unused-vars
function setupApiClient(axios: AxiosInstance) {
	return {
		// actuator
		actuator: new ActuatorApi(undefined, baseURL, axios),
		// core endpoints
		user: new V1alpha1UserApi(undefined, baseURL, axios),
		corePlugin: new V1alpha1PluginApi(undefined, baseURL, axios),
		attachment: new V1alpha1AttachmentApi(undefined, baseURL, axios),
		attachmentRef: new V1alpha1AttachmentReferenceApi(
			undefined,
			baseURL,
			axios
		),
		attachmentRelation: new V1alpha1AttachmentRelationApi(
			undefined,
			baseURL,
			axios
		),
		subject: new V1alpha1SubjectApi(undefined, baseURL, axios),
		subjectSyncPlatform: new V1alpha1SubjectSyncPlatformApi(
			undefined,
			baseURL,
			axios
		),
		indices: new V1alpha1IndicesApi(undefined, baseURL, axios),
		task: new V1alpha1TaskApi(undefined, baseURL, axios),
		subjectCollection: new V1alpha1SubjectCollectionApi(
			undefined,
			baseURL,
			axios
		),
		subjectRelation: new V1alpha1SubjectRelationApi(undefined, baseURL, axios),
		episode: new V1alpha1EpisodeApi(undefined, baseURL, axios),
		episodeCollection: new V1alpha1EpisodeCollectionApi(
			undefined,
			baseURL,
			axios
		),
		tag: new V1alpha1TagApi(undefined, baseURL, axios),
		staticRes: new V1alpha1StaticApi(undefined, baseURL, axios),
		// custom endpoints
		plugin: new PluginIkarosRunV1alpha1PluginApi(undefined, baseURL, axios),
		configmap: new SettingIkarosRunV1alpha1ConfigmapApi(
			undefined,
			baseURL,
			axios
		),
	};
}

export { apiClient };
