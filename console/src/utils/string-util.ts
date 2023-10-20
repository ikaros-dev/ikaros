import Utf8 from 'crypto-js/enc-utf8';
import Base64 from 'crypto-js/enc-base64';

export const base64Encode = (raw: string | undefined): string => {
	if (raw === undefined) {
		return '';
	}
	const word = Utf8.parse(raw);
	return Base64.stringify(word);
};

export const base64Decode = (base64: string | undefined): string => {
	if (base64 == undefined) {
		return '';
	}
	const word = Base64.parse(base64);
	return Utf8.stringify(word);
};

export const formatFileSize = (value): string => {
	if (!value) {
		return '0 Bytes';
	}
	const unitArr = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
	const srcsize = parseFloat(value);
	const index = Math.floor(Math.log(srcsize) / Math.log(1024));
	let size = srcsize / Math.pow(1024, index);
	// @ts-ignore
	size = size.toFixed(2);
	return size + ' ' + unitArr[index];
};
