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
import { SubjectRelation } from "../models";
/**
 * V1alpha1SubjectRelationApi - axios parameter creator
 * @export
 */
export const V1alpha1SubjectRelationApiAxiosParamCreator = function (
  configuration?: Configuration
) {
  return {
    /**
     * Create subject relation
     * @param {SubjectRelation} subjectRelation SubjectRelation
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    createSubjectRelation: async (
      subjectRelation: SubjectRelation,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'subjectRelation' is not null or undefined
      assertParamExists(
        "createSubjectRelation",
        "subjectRelation",
        subjectRelation
      );
      const localVarPath = `/api/v1alpha1/subject/relation`;
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

      localVarHeaderParameter["Content-Type"] = "application/json";

      setSearchParams(localVarUrlObj, localVarQueryParameter);
      let headersFromBaseOptions =
        baseOptions && baseOptions.headers ? baseOptions.headers : {};
      localVarRequestOptions.headers = {
        ...localVarHeaderParameter,
        ...headersFromBaseOptions,
        ...options.headers,
      };
      localVarRequestOptions.data = serializeDataIfNeeded(
        subjectRelation,
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
     * @param {number} subjectId Subject id
     * @param {'OTHER' | 'ANIME' | 'COMIC' | 'GAME' | 'MUSIC' | 'NOVEL' | 'REAL' | 'BEFORE' | 'AFTER' | 'SAME_WORLDVIEW' | 'ORIGINAL_SOUND_TRACK'} relationType Subject relation type
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    getSubjectRelationByIdAndType: async (
      subjectId: number,
      relationType:
        | "OTHER"
        | "ANIME"
        | "COMIC"
        | "GAME"
        | "MUSIC"
        | "NOVEL"
        | "REAL"
        | "BEFORE"
        | "AFTER"
        | "SAME_WORLDVIEW"
        | "ORIGINAL_SOUND_TRACK",
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'subjectId' is not null or undefined
      assertParamExists(
        "getSubjectRelationByIdAndType",
        "subjectId",
        subjectId
      );
      // verify required parameter 'relationType' is not null or undefined
      assertParamExists(
        "getSubjectRelationByIdAndType",
        "relationType",
        relationType
      );
      const localVarPath =
        `/api/v1alpha1/subject/relation/{subjectId}/{relationType}`
          .replace(`{${"subjectId"}}`, encodeURIComponent(String(subjectId)))
          .replace(
            `{${"relationType"}}`,
            encodeURIComponent(String(relationType))
          );
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
     *
     * @param {number} subjectId Subject id
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    getSubjectRelationsById: async (
      subjectId: number,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'subjectId' is not null or undefined
      assertParamExists("getSubjectRelationsById", "subjectId", subjectId);
      const localVarPath =
        `/api/v1alpha1/subject/relations/{subjectId}`.replace(
          `{${"subjectId"}}`,
          encodeURIComponent(String(subjectId))
        );
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
     * Remove subject relation
     * @param {number} subjectId Subject id
     * @param {'OTHER' | 'ANIME' | 'COMIC' | 'GAME' | 'MUSIC' | 'NOVEL' | 'REAL' | 'BEFORE' | 'AFTER' | 'SAME_WORLDVIEW' | 'ORIGINAL_SOUND_TRACK'} relationType Subject relation type code
     * @param {string} relationSubjects Relation subjects
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    removeSubjectRelation: async (
      subjectId: number,
      relationType:
        | "OTHER"
        | "ANIME"
        | "COMIC"
        | "GAME"
        | "MUSIC"
        | "NOVEL"
        | "REAL"
        | "BEFORE"
        | "AFTER"
        | "SAME_WORLDVIEW"
        | "ORIGINAL_SOUND_TRACK",
      relationSubjects: string,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'subjectId' is not null or undefined
      assertParamExists("removeSubjectRelation", "subjectId", subjectId);
      // verify required parameter 'relationType' is not null or undefined
      assertParamExists("removeSubjectRelation", "relationType", relationType);
      // verify required parameter 'relationSubjects' is not null or undefined
      assertParamExists(
        "removeSubjectRelation",
        "relationSubjects",
        relationSubjects
      );
      const localVarPath =
        `/api/v1alpha1/subject/relation/subjectId/{subjectId}`.replace(
          `{${"subjectId"}}`,
          encodeURIComponent(String(subjectId))
        );
      // use dummy base URL string because the URL constructor only accepts absolute URLs.
      const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
      let baseOptions;
      if (configuration) {
        baseOptions = configuration.baseOptions;
      }

      const localVarRequestOptions = {
        method: "DELETE",
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

      if (relationType !== undefined) {
        localVarQueryParameter["relation_type"] = relationType;
      }

      if (relationSubjects !== undefined) {
        localVarQueryParameter["relation_subjects"] = relationSubjects;
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
 * V1alpha1SubjectRelationApi - functional programming interface
 * @export
 */
export const V1alpha1SubjectRelationApiFp = function (
  configuration?: Configuration
) {
  const localVarAxiosParamCreator =
    V1alpha1SubjectRelationApiAxiosParamCreator(configuration);
  return {
    /**
     * Create subject relation
     * @param {SubjectRelation} subjectRelation SubjectRelation
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async createSubjectRelation(
      subjectRelation: SubjectRelation,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<void>
    > {
      const localVarAxiosArgs =
        await localVarAxiosParamCreator.createSubjectRelation(
          subjectRelation,
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
     * @param {number} subjectId Subject id
     * @param {'OTHER' | 'ANIME' | 'COMIC' | 'GAME' | 'MUSIC' | 'NOVEL' | 'REAL' | 'BEFORE' | 'AFTER' | 'SAME_WORLDVIEW' | 'ORIGINAL_SOUND_TRACK'} relationType Subject relation type
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async getSubjectRelationByIdAndType(
      subjectId: number,
      relationType:
        | "OTHER"
        | "ANIME"
        | "COMIC"
        | "GAME"
        | "MUSIC"
        | "NOVEL"
        | "REAL"
        | "BEFORE"
        | "AFTER"
        | "SAME_WORLDVIEW"
        | "ORIGINAL_SOUND_TRACK",
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<void>
    > {
      const localVarAxiosArgs =
        await localVarAxiosParamCreator.getSubjectRelationByIdAndType(
          subjectId,
          relationType,
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
     * @param {number} subjectId Subject id
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async getSubjectRelationsById(
      subjectId: number,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<void>
    > {
      const localVarAxiosArgs =
        await localVarAxiosParamCreator.getSubjectRelationsById(
          subjectId,
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
     * Remove subject relation
     * @param {number} subjectId Subject id
     * @param {'OTHER' | 'ANIME' | 'COMIC' | 'GAME' | 'MUSIC' | 'NOVEL' | 'REAL' | 'BEFORE' | 'AFTER' | 'SAME_WORLDVIEW' | 'ORIGINAL_SOUND_TRACK'} relationType Subject relation type code
     * @param {string} relationSubjects Relation subjects
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async removeSubjectRelation(
      subjectId: number,
      relationType:
        | "OTHER"
        | "ANIME"
        | "COMIC"
        | "GAME"
        | "MUSIC"
        | "NOVEL"
        | "REAL"
        | "BEFORE"
        | "AFTER"
        | "SAME_WORLDVIEW"
        | "ORIGINAL_SOUND_TRACK",
      relationSubjects: string,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<void>
    > {
      const localVarAxiosArgs =
        await localVarAxiosParamCreator.removeSubjectRelation(
          subjectId,
          relationType,
          relationSubjects,
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
 * V1alpha1SubjectRelationApi - factory interface
 * @export
 */
export const V1alpha1SubjectRelationApiFactory = function (
  configuration?: Configuration,
  basePath?: string,
  axios?: AxiosInstance
) {
  const localVarFp = V1alpha1SubjectRelationApiFp(configuration);
  return {
    /**
     * Create subject relation
     * @param {V1alpha1SubjectRelationApiCreateSubjectRelationRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    createSubjectRelation(
      requestParameters: V1alpha1SubjectRelationApiCreateSubjectRelationRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<void> {
      return localVarFp
        .createSubjectRelation(requestParameters.subjectRelation, options)
        .then((request) => request(axios, basePath));
    },
    /**
     *
     * @param {V1alpha1SubjectRelationApiGetSubjectRelationByIdAndTypeRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    getSubjectRelationByIdAndType(
      requestParameters: V1alpha1SubjectRelationApiGetSubjectRelationByIdAndTypeRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<void> {
      return localVarFp
        .getSubjectRelationByIdAndType(
          requestParameters.subjectId,
          requestParameters.relationType,
          options
        )
        .then((request) => request(axios, basePath));
    },
    /**
     *
     * @param {V1alpha1SubjectRelationApiGetSubjectRelationsByIdRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    getSubjectRelationsById(
      requestParameters: V1alpha1SubjectRelationApiGetSubjectRelationsByIdRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<void> {
      return localVarFp
        .getSubjectRelationsById(requestParameters.subjectId, options)
        .then((request) => request(axios, basePath));
    },
    /**
     * Remove subject relation
     * @param {V1alpha1SubjectRelationApiRemoveSubjectRelationRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    removeSubjectRelation(
      requestParameters: V1alpha1SubjectRelationApiRemoveSubjectRelationRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<void> {
      return localVarFp
        .removeSubjectRelation(
          requestParameters.subjectId,
          requestParameters.relationType,
          requestParameters.relationSubjects,
          options
        )
        .then((request) => request(axios, basePath));
    },
  };
};

/**
 * Request parameters for createSubjectRelation operation in V1alpha1SubjectRelationApi.
 * @export
 * @interface V1alpha1SubjectRelationApiCreateSubjectRelationRequest
 */
export interface V1alpha1SubjectRelationApiCreateSubjectRelationRequest {
  /**
   * SubjectRelation
   * @type {SubjectRelation}
   * @memberof V1alpha1SubjectRelationApiCreateSubjectRelation
   */
  readonly subjectRelation: SubjectRelation;
}

/**
 * Request parameters for getSubjectRelationByIdAndType operation in V1alpha1SubjectRelationApi.
 * @export
 * @interface V1alpha1SubjectRelationApiGetSubjectRelationByIdAndTypeRequest
 */
export interface V1alpha1SubjectRelationApiGetSubjectRelationByIdAndTypeRequest {
  /**
   * Subject id
   * @type {number}
   * @memberof V1alpha1SubjectRelationApiGetSubjectRelationByIdAndType
   */
  readonly subjectId: number;

  /**
   * Subject relation type
   * @type {'OTHER' | 'ANIME' | 'COMIC' | 'GAME' | 'MUSIC' | 'NOVEL' | 'REAL' | 'BEFORE' | 'AFTER' | 'SAME_WORLDVIEW' | 'ORIGINAL_SOUND_TRACK'}
   * @memberof V1alpha1SubjectRelationApiGetSubjectRelationByIdAndType
   */
  readonly relationType:
    | "OTHER"
    | "ANIME"
    | "COMIC"
    | "GAME"
    | "MUSIC"
    | "NOVEL"
    | "REAL"
    | "BEFORE"
    | "AFTER"
    | "SAME_WORLDVIEW"
    | "ORIGINAL_SOUND_TRACK";
}

/**
 * Request parameters for getSubjectRelationsById operation in V1alpha1SubjectRelationApi.
 * @export
 * @interface V1alpha1SubjectRelationApiGetSubjectRelationsByIdRequest
 */
export interface V1alpha1SubjectRelationApiGetSubjectRelationsByIdRequest {
  /**
   * Subject id
   * @type {number}
   * @memberof V1alpha1SubjectRelationApiGetSubjectRelationsById
   */
  readonly subjectId: number;
}

/**
 * Request parameters for removeSubjectRelation operation in V1alpha1SubjectRelationApi.
 * @export
 * @interface V1alpha1SubjectRelationApiRemoveSubjectRelationRequest
 */
export interface V1alpha1SubjectRelationApiRemoveSubjectRelationRequest {
  /**
   * Subject id
   * @type {number}
   * @memberof V1alpha1SubjectRelationApiRemoveSubjectRelation
   */
  readonly subjectId: number;

  /**
   * Subject relation type code
   * @type {'OTHER' | 'ANIME' | 'COMIC' | 'GAME' | 'MUSIC' | 'NOVEL' | 'REAL' | 'BEFORE' | 'AFTER' | 'SAME_WORLDVIEW' | 'ORIGINAL_SOUND_TRACK'}
   * @memberof V1alpha1SubjectRelationApiRemoveSubjectRelation
   */
  readonly relationType:
    | "OTHER"
    | "ANIME"
    | "COMIC"
    | "GAME"
    | "MUSIC"
    | "NOVEL"
    | "REAL"
    | "BEFORE"
    | "AFTER"
    | "SAME_WORLDVIEW"
    | "ORIGINAL_SOUND_TRACK";

  /**
   * Relation subjects
   * @type {string}
   * @memberof V1alpha1SubjectRelationApiRemoveSubjectRelation
   */
  readonly relationSubjects: string;
}

/**
 * V1alpha1SubjectRelationApi - object-oriented interface
 * @export
 * @class V1alpha1SubjectRelationApi
 * @extends {BaseAPI}
 */
export class V1alpha1SubjectRelationApi extends BaseAPI {
  /**
   * Create subject relation
   * @param {V1alpha1SubjectRelationApiCreateSubjectRelationRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof V1alpha1SubjectRelationApi
   */
  public createSubjectRelation(
    requestParameters: V1alpha1SubjectRelationApiCreateSubjectRelationRequest,
    options?: AxiosRequestConfig
  ) {
    return V1alpha1SubjectRelationApiFp(this.configuration)
      .createSubjectRelation(requestParameters.subjectRelation, options)
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   *
   * @param {V1alpha1SubjectRelationApiGetSubjectRelationByIdAndTypeRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof V1alpha1SubjectRelationApi
   */
  public getSubjectRelationByIdAndType(
    requestParameters: V1alpha1SubjectRelationApiGetSubjectRelationByIdAndTypeRequest,
    options?: AxiosRequestConfig
  ) {
    return V1alpha1SubjectRelationApiFp(this.configuration)
      .getSubjectRelationByIdAndType(
        requestParameters.subjectId,
        requestParameters.relationType,
        options
      )
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   *
   * @param {V1alpha1SubjectRelationApiGetSubjectRelationsByIdRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof V1alpha1SubjectRelationApi
   */
  public getSubjectRelationsById(
    requestParameters: V1alpha1SubjectRelationApiGetSubjectRelationsByIdRequest,
    options?: AxiosRequestConfig
  ) {
    return V1alpha1SubjectRelationApiFp(this.configuration)
      .getSubjectRelationsById(requestParameters.subjectId, options)
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   * Remove subject relation
   * @param {V1alpha1SubjectRelationApiRemoveSubjectRelationRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof V1alpha1SubjectRelationApi
   */
  public removeSubjectRelation(
    requestParameters: V1alpha1SubjectRelationApiRemoveSubjectRelationRequest,
    options?: AxiosRequestConfig
  ) {
    return V1alpha1SubjectRelationApiFp(this.configuration)
      .removeSubjectRelation(
        requestParameters.subjectId,
        requestParameters.relationType,
        requestParameters.relationSubjects,
        options
      )
      .then((request) => request(this.axios, this.basePath));
  }
}
