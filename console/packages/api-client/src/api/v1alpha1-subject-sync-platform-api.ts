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
import { Subject } from "../models";
/**
 * V1alpha1SubjectSyncPlatformApi - axios parameter creator
 * @export
 */
export const V1alpha1SubjectSyncPlatformApiAxiosParamCreator = function (
  configuration?: Configuration
) {
  return {
    /**
     * Sync subject and platform by platform name and platform id, create subject when params not contain subject id, update exists subject when params contain subject id.
     * @param {'BGM_TV' | 'TMDB' | 'AniDB' | 'TVDB' | 'VNDB' | 'DOU_BAN' | 'OTHER'} platform Platform.
     * @param {string} platformId Platform id
     * @param {number} [subjectId] Subject id.
     * @param {'PULL' | 'MERGE'} [action] Sync action, such as PULL or MERGE, default is PULL PULL will override all subject meta info, MERGE will update meta info that absent.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    syncSubjectAndPlatform: async (
      platform:
        | "BGM_TV"
        | "TMDB"
        | "AniDB"
        | "TVDB"
        | "VNDB"
        | "DOU_BAN"
        | "OTHER",
      platformId: string,
      subjectId?: number,
      action?: "PULL" | "MERGE",
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'platform' is not null or undefined
      assertParamExists("syncSubjectAndPlatform", "platform", platform);
      // verify required parameter 'platformId' is not null or undefined
      assertParamExists("syncSubjectAndPlatform", "platformId", platformId);
      const localVarPath = `/api/v1alpha1/subject/sync/platform`;
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

      if (subjectId !== undefined) {
        localVarQueryParameter["subjectId"] = subjectId;
      }

      if (platform !== undefined) {
        localVarQueryParameter["platform"] = platform;
      }

      if (platformId !== undefined) {
        localVarQueryParameter["platformId"] = platformId;
      }

      if (action !== undefined) {
        localVarQueryParameter["action"] = action;
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
 * V1alpha1SubjectSyncPlatformApi - functional programming interface
 * @export
 */
export const V1alpha1SubjectSyncPlatformApiFp = function (
  configuration?: Configuration
) {
  const localVarAxiosParamCreator =
    V1alpha1SubjectSyncPlatformApiAxiosParamCreator(configuration);
  return {
    /**
     * Sync subject and platform by platform name and platform id, create subject when params not contain subject id, update exists subject when params contain subject id.
     * @param {'BGM_TV' | 'TMDB' | 'AniDB' | 'TVDB' | 'VNDB' | 'DOU_BAN' | 'OTHER'} platform Platform.
     * @param {string} platformId Platform id
     * @param {number} [subjectId] Subject id.
     * @param {'PULL' | 'MERGE'} [action] Sync action, such as PULL or MERGE, default is PULL PULL will override all subject meta info, MERGE will update meta info that absent.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async syncSubjectAndPlatform(
      platform:
        | "BGM_TV"
        | "TMDB"
        | "AniDB"
        | "TVDB"
        | "VNDB"
        | "DOU_BAN"
        | "OTHER",
      platformId: string,
      subjectId?: number,
      action?: "PULL" | "MERGE",
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<Subject>
    > {
      const localVarAxiosArgs =
        await localVarAxiosParamCreator.syncSubjectAndPlatform(
          platform,
          platformId,
          subjectId,
          action,
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
 * V1alpha1SubjectSyncPlatformApi - factory interface
 * @export
 */
export const V1alpha1SubjectSyncPlatformApiFactory = function (
  configuration?: Configuration,
  basePath?: string,
  axios?: AxiosInstance
) {
  const localVarFp = V1alpha1SubjectSyncPlatformApiFp(configuration);
  return {
    /**
     * Sync subject and platform by platform name and platform id, create subject when params not contain subject id, update exists subject when params contain subject id.
     * @param {V1alpha1SubjectSyncPlatformApiSyncSubjectAndPlatformRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    syncSubjectAndPlatform(
      requestParameters: V1alpha1SubjectSyncPlatformApiSyncSubjectAndPlatformRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<Subject> {
      return localVarFp
        .syncSubjectAndPlatform(
          requestParameters.platform,
          requestParameters.platformId,
          requestParameters.subjectId,
          requestParameters.action,
          options
        )
        .then((request) => request(axios, basePath));
    },
  };
};

/**
 * Request parameters for syncSubjectAndPlatform operation in V1alpha1SubjectSyncPlatformApi.
 * @export
 * @interface V1alpha1SubjectSyncPlatformApiSyncSubjectAndPlatformRequest
 */
export interface V1alpha1SubjectSyncPlatformApiSyncSubjectAndPlatformRequest {
  /**
   * Platform.
   * @type {'BGM_TV' | 'TMDB' | 'AniDB' | 'TVDB' | 'VNDB' | 'DOU_BAN' | 'OTHER'}
   * @memberof V1alpha1SubjectSyncPlatformApiSyncSubjectAndPlatform
   */
  readonly platform:
    | "BGM_TV"
    | "TMDB"
    | "AniDB"
    | "TVDB"
    | "VNDB"
    | "DOU_BAN"
    | "OTHER";

  /**
   * Platform id
   * @type {string}
   * @memberof V1alpha1SubjectSyncPlatformApiSyncSubjectAndPlatform
   */
  readonly platformId: string;

  /**
   * Subject id.
   * @type {number}
   * @memberof V1alpha1SubjectSyncPlatformApiSyncSubjectAndPlatform
   */
  readonly subjectId?: number;

  /**
   * Sync action, such as PULL or MERGE, default is PULL PULL will override all subject meta info, MERGE will update meta info that absent.
   * @type {'PULL' | 'MERGE'}
   * @memberof V1alpha1SubjectSyncPlatformApiSyncSubjectAndPlatform
   */
  readonly action?: "PULL" | "MERGE";
}

/**
 * V1alpha1SubjectSyncPlatformApi - object-oriented interface
 * @export
 * @class V1alpha1SubjectSyncPlatformApi
 * @extends {BaseAPI}
 */
export class V1alpha1SubjectSyncPlatformApi extends BaseAPI {
  /**
   * Sync subject and platform by platform name and platform id, create subject when params not contain subject id, update exists subject when params contain subject id.
   * @param {V1alpha1SubjectSyncPlatformApiSyncSubjectAndPlatformRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof V1alpha1SubjectSyncPlatformApi
   */
  public syncSubjectAndPlatform(
    requestParameters: V1alpha1SubjectSyncPlatformApiSyncSubjectAndPlatformRequest,
    options?: AxiosRequestConfig
  ) {
    return V1alpha1SubjectSyncPlatformApiFp(this.configuration)
      .syncSubjectAndPlatform(
        requestParameters.platform,
        requestParameters.platformId,
        requestParameters.subjectId,
        requestParameters.action,
        options
      )
      .then((request) => request(this.axios, this.basePath));
  }
}
