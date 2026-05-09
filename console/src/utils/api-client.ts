import type { AxiosError, AxiosInstance } from 'axios';
import axios from 'axios';
import { i18n } from '../locales';
import {
	ActuatorApi,
	PluginIkarosRunV1PluginApi,
	SettingIkarosRunV1ConfigmapApi,
	V1AttachmentApi,
	V1AttachmentDriverApi,
	V1AttachmentReferenceApi,
	V1AttachmentRelationApi,
	V1AuthorityApi,
	V1CollectionEpisodeApi,
	V1CollectionSubjectApi,
	V1EpisodeApi,
	V1IndicesApi,
	V1NotifyApi,
	V1PluginApi,
	V1RoleApi,
	V1RoleAuthorityApi,
	V1SecurityApi,
	V1StaticApi,
	V1SubjectApi,
	V1SubjectRelationApi,
	V1SubjectSyncApi,
	V1TagApi,
	V1TaskApi,
	V1UserApi,
	V1UserMeApi,
	V1UserRoleApi,
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
				i18n.global.t('common.exception.network_error') + ': ' + msg,
				error
			);
			ElMessage.error(
				i18n.global.t('common.exception.network_error') + ': ' + msg
			);
			return Promise.reject(error);
		}

		const errorResponse = error.response;

		if (!errorResponse) {
			console.error(
				i18n.global.t('common.exception.network_error') + ': ' + msg,
				error
			);
			ElMessage.error(
				i18n.global.t('common.exception.network_error') + ': ' + msg
			);
			return Promise.reject(error);
		}

		const { status } = errorResponse;

		const { title } = errorResponse.data;

		if (status === 400) {
			console.error(
				i18n.global.t('common.exception.request_parameter_error', {
					title,
				}),
				error
			);
			ElMessage.error(
				i18n.global.t('common.exception.request_parameter_error') + ': ' + msg
			);
		} else if (status === 401) {
			console.error(
				i18n.global.t('common.exception.unauthorized') + ': ' + msg,
				error
			);
			ElMessage.error(
				i18n.global.t('common.exception.unauthorized') + ': ' + msg
			);
		} else if (status === 403) {
			console.error(
				i18n.global.t('common.exception.forbidden') + ': ' + msg,
				error
			);
			ElMessage.error(i18n.global.t('common.exception.forbidden') + ': ' + msg);
		} else if (status === 404) {
			return Promise.resolve();
			// console.error(
			// 	i18n.global.t('common.exception.not_found') + ': ' + msg,
			// 	error
			// );
			// ElMessage.error(
			// 	i18n.global.t('common.exception.not_found') + ': ' + msg
			// );
		} else if (status === 500) {
			return Promise.reject(error);
			// console.error(
			// 	i18n.global.t(
			// 		'common.exception.server_internal_error_with_title'
			// 	) +
			// 		': ' +
			// 		msg,
			// 	error
			// );
			// ElMessage.error(
			// 	i18n.global.t(
			// 		'common.exception.server_internal_error_with_title'
			// 	) +
			// 		': ' +
			// 		msg
			// );
		} else {
			console.error(
				i18n.global.t('common.exception.unknown_error_with_title', {
					title,
				}) +
					': ' +
					msg,
				error
			);
			ElMessage.error(
				i18n.global.t('common.exception.unknown_error_with_title') + ': ' + msg
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
		user: new V1UserApi(undefined, baseURL, axios),
		userMe: new V1UserMeApi(undefined, baseURL, axios),
		role: new V1RoleApi(undefined, baseURL, axios),
		userRole: new V1UserRoleApi(undefined, baseURL, axios),
		roleAuthority: new V1RoleAuthorityApi(undefined, baseURL, axios),
		authority: new V1AuthorityApi(undefined, baseURL, axios),
		corePlugin: new V1PluginApi(undefined, baseURL, axios),
		attachment: new V1AttachmentApi(undefined, baseURL, axios),
		attachmentRef: new V1AttachmentReferenceApi(
			undefined,
			baseURL,
			axios
		),
		attachmentRelation: new V1AttachmentRelationApi(
			undefined,
			baseURL,
			axios
		),
		attachmentDriver: new V1AttachmentDriverApi(undefined, baseURL, axios),
		subject: new V1SubjectApi(undefined, baseURL, axios),
		subjectSync: new V1SubjectSyncApi(undefined, baseURL, axios),
		indices: new V1IndicesApi(undefined, baseURL, axios),
		task: new V1TaskApi(undefined, baseURL, axios),
		collectionSubject: new V1CollectionSubjectApi(
			undefined,
			baseURL,
			axios
		),
		subjectRelation: new V1SubjectRelationApi(undefined, baseURL, axios),
		episode: new V1EpisodeApi(undefined, baseURL, axios),
		collectionEpisode: new V1CollectionEpisodeApi(
			undefined,
			baseURL,
			axios
		),
		tag: new V1TagApi(undefined, baseURL, axios),
		staticRes: new V1StaticApi(undefined, baseURL, axios),
		security: new V1SecurityApi(undefined, baseURL, axios),
		notify: new V1NotifyApi(undefined, baseURL, axios),
		// custom endpoints
		plugin: new PluginIkarosRunV1PluginApi(undefined, baseURL, axios),
		configmap: new SettingIkarosRunV1ConfigmapApi(
			undefined,
			baseURL,
			axios
		),
	};
}

const setApiClientJwtToken = (token: string) => {
	// console.debug('setJwtToken', token)
	if (!token) return;
	axiosInstance.defaults.headers.common['Authorization'] = 'Bearer ' + token;
};

export { apiClient, setApiClientJwtToken };
