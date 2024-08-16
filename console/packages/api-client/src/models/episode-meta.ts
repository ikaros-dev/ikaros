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
 * @interface EpisodeMeta
 */
export interface EpisodeMeta {
  /**
   *
   * @type {number}
   * @memberof EpisodeMeta
   */
  id?: number;
  /**
   *
   * @type {string}
   * @memberof EpisodeMeta
   */
  name?: string;
  /**
   *
   * @type {string}
   * @memberof EpisodeMeta
   */
  description?: string;
  /**
   *
   * @type {number}
   * @memberof EpisodeMeta
   */
  sequence?: number;
  /**
   *
   * @type {string}
   * @memberof EpisodeMeta
   */
  group?: EpisodeMetaGroupEnum;
  /**
   *
   * @type {number}
   * @memberof EpisodeMeta
   */
  subject_id?: number;
  /**
   *
   * @type {string}
   * @memberof EpisodeMeta
   */
  name_cn?: string;
  /**
   *
   * @type {string}
   * @memberof EpisodeMeta
   */
  air_time?: string;
}

export const EpisodeMetaGroupEnum = {
  Main: "MAIN",
  PromotionVideo: "PROMOTION_VIDEO",
  OpeningSong: "OPENING_SONG",
  EndingSong: "ENDING_SONG",
  SpecialPromotion: "SPECIAL_PROMOTION",
  SmallTheater: "SMALL_THEATER",
  Live: "LIVE",
  CommercialMessage: "COMMERCIAL_MESSAGE",
  OriginalSoundTrack: "ORIGINAL_SOUND_TRACK",
  OriginalVideoAnimation: "ORIGINAL_VIDEO_ANIMATION",
  OriginalAnimationDisc: "ORIGINAL_ANIMATION_DISC",
  MusicDist1: "MUSIC_DIST1",
  MusicDist2: "MUSIC_DIST2",
  MusicDist3: "MUSIC_DIST3",
  MusicDist4: "MUSIC_DIST4",
  MusicDist5: "MUSIC_DIST5",
  Other: "OTHER",
} as const;

export type EpisodeMetaGroupEnum =
  (typeof EpisodeMetaGroupEnum)[keyof typeof EpisodeMetaGroupEnum];
