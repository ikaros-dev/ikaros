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
import { PagingWrap } from "../models";
// @ts-ignore
import { Plugin } from "../models";
/**
 * PluginIkarosRunV1alpha1PluginApi - axios parameter creator
 * @export
 */
export const PluginIkarosRunV1alpha1PluginApiAxiosParamCreator = function (
  configuration?: Configuration
) {
  return {
    /**
     * Create plugin
     * @param {Plugin} [plugin] Fresh Plugin
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    createPlugin: async (
      plugin?: Plugin,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      const localVarPath = `/apis/plugin.ikaros.run/v1alpha1/plugin`;
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
        plugin,
        localVarRequestOptions,
        configuration
      );

      return {
        url: toPathString(localVarUrlObj),
        options: localVarRequestOptions,
      };
    },
    /**
     * Delete plugin
     * @param {string} name Name of Plugin
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    deletePlugin: async (
      name: string,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'name' is not null or undefined
      assertParamExists("deletePlugin", "name", name);
      const localVarPath =
        `/apis/plugin.ikaros.run/v1alpha1/plugin/{name}`.replace(
          `{${"name"}}`,
          encodeURIComponent(String(name))
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
     * Get plugin By Name.
     * @param {string} name Name of Plugin
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    getPlugin: async (
      name: string,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'name' is not null or undefined
      assertParamExists("getPlugin", "name", name);
      const localVarPath =
        `/apis/plugin.ikaros.run/v1alpha1/plugin/{name}`.replace(
          `{${"name"}}`,
          encodeURIComponent(String(name))
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
     * Get plugin meta value by name and metaName.
     * @param {string} name Name of Plugin
     * @param {string} metaName MetaName of Plugin
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    getPluginMeta: async (
      name: string,
      metaName: string,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'name' is not null or undefined
      assertParamExists("getPluginMeta", "name", name);
      // verify required parameter 'metaName' is not null or undefined
      assertParamExists("getPluginMeta", "metaName", metaName);
      const localVarPath =
        `/apis/plugin.ikaros.run/v1alpha1/plugin/{name}/{metaName}`
          .replace(`{${"name"}}`, encodeURIComponent(String(name)))
          .replace(`{${"metaName"}}`, encodeURIComponent(String(metaName)));
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
     * Get plugins by paging.
     * @param {string} page Page of Plugin
     * @param {string} size Size ofPlugin
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    getPluginsByPaging: async (
      page: string,
      size: string,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'page' is not null or undefined
      assertParamExists("getPluginsByPaging", "page", page);
      // verify required parameter 'size' is not null or undefined
      assertParamExists("getPluginsByPaging", "size", size);
      const localVarPath =
        `/apis/plugin.ikaros.run/v1alpha1/plugins/{page}/{size}`
          .replace(`{${"page"}}`, encodeURIComponent(String(page)))
          .replace(`{${"size"}}`, encodeURIComponent(String(size)));
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
     * List plugins
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    listPlugins: async (
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      const localVarPath = `/apis/plugin.ikaros.run/v1alpha1/plugins`;
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
     * Update plugin
     * @param {string} name Name of plugin
     * @param {Plugin} [plugin] Updated Plugin
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    updatePlugin: async (
      name: string,
      plugin?: Plugin,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'name' is not null or undefined
      assertParamExists("updatePlugin", "name", name);
      const localVarPath = `/apis/plugin.ikaros.run/v1alpha1/plugin`.replace(
        `{${"name"}}`,
        encodeURIComponent(String(name))
      );
      // use dummy base URL string because the URL constructor only accepts absolute URLs.
      const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
      let baseOptions;
      if (configuration) {
        baseOptions = configuration.baseOptions;
      }

      const localVarRequestOptions = {
        method: "PUT",
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
        plugin,
        localVarRequestOptions,
        configuration
      );

      return {
        url: toPathString(localVarUrlObj),
        options: localVarRequestOptions,
      };
    },
    /**
     * Update plugin metadata value.
     * @param {string} name Name of plugin
     * @param {string} metaName MetaName of plugin
     * @param {string} body Updated Plugin Metadata value. current request body receive data type is byte[].class, If you specific data type is a String.class, must to add English double quotation marks.  correct is: \&quot;new value\&quot;.  incorrect is: new value.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    updatePluginMeta: async (
      name: string,
      metaName: string,
      body: string,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'name' is not null or undefined
      assertParamExists("updatePluginMeta", "name", name);
      // verify required parameter 'metaName' is not null or undefined
      assertParamExists("updatePluginMeta", "metaName", metaName);
      // verify required parameter 'body' is not null or undefined
      assertParamExists("updatePluginMeta", "body", body);
      const localVarPath =
        `/apis/plugin.ikaros.run/v1alpha1/plugin/{name}/{metaName}`
          .replace(`{${"name"}}`, encodeURIComponent(String(name)))
          .replace(`{${"metaName"}}`, encodeURIComponent(String(metaName)));
      // use dummy base URL string because the URL constructor only accepts absolute URLs.
      const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
      let baseOptions;
      if (configuration) {
        baseOptions = configuration.baseOptions;
      }

      const localVarRequestOptions = {
        method: "PUT",
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
        body,
        localVarRequestOptions,
        configuration
      );

      return {
        url: toPathString(localVarUrlObj),
        options: localVarRequestOptions,
      };
    },
  };
};

/**
 * PluginIkarosRunV1alpha1PluginApi - functional programming interface
 * @export
 */
export const PluginIkarosRunV1alpha1PluginApiFp = function (
  configuration?: Configuration
) {
  const localVarAxiosParamCreator =
    PluginIkarosRunV1alpha1PluginApiAxiosParamCreator(configuration);
  return {
    /**
     * Create plugin
     * @param {Plugin} [plugin] Fresh Plugin
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async createPlugin(
      plugin?: Plugin,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<Plugin>
    > {
      const localVarAxiosArgs = await localVarAxiosParamCreator.createPlugin(
        plugin,
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
     * Delete plugin
     * @param {string} name Name of Plugin
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async deletePlugin(
      name: string,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<void>
    > {
      const localVarAxiosArgs = await localVarAxiosParamCreator.deletePlugin(
        name,
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
     * Get plugin By Name.
     * @param {string} name Name of Plugin
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async getPlugin(
      name: string,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<Plugin>
    > {
      const localVarAxiosArgs = await localVarAxiosParamCreator.getPlugin(
        name,
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
     * Get plugin meta value by name and metaName.
     * @param {string} name Name of Plugin
     * @param {string} metaName MetaName of Plugin
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async getPluginMeta(
      name: string,
      metaName: string,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<void>
    > {
      const localVarAxiosArgs = await localVarAxiosParamCreator.getPluginMeta(
        name,
        metaName,
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
     * Get plugins by paging.
     * @param {string} page Page of Plugin
     * @param {string} size Size ofPlugin
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async getPluginsByPaging(
      page: string,
      size: string,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<PagingWrap>
    > {
      const localVarAxiosArgs =
        await localVarAxiosParamCreator.getPluginsByPaging(page, size, options);
      return createRequestFunction(
        localVarAxiosArgs,
        globalAxios,
        BASE_PATH,
        configuration
      );
    },
    /**
     * List plugins
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async listPlugins(
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<Plugin>
    > {
      const localVarAxiosArgs = await localVarAxiosParamCreator.listPlugins(
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
     * Update plugin
     * @param {string} name Name of plugin
     * @param {Plugin} [plugin] Updated Plugin
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async updatePlugin(
      name: string,
      plugin?: Plugin,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<Plugin>
    > {
      const localVarAxiosArgs = await localVarAxiosParamCreator.updatePlugin(
        name,
        plugin,
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
     * Update plugin metadata value.
     * @param {string} name Name of plugin
     * @param {string} metaName MetaName of plugin
     * @param {string} body Updated Plugin Metadata value. current request body receive data type is byte[].class, If you specific data type is a String.class, must to add English double quotation marks.  correct is: \&quot;new value\&quot;.  incorrect is: new value.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async updatePluginMeta(
      name: string,
      metaName: string,
      body: string,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<Plugin>
    > {
      const localVarAxiosArgs =
        await localVarAxiosParamCreator.updatePluginMeta(
          name,
          metaName,
          body,
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
 * PluginIkarosRunV1alpha1PluginApi - factory interface
 * @export
 */
export const PluginIkarosRunV1alpha1PluginApiFactory = function (
  configuration?: Configuration,
  basePath?: string,
  axios?: AxiosInstance
) {
  const localVarFp = PluginIkarosRunV1alpha1PluginApiFp(configuration);
  return {
    /**
     * Create plugin
     * @param {PluginIkarosRunV1alpha1PluginApiCreatePluginRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    createPlugin(
      requestParameters: PluginIkarosRunV1alpha1PluginApiCreatePluginRequest = {},
      options?: AxiosRequestConfig
    ): AxiosPromise<Plugin> {
      return localVarFp
        .createPlugin(requestParameters.plugin, options)
        .then((request) => request(axios, basePath));
    },
    /**
     * Delete plugin
     * @param {PluginIkarosRunV1alpha1PluginApiDeletePluginRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    deletePlugin(
      requestParameters: PluginIkarosRunV1alpha1PluginApiDeletePluginRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<void> {
      return localVarFp
        .deletePlugin(requestParameters.name, options)
        .then((request) => request(axios, basePath));
    },
    /**
     * Get plugin By Name.
     * @param {PluginIkarosRunV1alpha1PluginApiGetPluginRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    getPlugin(
      requestParameters: PluginIkarosRunV1alpha1PluginApiGetPluginRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<Plugin> {
      return localVarFp
        .getPlugin(requestParameters.name, options)
        .then((request) => request(axios, basePath));
    },
    /**
     * Get plugin meta value by name and metaName.
     * @param {PluginIkarosRunV1alpha1PluginApiGetPluginMetaRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    getPluginMeta(
      requestParameters: PluginIkarosRunV1alpha1PluginApiGetPluginMetaRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<void> {
      return localVarFp
        .getPluginMeta(
          requestParameters.name,
          requestParameters.metaName,
          options
        )
        .then((request) => request(axios, basePath));
    },
    /**
     * Get plugins by paging.
     * @param {PluginIkarosRunV1alpha1PluginApiGetPluginsByPagingRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    getPluginsByPaging(
      requestParameters: PluginIkarosRunV1alpha1PluginApiGetPluginsByPagingRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<PagingWrap> {
      return localVarFp
        .getPluginsByPaging(
          requestParameters.page,
          requestParameters.size,
          options
        )
        .then((request) => request(axios, basePath));
    },
    /**
     * List plugins
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    listPlugins(options?: AxiosRequestConfig): AxiosPromise<Plugin> {
      return localVarFp
        .listPlugins(options)
        .then((request) => request(axios, basePath));
    },
    /**
     * Update plugin
     * @param {PluginIkarosRunV1alpha1PluginApiUpdatePluginRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    updatePlugin(
      requestParameters: PluginIkarosRunV1alpha1PluginApiUpdatePluginRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<Plugin> {
      return localVarFp
        .updatePlugin(requestParameters.name, requestParameters.plugin, options)
        .then((request) => request(axios, basePath));
    },
    /**
     * Update plugin metadata value.
     * @param {PluginIkarosRunV1alpha1PluginApiUpdatePluginMetaRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    updatePluginMeta(
      requestParameters: PluginIkarosRunV1alpha1PluginApiUpdatePluginMetaRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<Plugin> {
      return localVarFp
        .updatePluginMeta(
          requestParameters.name,
          requestParameters.metaName,
          requestParameters.body,
          options
        )
        .then((request) => request(axios, basePath));
    },
  };
};

/**
 * Request parameters for createPlugin operation in PluginIkarosRunV1alpha1PluginApi.
 * @export
 * @interface PluginIkarosRunV1alpha1PluginApiCreatePluginRequest
 */
export interface PluginIkarosRunV1alpha1PluginApiCreatePluginRequest {
  /**
   * Fresh Plugin
   * @type {Plugin}
   * @memberof PluginIkarosRunV1alpha1PluginApiCreatePlugin
   */
  readonly plugin?: Plugin;
}

/**
 * Request parameters for deletePlugin operation in PluginIkarosRunV1alpha1PluginApi.
 * @export
 * @interface PluginIkarosRunV1alpha1PluginApiDeletePluginRequest
 */
export interface PluginIkarosRunV1alpha1PluginApiDeletePluginRequest {
  /**
   * Name of Plugin
   * @type {string}
   * @memberof PluginIkarosRunV1alpha1PluginApiDeletePlugin
   */
  readonly name: string;
}

/**
 * Request parameters for getPlugin operation in PluginIkarosRunV1alpha1PluginApi.
 * @export
 * @interface PluginIkarosRunV1alpha1PluginApiGetPluginRequest
 */
export interface PluginIkarosRunV1alpha1PluginApiGetPluginRequest {
  /**
   * Name of Plugin
   * @type {string}
   * @memberof PluginIkarosRunV1alpha1PluginApiGetPlugin
   */
  readonly name: string;
}

/**
 * Request parameters for getPluginMeta operation in PluginIkarosRunV1alpha1PluginApi.
 * @export
 * @interface PluginIkarosRunV1alpha1PluginApiGetPluginMetaRequest
 */
export interface PluginIkarosRunV1alpha1PluginApiGetPluginMetaRequest {
  /**
   * Name of Plugin
   * @type {string}
   * @memberof PluginIkarosRunV1alpha1PluginApiGetPluginMeta
   */
  readonly name: string;

  /**
   * MetaName of Plugin
   * @type {string}
   * @memberof PluginIkarosRunV1alpha1PluginApiGetPluginMeta
   */
  readonly metaName: string;
}

/**
 * Request parameters for getPluginsByPaging operation in PluginIkarosRunV1alpha1PluginApi.
 * @export
 * @interface PluginIkarosRunV1alpha1PluginApiGetPluginsByPagingRequest
 */
export interface PluginIkarosRunV1alpha1PluginApiGetPluginsByPagingRequest {
  /**
   * Page of Plugin
   * @type {string}
   * @memberof PluginIkarosRunV1alpha1PluginApiGetPluginsByPaging
   */
  readonly page: string;

  /**
   * Size ofPlugin
   * @type {string}
   * @memberof PluginIkarosRunV1alpha1PluginApiGetPluginsByPaging
   */
  readonly size: string;
}

/**
 * Request parameters for updatePlugin operation in PluginIkarosRunV1alpha1PluginApi.
 * @export
 * @interface PluginIkarosRunV1alpha1PluginApiUpdatePluginRequest
 */
export interface PluginIkarosRunV1alpha1PluginApiUpdatePluginRequest {
  /**
   * Name of plugin
   * @type {string}
   * @memberof PluginIkarosRunV1alpha1PluginApiUpdatePlugin
   */
  readonly name: string;

  /**
   * Updated Plugin
   * @type {Plugin}
   * @memberof PluginIkarosRunV1alpha1PluginApiUpdatePlugin
   */
  readonly plugin?: Plugin;
}

/**
 * Request parameters for updatePluginMeta operation in PluginIkarosRunV1alpha1PluginApi.
 * @export
 * @interface PluginIkarosRunV1alpha1PluginApiUpdatePluginMetaRequest
 */
export interface PluginIkarosRunV1alpha1PluginApiUpdatePluginMetaRequest {
  /**
   * Name of plugin
   * @type {string}
   * @memberof PluginIkarosRunV1alpha1PluginApiUpdatePluginMeta
   */
  readonly name: string;

  /**
   * MetaName of plugin
   * @type {string}
   * @memberof PluginIkarosRunV1alpha1PluginApiUpdatePluginMeta
   */
  readonly metaName: string;

  /**
   * Updated Plugin Metadata value. current request body receive data type is byte[].class, If you specific data type is a String.class, must to add English double quotation marks.  correct is: \&quot;new value\&quot;.  incorrect is: new value.
   * @type {string}
   * @memberof PluginIkarosRunV1alpha1PluginApiUpdatePluginMeta
   */
  readonly body: string;
}

/**
 * PluginIkarosRunV1alpha1PluginApi - object-oriented interface
 * @export
 * @class PluginIkarosRunV1alpha1PluginApi
 * @extends {BaseAPI}
 */
export class PluginIkarosRunV1alpha1PluginApi extends BaseAPI {
  /**
   * Create plugin
   * @param {PluginIkarosRunV1alpha1PluginApiCreatePluginRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof PluginIkarosRunV1alpha1PluginApi
   */
  public createPlugin(
    requestParameters: PluginIkarosRunV1alpha1PluginApiCreatePluginRequest = {},
    options?: AxiosRequestConfig
  ) {
    return PluginIkarosRunV1alpha1PluginApiFp(this.configuration)
      .createPlugin(requestParameters.plugin, options)
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   * Delete plugin
   * @param {PluginIkarosRunV1alpha1PluginApiDeletePluginRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof PluginIkarosRunV1alpha1PluginApi
   */
  public deletePlugin(
    requestParameters: PluginIkarosRunV1alpha1PluginApiDeletePluginRequest,
    options?: AxiosRequestConfig
  ) {
    return PluginIkarosRunV1alpha1PluginApiFp(this.configuration)
      .deletePlugin(requestParameters.name, options)
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   * Get plugin By Name.
   * @param {PluginIkarosRunV1alpha1PluginApiGetPluginRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof PluginIkarosRunV1alpha1PluginApi
   */
  public getPlugin(
    requestParameters: PluginIkarosRunV1alpha1PluginApiGetPluginRequest,
    options?: AxiosRequestConfig
  ) {
    return PluginIkarosRunV1alpha1PluginApiFp(this.configuration)
      .getPlugin(requestParameters.name, options)
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   * Get plugin meta value by name and metaName.
   * @param {PluginIkarosRunV1alpha1PluginApiGetPluginMetaRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof PluginIkarosRunV1alpha1PluginApi
   */
  public getPluginMeta(
    requestParameters: PluginIkarosRunV1alpha1PluginApiGetPluginMetaRequest,
    options?: AxiosRequestConfig
  ) {
    return PluginIkarosRunV1alpha1PluginApiFp(this.configuration)
      .getPluginMeta(
        requestParameters.name,
        requestParameters.metaName,
        options
      )
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   * Get plugins by paging.
   * @param {PluginIkarosRunV1alpha1PluginApiGetPluginsByPagingRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof PluginIkarosRunV1alpha1PluginApi
   */
  public getPluginsByPaging(
    requestParameters: PluginIkarosRunV1alpha1PluginApiGetPluginsByPagingRequest,
    options?: AxiosRequestConfig
  ) {
    return PluginIkarosRunV1alpha1PluginApiFp(this.configuration)
      .getPluginsByPaging(
        requestParameters.page,
        requestParameters.size,
        options
      )
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   * List plugins
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof PluginIkarosRunV1alpha1PluginApi
   */
  public listPlugins(options?: AxiosRequestConfig) {
    return PluginIkarosRunV1alpha1PluginApiFp(this.configuration)
      .listPlugins(options)
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   * Update plugin
   * @param {PluginIkarosRunV1alpha1PluginApiUpdatePluginRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof PluginIkarosRunV1alpha1PluginApi
   */
  public updatePlugin(
    requestParameters: PluginIkarosRunV1alpha1PluginApiUpdatePluginRequest,
    options?: AxiosRequestConfig
  ) {
    return PluginIkarosRunV1alpha1PluginApiFp(this.configuration)
      .updatePlugin(requestParameters.name, requestParameters.plugin, options)
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   * Update plugin metadata value.
   * @param {PluginIkarosRunV1alpha1PluginApiUpdatePluginMetaRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof PluginIkarosRunV1alpha1PluginApi
   */
  public updatePluginMeta(
    requestParameters: PluginIkarosRunV1alpha1PluginApiUpdatePluginMetaRequest,
    options?: AxiosRequestConfig
  ) {
    return PluginIkarosRunV1alpha1PluginApiFp(this.configuration)
      .updatePluginMeta(
        requestParameters.name,
        requestParameters.metaName,
        requestParameters.body,
        options
      )
      .then((request) => request(this.axios, this.basePath));
  }
}
