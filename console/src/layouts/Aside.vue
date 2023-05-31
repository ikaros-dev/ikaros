<script setup lang="ts">
import { useLayoutStore } from '@/stores/layout';
import menus from './menus.config';

const layoutStore = useLayoutStore();

const savePath = (path) => {
	layoutStore.currentActivePath = path;
};
</script>

<template>
	<el-menu
		router
		unique-opened
		:default-active="layoutStore.currentActivePath"
		:collapse="!layoutStore.asideIsExtend"
		style="width: 100%; height: 100%; position: relative"
	>
		<div v-for="item in menus" :key="item.id">
			<span v-if="item.children && item.children.length">
				<el-sub-menu :index="item.id.toString()">
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
						@click="savePath(item2.path)"
					>
						<el-icon>
							<component :is="item2.elIcon"></component>
						</el-icon>
						<span>{{ item2.name }}</span>
					</el-menu-item>
				</el-sub-menu>
			</span>
			<span v-else>
				<el-menu-item
					:index="item.id.toString()"
					:route="item.path"
					@click="savePath(item.path)"
				>
					<el-icon>
						<component :is="item.elIcon"></component>
					</el-icon>
					<span>{{ item.name }}</span>
				</el-menu-item>
			</span>
		</div>
	</el-menu>
</template>

<style lang="scss" scoped></style>
