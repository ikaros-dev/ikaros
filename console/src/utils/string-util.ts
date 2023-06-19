import Utf8 from 'crypto-js/enc-utf8';
import Base64 from 'crypto-js/enc-base64';

export const base64Encode = (raw: string | undefined): string => {
	if (raw === undefined) {
		return '';
	}
	const word = Utf8.parse(raw);
	return Base64.stringify(word);
};
