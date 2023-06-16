import axios from 'axios';
import { i18n } from '../locales';
import type { AxiosError, AxiosInstance } from 'axios';
import {
	PluginIkarosRunV1alpha1PluginApi,
	SettingIkarosRunV1alpha1ConfigMapApi,
	V1alpha1EpisodeFileApi,
	V1alpha1FileApi,
	V1alpha1PluginApi,
	V1alpha1SubjectApi,
	V1alpha1UserApi,
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
		if (/Network Error/.test(error.message)) {
			console.error(
				i18n.global.t('core.common.exception.network_error') +
					': ' +
					error.message,
				error
			);
			ElMessage.error(
				i18n.global.t('core.common.exception.network_error') +
					': ' +
					error.message
			);
			return Promise.reject(error);
		}

		const errorResponse = error.response;

		if (!errorResponse) {
			console.error(
				i18n.global.t('core.common.exception.network_error') +
					': ' +
					error.message,
				error
			);
			ElMessage.error(
				i18n.global.t('core.common.exception.network_error') +
					': ' +
					error.message
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
					error.message
			);
		} else if (status === 401) {
			console.error(
				i18n.global.t('core.common.exception.unauthorized') +
					': ' +
					error.message,
				error
			);
			ElMessage.error(
				i18n.global.t('core.common.exception.unauthorized') +
					': ' +
					error.message
			);
		} else if (status === 403) {
			console.error(
				i18n.global.t('core.common.exception.forbidden') + ': ' + error.message,
				error
			);
			ElMessage.error(
				i18n.global.t('core.common.exception.forbidden') + ': ' + error.message
			);
		} else if (status === 404) {
			console.error(
				i18n.global.t('core.common.exception.not_found') + ': ' + error.message,
				error
			);
			ElMessage.error(
				i18n.global.t('core.common.exception.not_found') + ': ' + error.message
			);
		} else if (status === 500) {
			console.error(
				i18n.global.t(
					'core.common.exception.server_internal_error_with_title'
				) +
					': ' +
					error.message,
				error
			);
			ElMessage.error(
				i18n.global.t(
					'core.common.exception.server_internal_error_with_title'
				) +
					': ' +
					error.message
			);
		} else {
			console.error(
				i18n.global.t('core.common.exception.unknown_error_with_title', {
					title,
				}) +
					': ' +
					error.message,
				error
			);
			ElMessage.error(
				i18n.global.t('core.common.exception.unknown_error_with_title') +
					': ' +
					error.message
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
		// core endpoints
		user: new V1alpha1UserApi(undefined, baseURL, axios),
		corePlugin: new V1alpha1PluginApi(undefined, baseURL, axios),
		file: new V1alpha1FileApi(undefined, baseURL, axios),
		subject: new V1alpha1SubjectApi(undefined, baseURL, axios),
		episodefile: new V1alpha1EpisodeFileApi(undefined, baseURL, axios),
		// custom endpoints
		plugin: new PluginIkarosRunV1alpha1PluginApi(undefined, baseURL, axios),
		configmap: new SettingIkarosRunV1alpha1ConfigMapApi(
			undefined,
			baseURL,
			axios
		),
	};
}

export { apiClient };
