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
