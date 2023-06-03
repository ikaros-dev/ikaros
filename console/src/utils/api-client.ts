import axios from 'axios';
import type { AxiosInstance, AxiosError } from 'axios';
import { i18n } from '../locales';
import { ElMessage } from 'element-plus';
import { CoreIkarosRunV1alpha1PluginApi } from '@runikaros/api-client';

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
			ElMessage.error(i18n.global.t('core.common.elmsg.network_error'));
			return Promise.reject(error);
		}

		const errorResponse = error.response;

		if (!errorResponse) {
			ElMessage.error(i18n.global.t('core.common.elmsg.network_error'));
			return Promise.reject(error);
		}

		const { status } = errorResponse;

		const { title } = errorResponse.data;

		if (status === 400) {
			ElMessage.error(
				i18n.global.t('core.common.elmsg.request_parameter_error', { title })
			);
		} else if (status === 401) {
			// const userStore = useUserStore();
			// userStore.loginModalVisible = true;
			ElMessage.warning(i18n.global.t('core.common.elmsg.login_expired'));
		} else if (status === 403) {
			ElMessage.error(i18n.global.t('core.common.elmsg.forbidden'));
		} else if (status === 404) {
			ElMessage.error(i18n.global.t('core.common.elmsg.not_found'));
		} else if (status === 500) {
			ElMessage.error(
				i18n.global.t('core.common.elmsg.server_internal_error_with_title')
			);
		} else {
			ElMessage.error(
				i18n.global.t('core.common.elmsg.unknown_error_with_title', {
					title,
				})
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
		plugin: new CoreIkarosRunV1alpha1PluginApi(undefined, baseURL, axios),
	};
}

const apiClient = setupApiClient(axiosInstance);

export { apiClient };
