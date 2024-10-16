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
import { CreateUserReqParams } from "../models";
// @ts-ignore
import { UpdateUserRequest } from "../models";
// @ts-ignore
import { User } from "../models";
/**
 * V1alpha1UserApi - axios parameter creator
 * @export
 */
export const V1alpha1UserApiAxiosParamCreator = function (
  configuration?: Configuration
) {
  return {
    /**
     * Change user role by username and roleId.
     * @param {string} username Username for user.
     * @param {number} roleId Id for role.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    changeUserRole: async (
      username: string,
      roleId: number,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'username' is not null or undefined
      assertParamExists("changeUserRole", "username", username);
      // verify required parameter 'roleId' is not null or undefined
      assertParamExists("changeUserRole", "roleId", roleId);
      const localVarPath = `/api/v1alpha1/user/{username}/role`.replace(
        `{${"username"}}`,
        encodeURIComponent(String(username))
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

      if (roleId !== undefined) {
        localVarQueryParameter["roleId"] = roleId;
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
     * Delete user by id..
     * @param {string} id
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    deleteById1: async (
      id: string,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'id' is not null or undefined
      assertParamExists("deleteById1", "id", id);
      const localVarPath = `/api/v1alpha1/user/id/{id}`.replace(
        `{${"id"}}`,
        encodeURIComponent(String(id))
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
     * Exist user by email.
     * @param {string} email
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    existUserByEmail: async (
      email: string,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'email' is not null or undefined
      assertParamExists("existUserByEmail", "email", email);
      const localVarPath = `/api/v1alpha1/user/email/exists/{email}`.replace(
        `{${"email"}}`,
        encodeURIComponent(String(email))
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
     * Exist user by username.
     * @param {string} username
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    existUserByUsername: async (
      username: string,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'username' is not null or undefined
      assertParamExists("existUserByUsername", "username", username);
      const localVarPath =
        `/api/v1alpha1/user/username/exists/{username}`.replace(
          `{${"username"}}`,
          encodeURIComponent(String(username))
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
     * Get all users.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    getUsers: async (
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      const localVarPath = `/api/v1alpha1/users`;
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
     * Create user.
     * @param {CreateUserReqParams} createUserReqParams User info.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    postUser: async (
      createUserReqParams: CreateUserReqParams,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'createUserReqParams' is not null or undefined
      assertParamExists("postUser", "createUserReqParams", createUserReqParams);
      const localVarPath = `/api/v1alpha1/user`;
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
        createUserReqParams,
        localVarRequestOptions,
        configuration
      );

      return {
        url: toPathString(localVarUrlObj),
        options: localVarRequestOptions,
      };
    },
    /**
     * Update user information.
     * @param {UpdateUserRequest} updateUserRequest User update info.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    updateUser: async (
      updateUserRequest: UpdateUserRequest,
      options: AxiosRequestConfig = {}
    ): Promise<RequestArgs> => {
      // verify required parameter 'updateUserRequest' is not null or undefined
      assertParamExists("updateUser", "updateUserRequest", updateUserRequest);
      const localVarPath = `/api/v1alpha1/user`;
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
        updateUserRequest,
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
 * V1alpha1UserApi - functional programming interface
 * @export
 */
export const V1alpha1UserApiFp = function (configuration?: Configuration) {
  const localVarAxiosParamCreator =
    V1alpha1UserApiAxiosParamCreator(configuration);
  return {
    /**
     * Change user role by username and roleId.
     * @param {string} username Username for user.
     * @param {number} roleId Id for role.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async changeUserRole(
      username: string,
      roleId: number,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<void>
    > {
      const localVarAxiosArgs = await localVarAxiosParamCreator.changeUserRole(
        username,
        roleId,
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
     * Delete user by id..
     * @param {string} id
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async deleteById1(
      id: string,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<void>
    > {
      const localVarAxiosArgs = await localVarAxiosParamCreator.deleteById1(
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
     * Exist user by email.
     * @param {string} email
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async existUserByEmail(
      email: string,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<boolean>
    > {
      const localVarAxiosArgs =
        await localVarAxiosParamCreator.existUserByEmail(email, options);
      return createRequestFunction(
        localVarAxiosArgs,
        globalAxios,
        BASE_PATH,
        configuration
      );
    },
    /**
     * Exist user by username.
     * @param {string} username
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async existUserByUsername(
      username: string,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<boolean>
    > {
      const localVarAxiosArgs =
        await localVarAxiosParamCreator.existUserByUsername(username, options);
      return createRequestFunction(
        localVarAxiosArgs,
        globalAxios,
        BASE_PATH,
        configuration
      );
    },
    /**
     * Get all users.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async getUsers(
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<Array<User>>
    > {
      const localVarAxiosArgs = await localVarAxiosParamCreator.getUsers(
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
     * Create user.
     * @param {CreateUserReqParams} createUserReqParams User info.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async postUser(
      createUserReqParams: CreateUserReqParams,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<User>
    > {
      const localVarAxiosArgs = await localVarAxiosParamCreator.postUser(
        createUserReqParams,
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
     * Update user information.
     * @param {UpdateUserRequest} updateUserRequest User update info.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async updateUser(
      updateUserRequest: UpdateUserRequest,
      options?: AxiosRequestConfig
    ): Promise<
      (axios?: AxiosInstance, basePath?: string) => AxiosPromise<User>
    > {
      const localVarAxiosArgs = await localVarAxiosParamCreator.updateUser(
        updateUserRequest,
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
 * V1alpha1UserApi - factory interface
 * @export
 */
export const V1alpha1UserApiFactory = function (
  configuration?: Configuration,
  basePath?: string,
  axios?: AxiosInstance
) {
  const localVarFp = V1alpha1UserApiFp(configuration);
  return {
    /**
     * Change user role by username and roleId.
     * @param {V1alpha1UserApiChangeUserRoleRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    changeUserRole(
      requestParameters: V1alpha1UserApiChangeUserRoleRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<void> {
      return localVarFp
        .changeUserRole(
          requestParameters.username,
          requestParameters.roleId,
          options
        )
        .then((request) => request(axios, basePath));
    },
    /**
     * Delete user by id..
     * @param {V1alpha1UserApiDeleteById1Request} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    deleteById1(
      requestParameters: V1alpha1UserApiDeleteById1Request,
      options?: AxiosRequestConfig
    ): AxiosPromise<void> {
      return localVarFp
        .deleteById1(requestParameters.id, options)
        .then((request) => request(axios, basePath));
    },
    /**
     * Exist user by email.
     * @param {V1alpha1UserApiExistUserByEmailRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    existUserByEmail(
      requestParameters: V1alpha1UserApiExistUserByEmailRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<boolean> {
      return localVarFp
        .existUserByEmail(requestParameters.email, options)
        .then((request) => request(axios, basePath));
    },
    /**
     * Exist user by username.
     * @param {V1alpha1UserApiExistUserByUsernameRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    existUserByUsername(
      requestParameters: V1alpha1UserApiExistUserByUsernameRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<boolean> {
      return localVarFp
        .existUserByUsername(requestParameters.username, options)
        .then((request) => request(axios, basePath));
    },
    /**
     * Get all users.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    getUsers(options?: AxiosRequestConfig): AxiosPromise<Array<User>> {
      return localVarFp
        .getUsers(options)
        .then((request) => request(axios, basePath));
    },
    /**
     * Create user.
     * @param {V1alpha1UserApiPostUserRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    postUser(
      requestParameters: V1alpha1UserApiPostUserRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<User> {
      return localVarFp
        .postUser(requestParameters.createUserReqParams, options)
        .then((request) => request(axios, basePath));
    },
    /**
     * Update user information.
     * @param {V1alpha1UserApiUpdateUserRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    updateUser(
      requestParameters: V1alpha1UserApiUpdateUserRequest,
      options?: AxiosRequestConfig
    ): AxiosPromise<User> {
      return localVarFp
        .updateUser(requestParameters.updateUserRequest, options)
        .then((request) => request(axios, basePath));
    },
  };
};

/**
 * Request parameters for changeUserRole operation in V1alpha1UserApi.
 * @export
 * @interface V1alpha1UserApiChangeUserRoleRequest
 */
export interface V1alpha1UserApiChangeUserRoleRequest {
  /**
   * Username for user.
   * @type {string}
   * @memberof V1alpha1UserApiChangeUserRole
   */
  readonly username: string;

  /**
   * Id for role.
   * @type {number}
   * @memberof V1alpha1UserApiChangeUserRole
   */
  readonly roleId: number;
}

/**
 * Request parameters for deleteById1 operation in V1alpha1UserApi.
 * @export
 * @interface V1alpha1UserApiDeleteById1Request
 */
export interface V1alpha1UserApiDeleteById1Request {
  /**
   *
   * @type {string}
   * @memberof V1alpha1UserApiDeleteById1
   */
  readonly id: string;
}

/**
 * Request parameters for existUserByEmail operation in V1alpha1UserApi.
 * @export
 * @interface V1alpha1UserApiExistUserByEmailRequest
 */
export interface V1alpha1UserApiExistUserByEmailRequest {
  /**
   *
   * @type {string}
   * @memberof V1alpha1UserApiExistUserByEmail
   */
  readonly email: string;
}

/**
 * Request parameters for existUserByUsername operation in V1alpha1UserApi.
 * @export
 * @interface V1alpha1UserApiExistUserByUsernameRequest
 */
export interface V1alpha1UserApiExistUserByUsernameRequest {
  /**
   *
   * @type {string}
   * @memberof V1alpha1UserApiExistUserByUsername
   */
  readonly username: string;
}

/**
 * Request parameters for postUser operation in V1alpha1UserApi.
 * @export
 * @interface V1alpha1UserApiPostUserRequest
 */
export interface V1alpha1UserApiPostUserRequest {
  /**
   * User info.
   * @type {CreateUserReqParams}
   * @memberof V1alpha1UserApiPostUser
   */
  readonly createUserReqParams: CreateUserReqParams;
}

/**
 * Request parameters for updateUser operation in V1alpha1UserApi.
 * @export
 * @interface V1alpha1UserApiUpdateUserRequest
 */
export interface V1alpha1UserApiUpdateUserRequest {
  /**
   * User update info.
   * @type {UpdateUserRequest}
   * @memberof V1alpha1UserApiUpdateUser
   */
  readonly updateUserRequest: UpdateUserRequest;
}

/**
 * V1alpha1UserApi - object-oriented interface
 * @export
 * @class V1alpha1UserApi
 * @extends {BaseAPI}
 */
export class V1alpha1UserApi extends BaseAPI {
  /**
   * Change user role by username and roleId.
   * @param {V1alpha1UserApiChangeUserRoleRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof V1alpha1UserApi
   */
  public changeUserRole(
    requestParameters: V1alpha1UserApiChangeUserRoleRequest,
    options?: AxiosRequestConfig
  ) {
    return V1alpha1UserApiFp(this.configuration)
      .changeUserRole(
        requestParameters.username,
        requestParameters.roleId,
        options
      )
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   * Delete user by id..
   * @param {V1alpha1UserApiDeleteById1Request} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof V1alpha1UserApi
   */
  public deleteById1(
    requestParameters: V1alpha1UserApiDeleteById1Request,
    options?: AxiosRequestConfig
  ) {
    return V1alpha1UserApiFp(this.configuration)
      .deleteById1(requestParameters.id, options)
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   * Exist user by email.
   * @param {V1alpha1UserApiExistUserByEmailRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof V1alpha1UserApi
   */
  public existUserByEmail(
    requestParameters: V1alpha1UserApiExistUserByEmailRequest,
    options?: AxiosRequestConfig
  ) {
    return V1alpha1UserApiFp(this.configuration)
      .existUserByEmail(requestParameters.email, options)
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   * Exist user by username.
   * @param {V1alpha1UserApiExistUserByUsernameRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof V1alpha1UserApi
   */
  public existUserByUsername(
    requestParameters: V1alpha1UserApiExistUserByUsernameRequest,
    options?: AxiosRequestConfig
  ) {
    return V1alpha1UserApiFp(this.configuration)
      .existUserByUsername(requestParameters.username, options)
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   * Get all users.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof V1alpha1UserApi
   */
  public getUsers(options?: AxiosRequestConfig) {
    return V1alpha1UserApiFp(this.configuration)
      .getUsers(options)
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   * Create user.
   * @param {V1alpha1UserApiPostUserRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof V1alpha1UserApi
   */
  public postUser(
    requestParameters: V1alpha1UserApiPostUserRequest,
    options?: AxiosRequestConfig
  ) {
    return V1alpha1UserApiFp(this.configuration)
      .postUser(requestParameters.createUserReqParams, options)
      .then((request) => request(this.axios, this.basePath));
  }

  /**
   * Update user information.
   * @param {V1alpha1UserApiUpdateUserRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof V1alpha1UserApi
   */
  public updateUser(
    requestParameters: V1alpha1UserApiUpdateUserRequest,
    options?: AxiosRequestConfig
  ) {
    return V1alpha1UserApiFp(this.configuration)
      .updateUser(requestParameters.updateUserRequest, options)
      .then((request) => request(this.axios, this.basePath));
  }
}
