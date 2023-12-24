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
import { Episode } from '../models';
// @ts-ignore
import { EpisodeMeta } from '../models';
// @ts-ignore
import { EpisodeResource } from '../models';
/**
 * V1alpha1EpisodeApi - axios parameter creator
 * @export
 */
export const V1alpha1EpisodeApiAxiosParamCreator = function (
	configuration?: Configuration
) {
	return {
		/**
		 * Find episode all attachment refs by episode id.
		 * @param {number} id Episode id
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		findEpisodeAttachmentRefsById: async (
			id: number,
			options: AxiosRequestConfig = {}
		): Promise<RequestArgs> => {
			// verify required parameter 'id' is not null or undefined
			assertParamExists('findEpisodeAttachmentRefsById', 'id', id);
			const localVarPath = `/api/v1alpha1/episode/attachment/refs/{id}`.replace(
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
		/**
		 * Find episode by episode id.
		 * @param {number} id Episode id
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		findEpisodeById: async (
			id: number,
			options: AxiosRequestConfig = {}
		): Promise<RequestArgs> => {
			// verify required parameter 'id' is not null or undefined
			assertParamExists('findEpisodeById', 'id', id);
			const localVarPath = `/api/v1alpha1/episode/{id}`.replace(
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
		/**
		 * Find episode meta by episode id.
		 * @param {number} id Episode id
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		findEpisodeMetaById: async (
			id: number,
			options: AxiosRequestConfig = {}
		): Promise<RequestArgs> => {
			// verify required parameter 'id' is not null or undefined
			assertParamExists('findEpisodeMetaById', 'id', id);
			const localVarPath = `/api/v1alpha1/episode/meta/{id}`.replace(
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
 * V1alpha1EpisodeApi - functional programming interface
 * @export
 */
export const V1alpha1EpisodeApiFp = function (configuration?: Configuration) {
	const localVarAxiosParamCreator =
		V1alpha1EpisodeApiAxiosParamCreator(configuration);
	return {
		/**
		 * Find episode all attachment refs by episode id.
		 * @param {number} id Episode id
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		async findEpisodeAttachmentRefsById(
			id: number,
			options?: AxiosRequestConfig
		): Promise<
			(
				axios?: AxiosInstance,
				basePath?: string
			) => AxiosPromise<Array<EpisodeResource>>
		> {
			const localVarAxiosArgs =
				await localVarAxiosParamCreator.findEpisodeAttachmentRefsById(
					id,
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
		 * Find episode by episode id.
		 * @param {number} id Episode id
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		async findEpisodeById(
			id: number,
			options?: AxiosRequestConfig
		): Promise<
			(axios?: AxiosInstance, basePath?: string) => AxiosPromise<Episode>
		> {
			const localVarAxiosArgs = await localVarAxiosParamCreator.findEpisodeById(
				id,
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
		 * Find episode meta by episode id.
		 * @param {number} id Episode id
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		async findEpisodeMetaById(
			id: number,
			options?: AxiosRequestConfig
		): Promise<
			(axios?: AxiosInstance, basePath?: string) => AxiosPromise<EpisodeMeta>
		> {
			const localVarAxiosArgs =
				await localVarAxiosParamCreator.findEpisodeMetaById(id, options);
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
 * V1alpha1EpisodeApi - factory interface
 * @export
 */
export const V1alpha1EpisodeApiFactory = function (
	configuration?: Configuration,
	basePath?: string,
	axios?: AxiosInstance
) {
	const localVarFp = V1alpha1EpisodeApiFp(configuration);
	return {
		/**
		 * Find episode all attachment refs by episode id.
		 * @param {V1alpha1EpisodeApiFindEpisodeAttachmentRefsByIdRequest} requestParameters Request parameters.
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		findEpisodeAttachmentRefsById(
			requestParameters: V1alpha1EpisodeApiFindEpisodeAttachmentRefsByIdRequest,
			options?: AxiosRequestConfig
		): AxiosPromise<Array<EpisodeResource>> {
			return localVarFp
				.findEpisodeAttachmentRefsById(requestParameters.id, options)
				.then((request) => request(axios, basePath));
		},
		/**
		 * Find episode by episode id.
		 * @param {V1alpha1EpisodeApiFindEpisodeByIdRequest} requestParameters Request parameters.
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		findEpisodeById(
			requestParameters: V1alpha1EpisodeApiFindEpisodeByIdRequest,
			options?: AxiosRequestConfig
		): AxiosPromise<Episode> {
			return localVarFp
				.findEpisodeById(requestParameters.id, options)
				.then((request) => request(axios, basePath));
		},
		/**
		 * Find episode meta by episode id.
		 * @param {V1alpha1EpisodeApiFindEpisodeMetaByIdRequest} requestParameters Request parameters.
		 * @param {*} [options] Override http request option.
		 * @throws {RequiredError}
		 */
		findEpisodeMetaById(
			requestParameters: V1alpha1EpisodeApiFindEpisodeMetaByIdRequest,
			options?: AxiosRequestConfig
		): AxiosPromise<EpisodeMeta> {
			return localVarFp
				.findEpisodeMetaById(requestParameters.id, options)
				.then((request) => request(axios, basePath));
		},
	};
};

/**
 * Request parameters for findEpisodeAttachmentRefsById operation in V1alpha1EpisodeApi.
 * @export
 * @interface V1alpha1EpisodeApiFindEpisodeAttachmentRefsByIdRequest
 */
export interface V1alpha1EpisodeApiFindEpisodeAttachmentRefsByIdRequest {
	/**
	 * Episode id
	 * @type {number}
	 * @memberof V1alpha1EpisodeApiFindEpisodeAttachmentRefsById
	 */
	readonly id: number;
}

/**
 * Request parameters for findEpisodeById operation in V1alpha1EpisodeApi.
 * @export
 * @interface V1alpha1EpisodeApiFindEpisodeByIdRequest
 */
export interface V1alpha1EpisodeApiFindEpisodeByIdRequest {
	/**
	 * Episode id
	 * @type {number}
	 * @memberof V1alpha1EpisodeApiFindEpisodeById
	 */
	readonly id: number;
}

/**
 * Request parameters for findEpisodeMetaById operation in V1alpha1EpisodeApi.
 * @export
 * @interface V1alpha1EpisodeApiFindEpisodeMetaByIdRequest
 */
export interface V1alpha1EpisodeApiFindEpisodeMetaByIdRequest {
	/**
	 * Episode id
	 * @type {number}
	 * @memberof V1alpha1EpisodeApiFindEpisodeMetaById
	 */
	readonly id: number;
}

/**
 * V1alpha1EpisodeApi - object-oriented interface
 * @export
 * @class V1alpha1EpisodeApi
 * @extends {BaseAPI}
 */
export class V1alpha1EpisodeApi extends BaseAPI {
	/**
	 * Find episode all attachment refs by episode id.
	 * @param {V1alpha1EpisodeApiFindEpisodeAttachmentRefsByIdRequest} requestParameters Request parameters.
	 * @param {*} [options] Override http request option.
	 * @throws {RequiredError}
	 * @memberof V1alpha1EpisodeApi
	 */
	public findEpisodeAttachmentRefsById(
		requestParameters: V1alpha1EpisodeApiFindEpisodeAttachmentRefsByIdRequest,
		options?: AxiosRequestConfig
	) {
		return V1alpha1EpisodeApiFp(this.configuration)
			.findEpisodeAttachmentRefsById(requestParameters.id, options)
			.then((request) => request(this.axios, this.basePath));
	}

	/**
	 * Find episode by episode id.
	 * @param {V1alpha1EpisodeApiFindEpisodeByIdRequest} requestParameters Request parameters.
	 * @param {*} [options] Override http request option.
	 * @throws {RequiredError}
	 * @memberof V1alpha1EpisodeApi
	 */
	public findEpisodeById(
		requestParameters: V1alpha1EpisodeApiFindEpisodeByIdRequest,
		options?: AxiosRequestConfig
	) {
		return V1alpha1EpisodeApiFp(this.configuration)
			.findEpisodeById(requestParameters.id, options)
			.then((request) => request(this.axios, this.basePath));
	}

	/**
	 * Find episode meta by episode id.
	 * @param {V1alpha1EpisodeApiFindEpisodeMetaByIdRequest} requestParameters Request parameters.
	 * @param {*} [options] Override http request option.
	 * @throws {RequiredError}
	 * @memberof V1alpha1EpisodeApi
	 */
	public findEpisodeMetaById(
		requestParameters: V1alpha1EpisodeApiFindEpisodeMetaByIdRequest,
		options?: AxiosRequestConfig
	) {
		return V1alpha1EpisodeApiFp(this.configuration)
			.findEpisodeMetaById(requestParameters.id, options)
			.then((request) => request(this.axios, this.basePath));
	}
}
