export const getCompleteFileUrl = (reactiveUrl: string | undefined): string => {
	const curPageUrl = window.location.href;
	const pathName = window.location.pathname;
	const localhostPath = curPageUrl.substring(0, curPageUrl.indexOf(pathName));
	return reactiveUrl?.startsWith('http')
		? reactiveUrl
		: localhostPath + reactiveUrl;
};
