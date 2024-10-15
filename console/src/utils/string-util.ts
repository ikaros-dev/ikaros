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


export const objectToMap = (obj: any, parentKey = ''): Map<string, any> => {
	const result = new Map<string, any>();
  
	for (const key in obj) {
	  if (Object.prototype.hasOwnProperty.call(obj, key)) {
		const newKey = parentKey ? `${parentKey}.${key}` : key;
  
		// 如果属性值是对象，递归处理
		if (typeof obj[key] === 'object' && !Array.isArray(obj[key]) && obj[key] !== null) {
		  const nestedMap = objectToMap(obj[key], newKey);
		  nestedMap.forEach((value, nestedKey) => {
			result.set(nestedKey, value); // 合并子Map到父Map
		  });
		} else {
		  result.set(newKey, obj[key]); // 终止条件：遇到非对象类型
		}
	  }
	}
  
	return result;
};

export const copyValue2Clipboard = async (val: string) => {
	if (navigator.clipboard && window.isSecureContext) {
		return navigator.clipboard.writeText(val);
	} else {
		const textArea = document.createElement('textarea');
		textArea.value = val;
		document.body.appendChild(textArea);
		textArea.focus();
		textArea.select();
		return new Promise((res, rej) => {
			document.execCommand('copy') ? res(val) : rej();
			textArea.remove();
		});
	}
};