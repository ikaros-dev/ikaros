<script setup lang="ts">
import { useLayoutStore } from '@/stores/layout';
import type { MenuGroupType } from '@runikaros/shared';
import { useRouter, type RouteRecordRaw, useRoute } from 'vue-router';
import sortBy from 'lodash.sortby';
import { coreMenuGroups } from '@/router/routes.config';
import { i18n } from '@/locales';
import { onMounted, ref } from 'vue';
import { ElMenu, ElMenuItem, ElIcon, ElMenuItemGroup } from 'element-plus';

const t = i18n.global.t;
const layoutStore = useLayoutStore();
const route = useRoute();
const router = useRouter();

// Generate menus by routes
const menus = ref<MenuGroupType[]>([] as MenuGroupType[]);

const generateMenus = () => {
	// console.log('router.getRoutes(): ', router.getRoutes());
	// sort by menu.priority and meta.core
	const currentRoutes = sortBy(
		router.getRoutes().filter((route) => {
			const { meta } = route;
			if (!meta?.menu) {
				return false;
			}
			return true;
		}),
		[
			(route: RouteRecordRaw) => !route.meta?.core,
			// @ts-ignore
			(route: RouteRecordRaw) => route.meta?.menu?.priority || 0,
		]
	);

	// group by menu.group
	menus.value = currentRoutes.reduce((acc, route) => {
		const { menu } = route.meta;
		if (!menu) {
			return acc;
		}
		const group = acc.find((item) => item.id === menu.group);
		const childRoute = route.children[0];
		const childMetaMenu = childRoute?.meta?.menu;

		// only support one level
		const menuChildren = childMetaMenu
			? [
					{
						name: childMetaMenu.name,
						path: childRoute.path,
						icon: childMetaMenu.icon,
					},
			  ]
			: undefined;
		if (group) {
			group.items?.push({
				name: menu.name,
				path: route.path,
				icon: menu.icon,
				mobile: menu.mobile,
				children: menuChildren,
			});
		} else {
			const menuGroup = coreMenuGroups.find((item) => item.id === menu.group);
			let name = '';
			if (!menuGroup) {
				name = menu.group;
			} else if (menuGroup.name) {
				name = menuGroup.name;
			}
			acc.push({
				id: menuGroup?.id || menu.group,
				name: name,
				priority: menuGroup?.priority || 0,
				items: [
					{
						name: menu.name,
						path: route.path,
						icon: menu.icon,
						mobile: menu.mobile,
						children: menuChildren,
					},
				],
			});
		}
		return acc;
	}, [] as MenuGroupType[]);

	// filter undefined
	menus.value = menus.value.filter((group) => group.id);

	// sort by menu.priority
	menus.value = sortBy(menus.value, [
		(menu: MenuGroupType) => {
			return coreMenuGroups.findIndex((item) => item.id === menu.id) < 0;
		},
		(menu: MenuGroupType) => menu.priority || 0,
	]);

	// Set current active path from route
	switchActivePath(route.path);
};

const switchActivePath = (path) => {
	layoutStore.currentActivePath = path;
};

onMounted(generateMenus);

// console.log('menus', menus);
</script>

<template>
	<el-menu
		router
		unique-opened
		:default-active="layoutStore.currentActivePath"
		:collapse="!layoutStore.asideIsExtend"
		style="width: 100%; height: 100%; position: relative"
		@select="switchActivePath"
	>
		<div v-for="group in menus" :key="group.id">
			<div v-if="!group.id || !group.name">
				<el-menu-item
					v-for="item in group.items"
					:key="item.path"
					:index="item.path"
					:route="item.path"
				>
					<el-icon>
						<component :is="item.icon"></component>
					</el-icon>
					<template #title>
						<span>
							{{ i18n.global.te(item.name) ? t(item.name) : item.name }}
						</span>
					</template>
				</el-menu-item>
			</div>
			<div v-else>
				<el-menu-item-group>
					<template #title>
						<span>{{ t(group.name) }}</span>
					</template>
					<el-menu-item
						v-for="item in group.items"
						:key="item.path"
						:index="item.path"
						:route="item.path"
					>
						<el-icon>
							<component :is="item.icon"></component>
						</el-icon>
						<template #title>
							<span>
								{{ i18n.global.te(item.name) ? t(item.name) : item.name }}
							</span>
						</template>
					</el-menu-item>
				</el-menu-item-group>
			</div>
		</div>
	</el-menu>
</template>

<style lang="scss" scoped></style>
