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
 * @interface AttachmentReference
 */
export interface AttachmentReference {
  /**
   *
   * @type {number}
   * @memberof AttachmentReference
   */
  id?: number;
  /**
   *
   * @type {string}
   * @memberof AttachmentReference
   */
  type?: AttachmentReferenceTypeEnum;
  /**
   *
   * @type {number}
   * @memberof AttachmentReference
   */
  attachmentId?: number;
  /**
   *
   * @type {number}
   * @memberof AttachmentReference
   */
  referenceId?: number;
}

export const AttachmentReferenceTypeEnum = {
  Subject: "SUBJECT",
  Episode: "EPISODE",
  UserAvatar: "USER_AVATAR",
} as const;

export type AttachmentReferenceTypeEnum =
  (typeof AttachmentReferenceTypeEnum)[keyof typeof AttachmentReferenceTypeEnum];
