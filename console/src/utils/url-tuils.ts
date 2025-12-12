export const getCompleteFileUrl = (reactiveUrl: string | undefined): string => {
	var curPageUrl = window.location.href;
	var pathName = window.location.pathname;
	var localhostPath = curPageUrl.substring(0, curPageUrl.indexOf(pathName));
	return reactiveUrl?.startsWith('http')
		? reactiveUrl
		: localhostPath + reactiveUrl;
};