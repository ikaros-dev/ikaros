export const filePlaceMap = new Map([['LOCAL', '本地']]);

export const fileTypeMap = new Map([
	['IMAGE', '图片'],
	['VIDEO', '视频'],
	['DOCUMENT', '文档'],
	['VOICE', '音声'],
	['UNKNOWN', '未知'],
]);

export const taskNamePrefix = {
	fileRemote: {
		push: 'FilePush2RemoteTask-',
		pull: 'FilePull4RemoteTask-',
		delete: 'FileDeleteRemoteTask-',
	},
	folderRemote: {
		pull: 'FolderPull4RemoteTask-',
		push: 'FolderPush2RemoteTask-',
	},
};

export const episodeGroups = [
	'MAIN',
	'PROMOTION_VIDEO',
	'OPENING_SONG',
	'ENDING_SONG',
	'SPECIAL_PROMOTION',
	'SMALL_THEATER',
	'LIVE',
	'COMMERCIAL_MESSAGE',
	'ORIGINAL_SOUND_TRACK',
	'ORIGINAL_VIDEO_ANIMATION',
	'ORIGINAL_ANIMATION_DISC',
	'MUSIC_DIST1',
	'MUSIC_DIST2',
	'MUSIC_DIST3',
	'MUSIC_DIST4',
	'MUSIC_DIST5',
	'OTHER',
];

export const episodeGroupLabelMap = new Map([
	['MAIN', '正片(MAIN)'],
	['PROMOTION_VIDEO', '宣传(PV)'],
	['OPENING_SONG', '片头曲(OP)'],
	['ENDING_SONG', '片尾曲(ED)'],
	['SPECIAL_PROMOTION', '特典(SP)'],
	['SMALL_THEATER', '小剧场(ST)'],
	['LIVE', '直播(Live)'],
	['COMMERCIAL_MESSAGE', '广告(CM)'],
	['ORIGINAL_SOUND_TRACK', 'OST'],
	['ORIGINAL_VIDEO_ANIMATION', 'OVA'],
	['ORIGINAL_ANIMATION_DISC', 'OAD'],
	['OTHER', '其它(Other)'],
	['MUSIC_DIST1', '音乐列表一(MUSIC_DIST1)'],
	['MUSIC_DIST2', '音乐列表二(MUSIC_DIST2)'],
	['MUSIC_DIST3', '音乐列表三(MUSIC_DIST3)'],
	['MUSIC_DIST4', '音乐列表四(MUSIC_DIST4)'],
	['MUSIC_DIST5', '音乐列表五(MUSIC_DIST5)'],
]);

export const subjectTypes = [
	'ANIME',
	'COMIC',
	'GAME',
	'MUSIC',
	'NOVEL',
	'REAL',
	'OTHER',
];

export const subjectTypeAliasMap = new Map([
	['ANIME', '动漫'],
	['COMIC', '漫画'],
	['GAME', '游戏'],
	['MUSIC', '音声'],
	['NOVEL', '小说'],
	['REAL', '三次元'],
	['OTHER', '其它'],
]);

export const subjectCollectTypeAliasMap = new Map([
	['WISH', '想看'],
	['DOING', '在看'],
	['DONE', '看完'],
	['SHELVE', '搁置'],
	['DISCARD', '抛弃'],
]);

export const subtitleNameChineseMap = new Map([
	['SC', '简体中文'],
	['sc', '简体中文'],
	['TC', '繁体中文'],
	['tc', '繁体中文'],
	['JPSC', '简体中文'],
	['jpsc', '简体中文'],
	['JPTC', '繁体中文'],
	['jptc', '繁体中文'],
]);

export const subjectRelationTypes = [
	'OTHER',
	'ANIME',
	'COMIC',
	'GAME',
	'MUSIC',
	'NOVEL',
	'REAL',
	'BEFORE',
	'AFTER',
	'SAME_WORLDVIEW',
	'ORIGINAL_SOUND_TRACK',
	'ORIGINAL_VIDEO_ANIMATION',
	'ORIGINAL_ANIMATION_DISC',
];

export const scoreColors = ['#99A9BF', '#F7BA2A', '#FF9900'];
export const scoreTexts = [
	'不忍直视 1 (请谨慎评价)',
	'很差 2',
	'差 3',
	'较差 4',
	'不过不失 5',
	'还行 6',
	'推荐 7',
	'力荐 8',
	'神作 9',
	'超神作 10 (请谨慎评价)',
];

export const attachmentRootId = "019b715b-08c7-7509-ab14-2abe47f440f3";
