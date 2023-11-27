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
	'COMMERCIAL_MESSAGE',
	'LIVE',
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
	['COMMERCIAL_MESSAGE', '广告(CM)'],
	['LIVE', '直播(Live)'],
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
	['tc', '简体中文'],
]);
