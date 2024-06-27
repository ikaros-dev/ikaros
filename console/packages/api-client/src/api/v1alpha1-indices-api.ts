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

import type { Configuration } from "../configuration";
import type { AxiosPromise, AxiosInstance, AxiosRequestConfig } from "axios";
import globalAxios from "axios";
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
} from "../common";
// @ts-ignore
import {
  BASE_PATH,
  COLLECTION_FORMATS,
  RequestArgs,
  BaseAPI,
  RequiredError,
} from "../base";
// @ts-ignore
import { SubjectHints } from "../models";
/**
 * V1alpha1IndicesApi - axios parameter creator
 * @export
 */
export const V1alpha1IndicesApiAxiosParamCreator = function (
  configuration?: Configuration
) {
  return {
    /**
     * Build or rebuild subject indices for full text search
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    buildSubjectIndices: async (
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      const localVarPath = `/api/v1alpha1/indices/subject`;
      // use dummy base URL string because the URL constructor only accepts absolute URLs.
      const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
      let baseOptions;
      if (configuration) {
        baseOptions = configuration.baseOptions;
      }

      const localVarRequestOptions = {
        method: "POST",
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
     * Search subjects with fuzzy query
     * @param {string} keyword
     * @param {number} [limit]
     * @param {string} [highlightPostTag]
     * @param {string} [highlightPreTag]
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    searchSubject: async (
      keyword: string,
      limit?: number,
      highlightPostTag?: string,
      highlightPreTag?: string,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'keyword' is not null or undefined
      assertParamExists("searchSubject", "keyword", keyword);
      const localVarPath = `/api/v1alpha1/indices/subject`;
      // use dummy base URL string because the URL constructor only accepts absolute URLs.
      const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
      let baseOptions;
      if (configuration) {
        baseOptions = configuration.baseOptions;
      }

      const localVarRequestOptions = {
        method: "GET",
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

      if (limit !== undefined) {
        localVarQueryParameter["limit"] = limit;
      }

      if (highlightPostTag !== undefined) {
        localVarQueryParameter["highlightPostTag"] = highlightPostTag;
      }

      if (highlightPreTag !== undefined) {
        localVarQueryParameter["highlightPreTag"] = highlightPreTag;
      }

      if (keyword !== undefined) {
        localVarQueryParameter["keyword"] = keyword;
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
  };
};

/**
 * V1alpha1IndicesApi - functional programming interface
 * @export
 */
export const V1alpha1IndicesApiFp = function (configuration?: Configuration) {
  const localVarAxiosParamCreator =
    V1alpha1IndicesApiAxiosParamCreator(configuration);
  return {
    /**
     * Build or rebuild subject indices for full text search
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async buildSubjectIndices(
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<void>
    > {
      const localVarAxiosArgs =
        await localVarAxiosParamCreator.buildSubjectIndices(options);
      return createRequestFunction(
        localVarAxiosArgs,
        globalAxios,
        BASE_PATH,
        configuration
      );
    },
    /**
     * Search subjects with fuzzy query
     * @param {string} keyword
     * @param {number} [limit]
     * @param {string} [highlightPostTag]
     * @param {string} [highlightPreTag]
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async searchSubject(
      keyword: string,
      limit?: number,
      highlightPostTag?: string,
      highlightPreTag?: string,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<SubjectHints>
    > {
      const localVarAxiosArgs = await localVarAxiosParamCreator.searchSubject(
        keyword,
        limit,
        highlightPostTag,
        highlightPreTag,
        options
      );
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
 * V1alpha1IndicesApi - factory interface
 * @export
 */
export const V1alpha1IndicesApiFactory = function (
  configuration?: Configuration,
  basePath?: string,
  axios?: AxiosInstance
) {
  const localVarFp = V1alpha1IndicesApiFp(configuration);
  return {
    /**
     * Build or rebuild subject indices for full text search
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    buildSubjectIndices(options?: AxiosRequestConfig): AxiosPromise<void> {
      return localVarFp
        .buildSubjectIndices(options)
        .then((request) => request(axios, basePath));
    },
    /**
     * Search subjects with fuzzy query
     * @param {V1alpha1IndicesApiSearchSubjectRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    searchSubject(
      requestParameters: V1alpha1IndicesApiSearchSubjectRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<SubjectHints> {
      return localVarFp
        .searchSubject(
          requestParameters.keyword,
          requestParameters.limit,
          requestParameters.highlightPostTag,
          requestParameters.highlightPreTag,
          options
        )
        .then((request) => request(axios, basePath));
    },
  };
};

/**
 * Request parameters for searchSubject operation in V1alpha1IndicesApi.
 * @export
 * @interface V1alpha1IndicesApiSearchSubjectRequest
 */
export interface V1alpha1IndicesApiSearchSubjectRequest {
  /**
   *
   * @type {string}
   * @memberof V1alpha1IndicesApiSearchSubject
   */
  readonly keyword: string;

  /**
   *
   * @type {number}
   * @memberof V1alpha1IndicesApiSearchSubject
   */
  readonly limit?: number;

  /**
   *
   * @type {string}
   * @memberof V1alpha1IndicesApiSearchSubject
   */
  readonly highlightPostTag?: string;

  /**
   *
   * @type {string}
   * @memberof V1alpha1IndicesApiSearchSubject
   */
  readonly highlightPreTag?: string;
}

/**
 * V1alpha1IndicesApi - object-oriented interface
 * @export
 * @class V1alpha1IndicesApi
 * @extends {BaseAPI}
 */
export class V1alpha1IndicesApi extends BaseAPI {
  /**
   * Build or rebuild subject indices for full text search
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof V1alpha1IndicesApi
   */
  public buildSubjectIndices(options?: AxiosRequestConfig) {
    return V1alpha1IndicesApiFp(this.configuration)
      .buildSubjectIndices(options)
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   * Search subjects with fuzzy query
   * @param {V1alpha1IndicesApiSearchSubjectRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof V1alpha1IndicesApi
   */
  public searchSubject(
    requestParameters: V1alpha1IndicesApiSearchSubjectRequest,
    options?: AxiosRequestConfig
  ) {
    return V1alpha1IndicesApiFp(this.configuration)
      .searchSubject(
        requestParameters.keyword,
        requestParameters.limit,
        requestParameters.highlightPostTag,
        requestParameters.highlightPreTag,
        options
      )
      .then((request) => request(this.axios, this.basePath));
  }
}
