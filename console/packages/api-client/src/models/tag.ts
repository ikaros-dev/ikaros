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

/**
 *
 * @export
 * @interface Tag
 */
export interface Tag {
	/**
	 *
	 * @type {number}
	 * @memberof Tag
	 */
	id?: number;
	/**
	 *
	 * @type {string}
	 * @memberof Tag
	 */
	type?: TagTypeEnum;
	/**
	 *
	 * @type {number}
	 * @memberof Tag
	 */
	masterId?: number;
	/**
	 *
	 * @type {string}
	 * @memberof Tag
	 */
	name?: string;
	/**
	 *
	 * @type {number}
	 * @memberof Tag
	 */
	userId?: number;
	/**
	 *
	 * @type {string}
	 * @memberof Tag
	 */
	createTime?: string;
}

export const TagTypeEnum = {
	Subject: 'SUBJECT',
	Episode: 'EPISODE',
} as const;

export type TagTypeEnum = (typeof TagTypeEnum)[keyof typeof TagTypeEnum];
