<!-- 头部左边导航栏是否收起控制按钮 -->
<script setup lang="ts">
import { Fold, Expand } from '@element-plus/icons-vue';
import { useLayoutStore } from '@/stores/layout';
import { ElIcon } from 'element-plus';

const layoutStore = useLayoutStore();

const toggleClick = () => {
	layoutStore.switchLayoutAsideExtendState();
};
</script>

<template>
	<div class="hamburger-container" @click="toggleClick">
		<el-icon :size="40">
			<span v-if="layoutStore.asideIsExtend"><Fold /></span>
			<span v-else><Expand /></span>
		</el-icon>
	</div>
</template>

<style lang="scss" scoped>
.hamburger-container {
	margin-right: 16px;
	box-sizing: border-box;
	cursor: pointer;
}
</style>
