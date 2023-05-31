<script setup lang="ts">
import { useLayoutStore } from '@/stores/layout';
const layoutStore = useLayoutStore();
</script>

<template>
	<el-menu
		router
		unique-opened
		default-active="0"
		:collapse="!layoutStore.asideIsExtend"
		style="width: 100%; height: 100%; position: relative"
	>
		<el-menu-item index="0">
			<el-icon><Odometer /></el-icon>
			<span>仪表盘</span>
		</el-menu-item>
		<el-sub-menu index="1">
			<template #title>
				<el-icon><DataAnalysis /></el-icon>
				<span>番剧</span>
			</template>
			<el-menu-item index="1-1" route="/anime/edit">
				<el-icon><Edit /></el-icon>
				<span>编辑</span>
			</el-menu-item>
			<el-menu-item index="1-2" route="/anime/list">
				<el-icon><Files /></el-icon>
				<span>列表</span>
			</el-menu-item>
		</el-sub-menu>

		<el-sub-menu index="2">
			<template #title>
				<el-icon><Picture /></el-icon>
				<span>漫画</span>
			</template>
			<el-menu-item index="2-1" route="/comic/edit">
				<el-icon><Edit /></el-icon>
				<span>编辑</span>
			</el-menu-item>
			<el-menu-item index="2-2" route="/comic/list">
				<el-icon><Files /></el-icon>
				<span>列表</span>
			</el-menu-item>
		</el-sub-menu>

		<el-sub-menu index="3">
			<template #title>
				<el-icon><Ship /></el-icon>
				<span>游戏</span>
			</template>
			<el-menu-item index="3-1" route="/game/edit">
				<el-icon><Edit /></el-icon>
				<span>编辑</span>
			</el-menu-item>
			<el-menu-item index="3-2" route="/game/list">
				<el-icon><Files /></el-icon>
				<span>列表</span>
			</el-menu-item>
		</el-sub-menu>

		<el-sub-menu index="4">
			<template #title>
				<el-icon><Headset /></el-icon>
				<span>音声</span>
			</template>
			<el-menu-item index="4-1" route="/music/edit">
				<el-icon><Edit /></el-icon>
				<span>编辑</span>
			</el-menu-item>
			<el-menu-item index="4-2" route="/music/list">
				<el-icon><Files /></el-icon>
				<span>列表</span>
			</el-menu-item>
		</el-sub-menu>

		<el-sub-menu index="5">
			<template #title>
				<el-icon><Collection /></el-icon>
				<span>小说</span>
			</template>
			<el-menu-item index="5-1" route="/novel/edit">
				<el-icon><Edit /></el-icon>
				<span>编辑</span>
			</el-menu-item>
			<el-menu-item index="5-2" route="/novel/list">
				<el-icon><Files /></el-icon>
				<span>列表</span>
			</el-menu-item>
		</el-sub-menu>

		<el-sub-menu index="9">
			<template #title>
				<el-icon><setting /></el-icon>
				<span>系统</span>
			</template>
			<el-menu-item index="9-1" route="/plugin">
				<el-icon><Tools /></el-icon>
				<span>插件</span>
			</el-menu-item>
			<el-menu-item index="9-2" route="/profile">
				<el-icon><UserFilled /></el-icon>
				<span>个人</span>
			</el-menu-item>
			<el-menu-item index="9-3" route="/setting">
				<el-icon><Orange /></el-icon>
				<span>设置</span>
			</el-menu-item>
		</el-sub-menu>
	</el-menu>
</template>

<style lang="scss" scoped></style>
