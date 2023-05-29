<script setup lang="ts">
import { RouterView } from 'vue-router';
</script>

<template>
	<!-- 此处样式撑满所有屏幕 -->
	<el-container style="height: 100%; width: 100%; position: absolute">
		<el-aside class="ik-bl-aside"> Aside </el-aside>
		<el-container>
			<el-header class="ik-blc-header">Header</el-header>
			<el-main class="ik-blc-main">
				<p>Main</p>
				<br />
				<RouterView />
			</el-main>
			<el-footer class="ik-blc-footer">Footer</el-footer>
		</el-container>
	</el-container>
</template>

<style lang="scss" scoped>
.ik-bl-header {
	background-color: blue;
}
.ik-bl-aside {
	background-color: brown;
	width: 200px;
}
.ik-blc-main {
	background-color: aliceblue;
}
.ik-blc-footer {
	background-color: whitesmoke;
}
</style>
