const POSTFIX_IMAGES = ['jpg', 'jpeg', 'png', 'gif', 'webp'];

const POSTFIX_VIDEO = [
	'mkv',
	'mp4',
	'avi',
	'flv',
	'f4v',
	'webm',
	'm4v',
	'mov',
	'3gp',
	'3g2',
	'rm',
	'rmvb',
	'wmv',
	'asf',
	'mpg',
	'mpeg',
	'mpe',
	'ts',
	'div',
	'dv',
	'divx',
	'vob',
	'dat',
	'lavf',
	'cpk',
	'dirac',
	'ram',
	'qt',
	'fli',
	'flc',
	'mod',
	'mpg',
	'mlv',
	'mpe',
	'mpeg',
	'm3u8',
];

const POSTFIX_DOCUMENTS = ['txt', 'doc', 'docx', 'ppt', 'xlsx', 'pptx', 'ass'];

const POSTFIX_VOICES = ['mp3', 'wma', 'wav', 'ape', 'flac', 'ogg', 'aac'];

export function getPostfix(name: string): string {
	if (!name) {
		return '';
	}
	return name.replace(/.+\./, '');
}

export function isImage(name: string): boolean {
	return POSTFIX_IMAGES.indexOf(getPostfix(name)) !== -1;
}
export function isVideo(name: string): boolean {
	return POSTFIX_VIDEO.indexOf(getPostfix(name)) !== -1;
}
export function isVoice(name: string): boolean {
	return POSTFIX_VOICES.indexOf(getPostfix(name)) !== -1;
}

export function isDoucment(name: string): boolean {
	return POSTFIX_DOCUMENTS.indexOf(getPostfix(name)) !== -1;
}
