/* tslint:disable */
/* eslint-disable */
/**
 * Ikaros Open API Documentation
 * Documentation for Ikaros Open API
 *
 * The version of the OpenAPI document: 1.0.0
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import type { Configuration } from '../configuration';
import type { AxiosPromise, AxiosInstance, AxiosRequestConfig } from 'axios';
import globalAxios from 'axios';
// Some imports not used depending on template conditions
// @ts-ignore
import {
	DUMMY_BASE_URL,
	assertParamExists,
	setApiKeyToObject,
	setBasicAuthToObject,
	setBearerAuthToObject,
	setOAuthToObject,
	setSearchParams,
	serializeDataIfNeeded,
	toPathString,
	createRequestFunction,
} from '../common';
// @ts-ignore
import {
	BASE_PATH,
	COLLECTION_FORMATS,
	RequestArgs,
	BaseAPI,
	RequiredError,
} from '../base';
// @ts-ignore
import { PagingWrap } from '../models';
// @ts-ignore
import { Subject } from '../models';
/**
 * V1alpha1SubjectApi - axios parameter creator
 * @export
 */
export const V1alpha1SubjectApiAxiosParamCreator = function (
	configuration?: Configuration
) {
	return {
		/**
		 * Delete subject by id.
		 * @param {number} id Subject id
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		deleteSubjectById: async (
			id: number,
			options: AxiosRequestConfig = {}
		): Promise<RequestArgs> => {
			// verify required parameter 'id' is not null or undefined
			assertParamExists('deleteSubjectById', 'id', id);
			const localVarPath = `/api/v1alpha1/subject/{id}`.replace(
				`{${'id'}}`,
				encodeURIComponent(String(id))
			);
			// use dummy base URL string because the URL constructor only accepts absolute URLs.
			const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
			let baseOptions;
			if (configuration) {
				baseOptions = configuration.baseOptions;
			}

			const localVarRequestOptions = {
				method: 'DELETE',
				...baseOptions,
				...options,
			};
			const localVarHeaderParameter = {} as any;
			const localVarQueryParameter = {} as any;

			// authentication BasicAuth required
			// http basic authentication required
			setBasicAuthToObject(localVarRequestOptions, configuration);

			// authentication BearerAuth required
			// http bearer authentication required
			await setBearerAuthToObject(localVarHeaderParameter, configuration);

			setSearchParams(localVarUrlObj, localVarQueryParameter);
			let headersFromBaseOptions =
				baseOptions && baseOptions.headers ? baseOptions.headers : {};
			localVarRequestOptions.headers = {
				...localVarHeaderParameter,
				...headersFromBaseOptions,
				...options.headers,
			};

			return {
				url: toPathString(localVarUrlObj),
				options: localVarRequestOptions,
			};
		},
		/**
		 * List subjects by condition.
		 * @param {number} [page] 第几页，从1开始, 默认为1.
		 * @param {number} [size] 每页条数，默认为10.
		 * @param {string} [name] 经过Basic64编码的名称，名称字段模糊查询。
		 * @param {string} [nameCn] 经过Basic64编码的中文名称，中文名称字段模糊查询。
		 * @param {boolean} [nsfw] Not Safe/Suitable For Work.
		 * @param {'OTHER' | 'ANIME' | 'COMIC' | 'GAME' | 'MUSIC' | 'NOVEL' | 'REAL'} [type]
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		listSubjectsByCondition: async (
			page?: number,
			size?: number,
			name?: string,
			nameCn?: string,
			nsfw?: boolean,
			type?: 'OTHER' | 'ANIME' | 'COMIC' | 'GAME' | 'MUSIC' | 'NOVEL' | 'REAL',
			options: AxiosRequestConfig = {}
		): Promise<RequestArgs> => {
			const localVarPath = `/api/v1alpha1/subjects/condition`;
			// use dummy base URL string because the URL constructor only accepts absolute URLs.
			const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
			let baseOptions;
			if (configuration) {
				baseOptions = configuration.baseOptions;
			}

			const localVarRequestOptions = {
				method: 'GET',
				...baseOptions,
				...options,
			};
			const localVarHeaderParameter = {} as any;
			const localVarQueryParameter = {} as any;

			// authentication BasicAuth required
			// http basic authentication required
			setBasicAuthToObject(localVarRequestOptions, configuration);

			// authentication BearerAuth required
			// http bearer authentication required
			await setBearerAuthToObject(localVarHeaderParameter, configuration);

			if (page !== undefined) {
				localVarQueryParameter['page'] = page;
			}

			if (size !== undefined) {
				localVarQueryParameter['size'] = size;
			}

			if (name !== undefined) {
				localVarQueryParameter['name'] = name;
			}

			if (nameCn !== undefined) {
				localVarQueryParameter['nameCn'] = nameCn;
			}

			if (nsfw !== undefined) {
				localVarQueryParameter['nsfw'] = nsfw;
			}

			if (type !== undefined) {
				localVarQueryParameter['type'] = type;
			}

			setSearchParams(localVarUrlObj, localVarQueryParameter);
			let headersFromBaseOptions =
				baseOptions && baseOptions.headers ? baseOptions.headers : {};
			localVarRequestOptions.headers = {
				...localVarHeaderParameter,
				...headersFromBaseOptions,
				...options.headers,
			};

			return {
				url: toPathString(localVarUrlObj),
				options: localVarRequestOptions,
			};
		},
		/**
		 * Create or update single subject.
		 * @param {Subject} subject
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		saveSubject: async (
			subject: Subject,
			options: AxiosRequestConfig = {}
		): Promise<RequestArgs> => {
			// verify required parameter 'subject' is not null or undefined
			assertParamExists('saveSubject', 'subject', subject);
			const localVarPath = `/api/v1alpha1/subject`;
			// use dummy base URL string because the URL constructor only accepts absolute URLs.
			const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
			let baseOptions;
			if (configuration) {
				baseOptions = configuration.baseOptions;
			}

			const localVarRequestOptions = {
				method: 'POST',
				...baseOptions,
				...options,
			};
			const localVarHeaderParameter = {} as any;
			const localVarQueryParameter = {} as any;

			// authentication BasicAuth required
			// http basic authentication required
			setBasicAuthToObject(localVarRequestOptions, configuration);

			// authentication BearerAuth required
			// http bearer authentication required
			await setBearerAuthToObject(localVarHeaderParameter, configuration);

			localVarHeaderParameter['Content-Type'] = 'application/json';

			setSearchParams(localVarUrlObj, localVarQueryParameter);
			let headersFromBaseOptions =
				baseOptions && baseOptions.headers ? baseOptions.headers : {};
			localVarRequestOptions.headers = {
				...localVarHeaderParameter,
				...headersFromBaseOptions,
				...options.headers,
			};
			localVarRequestOptions.data = serializeDataIfNeeded(
				subject,
				localVarRequestOptions,
				configuration
			);

			return {
				url: toPathString(localVarUrlObj),
				options: localVarRequestOptions,
			};
		},
		/**
		 *
		 * @param {number} page Search page
		 * @param {number} size Search page size
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		searchAllSubjectByPaging: async (
			page: number,
			size: number,
			options: AxiosRequestConfig = {}
		): Promise<RequestArgs> => {
			// verify required parameter 'page' is not null or undefined
			assertParamExists('searchAllSubjectByPaging', 'page', page);
			// verify required parameter 'size' is not null or undefined
			assertParamExists('searchAllSubjectByPaging', 'size', size);
			const localVarPath = `/api/v1alpha1/subjects/{page}/{size}`
				.replace(`{${'page'}}`, encodeURIComponent(String(page)))
				.replace(`{${'size'}}`, encodeURIComponent(String(size)));
			// use dummy base URL string because the URL constructor only accepts absolute URLs.
			const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
			let baseOptions;
			if (configuration) {
				baseOptions = configuration.baseOptions;
			}

			const localVarRequestOptions = {
				method: 'GET',
				...baseOptions,
				...options,
			};
			const localVarHeaderParameter = {} as any;
			const localVarQueryParameter = {} as any;

			// authentication BasicAuth required
			// http basic authentication required
			setBasicAuthToObject(localVarRequestOptions, configuration);

			// authentication BearerAuth required
			// http bearer authentication required
			await setBearerAuthToObject(localVarHeaderParameter, configuration);

			setSearchParams(localVarUrlObj, localVarQueryParameter);
			let headersFromBaseOptions =
				baseOptions && baseOptions.headers ? baseOptions.headers : {};
			localVarRequestOptions.headers = {
				...localVarHeaderParameter,
				...headersFromBaseOptions,
				...options.headers,
			};

			return {
				url: toPathString(localVarUrlObj),
				options: localVarRequestOptions,
			};
		},
		/**
		 * Search single subject by id.
		 * @param {number} id Subject ID
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		searchSubjectById: async (
			id: number,
			options: AxiosRequestConfig = {}
		): Promise<RequestArgs> => {
			// verify required parameter 'id' is not null or undefined
			assertParamExists('searchSubjectById', 'id', id);
			const localVarPath = `/api/v1alpha1/subject/{id}`.replace(
				`{${'id'}}`,
				encodeURIComponent(String(id))
			);
			// use dummy base URL string because the URL constructor only accepts absolute URLs.
			const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
			let baseOptions;
			if (configuration) {
				baseOptions = configuration.baseOptions;
			}

			const localVarRequestOptions = {
				method: 'GET',
				...baseOptions,
				...options,
			};
			const localVarHeaderParameter = {} as any;
			const localVarQueryParameter = {} as any;

			// authentication BasicAuth required
			// http basic authentication required
			setBasicAuthToObject(localVarRequestOptions, configuration);

			// authentication BearerAuth required
			// http bearer authentication required
			await setBearerAuthToObject(localVarHeaderParameter, configuration);

			setSearchParams(localVarUrlObj, localVarQueryParameter);
			let headersFromBaseOptions =
				baseOptions && baseOptions.headers ? baseOptions.headers : {};
			localVarRequestOptions.headers = {
				...localVarHeaderParameter,
				...headersFromBaseOptions,
				...options.headers,
			};

			return {
				url: toPathString(localVarUrlObj),
				options: localVarRequestOptions,
			};
		},
	};
};

/**
 * V1alpha1SubjectApi - functional programming interface
 * @export
 */
export const V1alpha1SubjectApiFp = function (configuration?: Configuration) {
	const localVarAxiosParamCreator =
		V1alpha1SubjectApiAxiosParamCreator(configuration);
	return {
		/**
		 * Delete subject by id.
		 * @param {number} id Subject id
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		async deleteSubjectById(
			id: number,
			options?: AxiosRequestConfig
		): Promise<
			(axios?: AxiosInstance, basePath?: string) => AxiosPromise<void>
		> {
			const localVarAxiosArgs =
				await localVarAxiosParamCreator.deleteSubjectById(id, options);
			return createRequestFunction(
				localVarAxiosArgs,
				globalAxios,
				BASE_PATH,
				configuration
			);
		},
		/**
		 * List subjects by condition.
		 * @param {number} [page] 第几页，从1开始, 默认为1.
		 * @param {number} [size] 每页条数，默认为10.
		 * @param {string} [name] 经过Basic64编码的名称，名称字段模糊查询。
		 * @param {string} [nameCn] 经过Basic64编码的中文名称，中文名称字段模糊查询。
		 * @param {boolean} [nsfw] Not Safe/Suitable For Work.
		 * @param {'OTHER' | 'ANIME' | 'COMIC' | 'GAME' | 'MUSIC' | 'NOVEL' | 'REAL'} [type]
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		async listSubjectsByCondition(
			page?: number,
			size?: number,
			name?: string,
			nameCn?: string,
			nsfw?: boolean,
			type?: 'OTHER' | 'ANIME' | 'COMIC' | 'GAME' | 'MUSIC' | 'NOVEL' | 'REAL',
			options?: AxiosRequestConfig
		): Promise<
			(axios?: AxiosInstance, basePath?: string) => AxiosPromise<PagingWrap>
		> {
			const localVarAxiosArgs =
				await localVarAxiosParamCreator.listSubjectsByCondition(
					page,
					size,
					name,
					nameCn,
					nsfw,
					type,
					options
				);
			return createRequestFunction(
				localVarAxiosArgs,
				globalAxios,
				BASE_PATH,
				configuration
			);
		},
		/**
		 * Create or update single subject.
		 * @param {Subject} subject
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		async saveSubject(
			subject: Subject,
			options?: AxiosRequestConfig
		): Promise<
			(axios?: AxiosInstance, basePath?: string) => AxiosPromise<Subject>
		> {
			const localVarAxiosArgs = await localVarAxiosParamCreator.saveSubject(
				subject,
				options
			);
			return createRequestFunction(
				localVarAxiosArgs,
				globalAxios,
				BASE_PATH,
				configuration
			);
		},
		/**
		 *
		 * @param {number} page Search page
		 * @param {number} size Search page size
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		async searchAllSubjectByPaging(
			page: number,
			size: number,
			options?: AxiosRequestConfig
		): Promise<
			(axios?: AxiosInstance, basePath?: string) => AxiosPromise<PagingWrap>
		> {
			const localVarAxiosArgs =
				await localVarAxiosParamCreator.searchAllSubjectByPaging(
					page,
					size,
					options
				);
			return createRequestFunction(
				localVarAxiosArgs,
				globalAxios,
				BASE_PATH,
				configuration
			);
		},
		/**
		 * Search single subject by id.
		 * @param {number} id Subject ID
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		async searchSubjectById(
			id: number,
			options?: AxiosRequestConfig
		): Promise<
			(axios?: AxiosInstance, basePath?: string) => AxiosPromise<Subject>
		> {
			const localVarAxiosArgs =
				await localVarAxiosParamCreator.searchSubjectById(id, options);
			return createRequestFunction(
				localVarAxiosArgs,
				globalAxios,
				BASE_PATH,
				configuration
			);
		},
	};
};

/**
 * V1alpha1SubjectApi - factory interface
 * @export
 */
export const V1alpha1SubjectApiFactory = function (
	configuration?: Configuration,
	basePath?: string,
	axios?: AxiosInstance
) {
	const localVarFp = V1alpha1SubjectApiFp(configuration);
	return {
		/**
		 * Delete subject by id.
		 * @param {V1alpha1SubjectApiDeleteSubjectByIdRequest} requestParameters Request parameters.
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		deleteSubjectById(
			requestParameters: V1alpha1SubjectApiDeleteSubjectByIdRequest,
			options?: AxiosRequestConfig
		): AxiosPromise<void> {
			return localVarFp
				.deleteSubjectById(requestParameters.id, options)
				.then((request) => request(axios, basePath));
		},
		/**
		 * List subjects by condition.
		 * @param {V1alpha1SubjectApiListSubjectsByConditionRequest} requestParameters Request parameters.
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		listSubjectsByCondition(
			requestParameters: V1alpha1SubjectApiListSubjectsByConditionRequest = {},
			options?: AxiosRequestConfig
		): AxiosPromise<PagingWrap> {
			return localVarFp
				.listSubjectsByCondition(
					requestParameters.page,
					requestParameters.size,
					requestParameters.name,
					requestParameters.nameCn,
					requestParameters.nsfw,
					requestParameters.type,
					options
				)
				.then((request) => request(axios, basePath));
		},
		/**
		 * Create or update single subject.
		 * @param {V1alpha1SubjectApiSaveSubjectRequest} requestParameters Request parameters.
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		saveSubject(
			requestParameters: V1alpha1SubjectApiSaveSubjectRequest,
			options?: AxiosRequestConfig
		): AxiosPromise<Subject> {
			return localVarFp
				.saveSubject(requestParameters.subject, options)
				.then((request) => request(axios, basePath));
		},
		/**
		 *
		 * @param {V1alpha1SubjectApiSearchAllSubjectByPagingRequest} requestParameters Request parameters.
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		searchAllSubjectByPaging(
			requestParameters: V1alpha1SubjectApiSearchAllSubjectByPagingRequest,
			options?: AxiosRequestConfig
		): AxiosPromise<PagingWrap> {
			return localVarFp
				.searchAllSubjectByPaging(
					requestParameters.page,
					requestParameters.size,
					options
				)
				.then((request) => request(axios, basePath));
		},
		/**
		 * Search single subject by id.
		 * @param {V1alpha1SubjectApiSearchSubjectByIdRequest} requestParameters Request parameters.
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		searchSubjectById(
			requestParameters: V1alpha1SubjectApiSearchSubjectByIdRequest,
			options?: AxiosRequestConfig
		): AxiosPromise<Subject> {
			return localVarFp
				.searchSubjectById(requestParameters.id, options)
				.then((request) => request(axios, basePath));
		},
	};
};

/**
 * Request parameters for deleteSubjectById operation in V1alpha1SubjectApi.
 * @export
 * @interface V1alpha1SubjectApiDeleteSubjectByIdRequest
 */
export interface V1alpha1SubjectApiDeleteSubjectByIdRequest {
	/**
	 * Subject id
	 * @type {number}
	 * @memberof V1alpha1SubjectApiDeleteSubjectById
	 */
	readonly id: number;
}

/**
 * Request parameters for listSubjectsByCondition operation in V1alpha1SubjectApi.
 * @export
 * @interface V1alpha1SubjectApiListSubjectsByConditionRequest
 */
export interface V1alpha1SubjectApiListSubjectsByConditionRequest {
	/**
	 * 第几页，从1开始, 默认为1.
	 * @type {number}
	 * @memberof V1alpha1SubjectApiListSubjectsByCondition
	 */
	readonly page?: number;

	/**
	 * 每页条数，默认为10.
	 * @type {number}
	 * @memberof V1alpha1SubjectApiListSubjectsByCondition
	 */
	readonly size?: number;

	/**
	 * 经过Basic64编码的名称，名称字段模糊查询。
	 * @type {string}
	 * @memberof V1alpha1SubjectApiListSubjectsByCondition
	 */
	readonly name?: string;

	/**
	 * 经过Basic64编码的中文名称，中文名称字段模糊查询。
	 * @type {string}
	 * @memberof V1alpha1SubjectApiListSubjectsByCondition
	 */
	readonly nameCn?: string;

	/**
	 * Not Safe/Suitable For Work.
	 * @type {boolean}
	 * @memberof V1alpha1SubjectApiListSubjectsByCondition
	 */
	readonly nsfw?: boolean;

	/**
	 *
	 * @type {'OTHER' | 'ANIME' | 'COMIC' | 'GAME' | 'MUSIC' | 'NOVEL' | 'REAL'}
	 * @memberof V1alpha1SubjectApiListSubjectsByCondition
	 */
	readonly type?:
		| 'OTHER'
		| 'ANIME'
		| 'COMIC'
		| 'GAME'
		| 'MUSIC'
		| 'NOVEL'
		| 'REAL';
}

/**
 * Request parameters for saveSubject operation in V1alpha1SubjectApi.
 * @export
 * @interface V1alpha1SubjectApiSaveSubjectRequest
 */
export interface V1alpha1SubjectApiSaveSubjectRequest {
	/**
	 *
	 * @type {Subject}
	 * @memberof V1alpha1SubjectApiSaveSubject
	 */
	readonly subject: Subject;
}

/**
 * Request parameters for searchAllSubjectByPaging operation in V1alpha1SubjectApi.
 * @export
 * @interface V1alpha1SubjectApiSearchAllSubjectByPagingRequest
 */
export interface V1alpha1SubjectApiSearchAllSubjectByPagingRequest {
	/**
	 * Search page
	 * @type {number}
	 * @memberof V1alpha1SubjectApiSearchAllSubjectByPaging
	 */
	readonly page: number;

	/**
	 * Search page size
	 * @type {number}
	 * @memberof V1alpha1SubjectApiSearchAllSubjectByPaging
	 */
	readonly size: number;
}

/**
 * Request parameters for searchSubjectById operation in V1alpha1SubjectApi.
 * @export
 * @interface V1alpha1SubjectApiSearchSubjectByIdRequest
 */
export interface V1alpha1SubjectApiSearchSubjectByIdRequest {
	/**
	 * Subject ID
	 * @type {number}
	 * @memberof V1alpha1SubjectApiSearchSubjectById
	 */
	readonly id: number;
}

/**
 * V1alpha1SubjectApi - object-oriented interface
 * @export
 * @class V1alpha1SubjectApi
 * @extends {BaseAPI}
 */
export class V1alpha1SubjectApi extends BaseAPI {
	/**
	 * Delete subject by id.
	 * @param {V1alpha1SubjectApiDeleteSubjectByIdRequest} requestParameters Request parameters.
	 * @param {*} [options] Override http request option.
	 * @throws {RequiredError}
	 * @memberof V1alpha1SubjectApi
	 */
	public deleteSubjectById(
		requestParameters: V1alpha1SubjectApiDeleteSubjectByIdRequest,
		options?: AxiosRequestConfig
	) {
		return V1alpha1SubjectApiFp(this.configuration)
			.deleteSubjectById(requestParameters.id, options)
			.then((request) => request(this.axios, this.basePath));
	}

	/**
	 * List subjects by condition.
	 * @param {V1alpha1SubjectApiListSubjectsByConditionRequest} requestParameters Request parameters.
	 * @param {*} [options] Override http request option.
	 * @throws {RequiredError}
	 * @memberof V1alpha1SubjectApi
	 */
	public listSubjectsByCondition(
		requestParameters: V1alpha1SubjectApiListSubjectsByConditionRequest = {},
		options?: AxiosRequestConfig
	) {
		return V1alpha1SubjectApiFp(this.configuration)
			.listSubjectsByCondition(
				requestParameters.page,
				requestParameters.size,
				requestParameters.name,
				requestParameters.nameCn,
				requestParameters.nsfw,
				requestParameters.type,
				options
			)
			.then((request) => request(this.axios, this.basePath));
	}

	/**
	 * Create or update single subject.
	 * @param {V1alpha1SubjectApiSaveSubjectRequest} requestParameters Request parameters.
	 * @param {*} [options] Override http request option.
	 * @throws {RequiredError}
	 * @memberof V1alpha1SubjectApi
	 */
	public saveSubject(
		requestParameters: V1alpha1SubjectApiSaveSubjectRequest,
		options?: AxiosRequestConfig
	) {
		return V1alpha1SubjectApiFp(this.configuration)
			.saveSubject(requestParameters.subject, options)
			.then((request) => request(this.axios, this.basePath));
	}

	/**
	 *
	 * @param {V1alpha1SubjectApiSearchAllSubjectByPagingRequest} requestParameters Request parameters.
	 * @param {*} [options] Override http request option.
	 * @throws {RequiredError}
	 * @memberof V1alpha1SubjectApi
	 */
	public searchAllSubjectByPaging(
		requestParameters: V1alpha1SubjectApiSearchAllSubjectByPagingRequest,
		options?: AxiosRequestConfig
	) {
		return V1alpha1SubjectApiFp(this.configuration)
			.searchAllSubjectByPaging(
				requestParameters.page,
				requestParameters.size,
				options
			)
			.then((request) => request(this.axios, this.basePath));
	}

	/**
	 * Search single subject by id.
	 * @param {V1alpha1SubjectApiSearchSubjectByIdRequest} requestParameters Request parameters.
	 * @param {*} [options] Override http request option.
	 * @throws {RequiredError}
	 * @memberof V1alpha1SubjectApi
	 */
	public searchSubjectById(
		requestParameters: V1alpha1SubjectApiSearchSubjectByIdRequest,
		options?: AxiosRequestConfig
	) {
		return V1alpha1SubjectApiFp(this.configuration)
			.searchSubjectById(requestParameters.id, options)
			.then((request) => request(this.axios, this.basePath));
	}
}
