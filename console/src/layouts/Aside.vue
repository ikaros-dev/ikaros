<script setup lang="ts">
import { useLayoutStore } from '@/stores/layout';
// import { menus as menuOld } from './menus.config';
// import { Odometer } from '@element-plus/icons-vue';
import type { MenuGroupType, MenuItemType } from '@runikaros/shared';
import {
	// RouterView,
	// useRoute,
	useRouter,
	type RouteRecordRaw,
} from 'vue-router';
import sortBy from 'lodash.sortby';
import { coreMenuGroups } from '@/router/routes.config';
import { i18n } from '@/locales';

const t = i18n.global.t;
const layoutStore = useLayoutStore();
// const route = useRoute();
const router = useRouter();

const saveCurrentActiveId = (id) => {
	layoutStore.currentActiveId = id;
};

// Generate menus by routes
const menus = ref<MenuGroupType[]>([] as MenuGroupType[]);
const minimenus = ref<MenuItemType[]>([] as MenuItemType[]);

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

	minimenus.value = menus.value
		.reduce((acc, group) => {
			if (group?.items) {
				acc.push(...group.items);
			}
			return acc;
		}, [] as MenuItemType[])
		.filter((item) => item.mobile);
};

onMounted(generateMenus);

// console.log('menus', menus);
// console.log('minimenus', minimenus);
</script>

<template>
	<el-menu
		router
		unique-opened
		:default-active="layoutStore.currentActiveId"
		:collapse="!layoutStore.asideIsExtend"
		style="width: 100%; height: 100%; position: relative"
	>
		<!-- <el-menu-item
			index="0"
			route="/dashboard"
			@click="saveCurrentActiveId('0')"
		>
			<el-icon>
				<Odometer />
			</el-icon>
			<template #title>
				<span>仪表盘</span>
			</template>
		</el-menu-item> -->

		<!-- <el-sub-menu v-for="(item, index) in menuOld" :key="index" :index="item.id">
			<template #title>
				<el-icon>
					<component :is="item.elIcon"></component>
				</el-icon>
				<span>{{ item.name }}</span>
			</template>
			<el-menu-item
				v-for="item2 in item.children"
				:key="item2.id"
				:index="item2.id.toString()"
				:route="item2.path"
				@click="saveCurrentActiveId(item2.id)"
			>
				<el-icon>
					<component :is="item2.elIcon"></component>
				</el-icon>
				<template #title>
					<span>{{ item2.name }}</span>
				</template>
			</el-menu-item>
		</el-sub-menu> -->

		<div v-for="group in menus" :key="group.id">
			<div v-if="!group.id || !group.name">
				<el-menu-item
					v-for="item in minimenus"
					:key="item.path"
					:index="item.path"
					:route="item.path"
					@click="saveCurrentActiveId(item.path)"
				>
					<el-icon>
						<component :is="item.icon"></component>
					</el-icon>
					<template #title>
						<span>{{ t(item.name) }}</span>
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
						@click="saveCurrentActiveId(item.path)"
					>
						<el-icon>
							<component :is="item.icon"></component>
						</el-icon>
						<template #title>
							<span>{{ t(item.name) }}</span>
						</template>
					</el-menu-item>
				</el-menu-item-group>
			</div>
		</div>

		<!-- <el-menu-item-group title="Group One">
			<el-menu-item index="1-1">
				<el-icon><Odometer /></el-icon>
				item one
			</el-menu-item>
			<el-menu-item index="1-2">
				<el-icon><Odometer /></el-icon>
				item two
			</el-menu-item>
		</el-menu-item-group>
		<el-menu-item-group title="Group Two">
			<el-menu-item index="1-3">
				<el-icon><Odometer /></el-icon>
				item three
			</el-menu-item>
		</el-menu-item-group> -->
	</el-menu>
</template>

<style lang="scss" scoped></style>
