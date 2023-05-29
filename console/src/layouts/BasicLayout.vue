<script setup lang="ts">
import { RouterView } from 'vue-router';
</script>

<template>
	<div>
		<el-container>
			<el-aside width="200px"> Aside </el-aside>
			<el-container>
				<el-header>Header</el-header>
				<el-main><RouterView /></el-main>
			</el-container>
		</el-container>
	</div>
</template>

<style lang="less" scoped></style>
