export const getCompleteFileUrl = (reactiveUrl: string | undefined): string => {
	const curPageUrl = window.location.href;
	const pathName = window.location.pathname;
	const localhostPath = curPageUrl.substring(0, curPageUrl.indexOf(pathName));
	return reactiveUrl?.startsWith('http')
		? reactiveUrl
		: localhostPath + reactiveUrl;
};

/**
 * 为本地流接口路径追加 redirect=true 参数，使对象存储等外部直链场景
 * 返回 307 重定向，避免流量经过服务端代理。已为 http 直链或已带该参数时不做处理。
 */
export const appendRedirectParam = (reactiveUrl: string | undefined): string => {
	if (!reactiveUrl || reactiveUrl.startsWith('http')) {
		return reactiveUrl ?? '';
	}
	if (reactiveUrl.includes('redirect=true')) {
		return reactiveUrl;
	}
	const sep = reactiveUrl.includes('?') ? '&' : '?';
	return reactiveUrl + sep + 'redirect=true';
};
