<script setup lang="ts">
import { useLayoutStore } from '@/stores/layout';
import { menus } from './menus.config';

const layoutStore = useLayoutStore();

const saveCurrentActiveId = (id) => {
	layoutStore.currentActiveId = id;
};
</script>

<template>
	<el-menu
		router
		unique-opened
		:default-active="layoutStore.currentActiveId"
		:collapse="!layoutStore.asideIsExtend"
		style="width: 100%; height: 100%; position: relative"
	>
		<el-menu-item
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
		</el-menu-item>

		<el-sub-menu v-for="(item, index) in menus" :key="index" :index="item.id">
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
		</el-sub-menu>
	</el-menu>
</template>

<style lang="scss" scoped></style>
