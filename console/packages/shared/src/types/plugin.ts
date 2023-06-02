import type { Component } from 'vue';
import type { RouteRecordRaw, RouteRecordName } from 'vue-router';

export interface RouteRecordAppend {
	parentName: RouteRecordName;
	route: RouteRecordRaw;
}

export interface ExtensionPoint {
	// todo add some extension points
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
