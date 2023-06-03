import axios from 'axios';
import type { AxiosInstance, AxiosError } from 'axios';
import { i18n } from '../locales';
import { PluginIkarosRunV1alpha1PluginApi } from '@runikaros/api-client';

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
			console.error(i18n.global.t('core.common.elmsg.network_error'), error);
			return Promise.reject(error);
		}

		const errorResponse = error.response;

		if (!errorResponse) {
			console.error(i18n.global.t('core.common.elmsg.network_error'), error);
			return Promise.reject(error);
		}

		const { status } = errorResponse;

		const { title } = errorResponse.data;

		if (status === 400) {
			console.error(
				i18n.global.t('core.common.elmsg.request_parameter_error', { title }),
				error
			);
		} else if (status === 401) {
			console.error(i18n.global.t('core.common.elmsg.unauthorized'), error);
		} else if (status === 403) {
			console.error(i18n.global.t('core.common.elmsg.forbidden'), error);
		} else if (status === 404) {
			console.error(i18n.global.t('core.common.elmsg.not_found'), error);
		} else if (status === 500) {
			console.error(
				i18n.global.t('core.common.elmsg.server_internal_error_with_title'),
				error
			);
		} else {
			console.error(
				i18n.global.t('core.common.elmsg.unknown_error_with_title', {
					title,
				}),
				error
			);
		}

		return Promise.reject(error);
	}
);

axiosInstance.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';

// eslint-disable-next-line no-shadow, no-unused-vars
function setupApiClient(axios: AxiosInstance) {
	return {
		// core endpoints

		// custom endpoints
		plugin: new PluginIkarosRunV1alpha1PluginApi(undefined, baseURL, axios),
	};
}

const apiClient = setupApiClient(axiosInstance);

export { apiClient };
