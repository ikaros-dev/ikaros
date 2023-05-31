<script setup lang="ts">
import { useLayoutStore } from '@/stores/layout';
const layoutStore = useLayoutStore();
</script>

<template>
	<el-menu
		:collapse="!layoutStore.asideIsExtend"
		style="width: 100%; height: 100%; position: relative"
	>
		<el-sub-menu index="1">
			<template #title>
				<el-icon><DataAnalysis /></el-icon>
				<span>番剧</span>
			</template>
			<el-menu-item index="1-1">新增</el-menu-item>
			<el-menu-item index="1-2">新增</el-menu-item>
			<el-menu-item index="1-3">新增</el-menu-item>
			<el-menu-item index="1-4">新增</el-menu-item>
		</el-sub-menu>

		<el-sub-menu index="2">
			<template #title>
				<el-icon><Picture /></el-icon>
				<span>漫画</span>
			</template>
			<el-menu-item index="2-1">新增</el-menu-item>
			<el-menu-item index="2-2">新增</el-menu-item>
			<el-menu-item index="2-3">新增</el-menu-item>
			<el-menu-item index="2-4">新增</el-menu-item>
		</el-sub-menu>

		<el-sub-menu index="3">
			<template #title>
				<el-icon><Ship /></el-icon>
				<span>游戏</span>
			</template>
			<el-menu-item index="3-1">新增</el-menu-item>
			<el-menu-item index="3-2">新增</el-menu-item>
			<el-menu-item index="3-3">新增</el-menu-item>
			<el-menu-item index="3-4">新增</el-menu-item>
		</el-sub-menu>

		<el-sub-menu index="4">
			<template #title>
				<el-icon><Headset /></el-icon>
				<span>音声</span>
			</template>
			<el-menu-item index="4-1">新增</el-menu-item>
			<el-menu-item index="4-2">新增</el-menu-item>
			<el-menu-item index="4-3">新增</el-menu-item>
			<el-menu-item index="4-4">新增</el-menu-item>
		</el-sub-menu>

		<el-sub-menu index="5">
			<template #title>
				<el-icon><Collection /></el-icon>
				<span>小说</span>
			</template>
			<el-menu-item index="5-1">新增</el-menu-item>
			<el-menu-item index="5-2">新增</el-menu-item>
			<el-menu-item index="5-3">新增</el-menu-item>
			<el-menu-item index="5-4">新增</el-menu-item>
		</el-sub-menu>

		<el-sub-menu index="9">
			<template #title>
				<el-icon><setting /></el-icon>
				<span>系统</span>
			</template>
			<el-menu-item index="9-1">个人中心</el-menu-item>
			<el-menu-item index="9-2">新增</el-menu-item>
			<el-menu-item index="9-3">新增</el-menu-item>
			<el-menu-item index="9-4">新增</el-menu-item>
		</el-sub-menu>
	</el-menu>
</template>

<style lang="scss" scoped></style>
