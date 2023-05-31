<!-- 头部左边导航栏是否收起控制按钮 -->
<script setup lang="ts">
import { Fold } from '@element-plus/icons-vue';
</script>

<template>
	<div class="hamburger-container">
		<el-icon :size="40"><Fold /></el-icon>
	</div>
</template>

<style lang="scss" scoped>
.hamburger-container {
	margin-right: 16px;
	box-sizing: border-box;
	cursor: pointer;
}
</style>
