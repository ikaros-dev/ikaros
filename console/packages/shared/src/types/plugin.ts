import type { Component } from 'vue';
import type { RouteRecordRaw, RouteRecordName } from 'vue-router';

export interface RouteRecordAppend {
	parentName: RouteRecordName;
	route: RouteRecordRaw;
}

export interface ExtensionPoint {
	// 根据第三方元数据平台ID快速填充条目数据，这个值是插件指定的三方平台枚举名称，插件需要自己实现服务端对应的转化拓展点。
	'subject:sync:platform'?: () => string | Promise<string>;
}

export interface PluginModule {
	/**
	 * Current plugin module name.
	 */
	name: string;

	/**
	 * These components will be registered when plugin is activated.
	 */
	components?: Record<string, Component>;

	/**
	 * Activate hook will be called when plugin is activated.
	 */
	activated?: () => void;

	/**
	 * Deactivate hook will be called when plugin is deactivated.
	 */
	deactivated?: () => void;

	routes?: RouteRecordRaw[] | RouteRecordAppend[];

	extensionPoints?: ExtensionPoint;
}
