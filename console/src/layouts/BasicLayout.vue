<script setup lang="ts">
import Aside from './Aside.vue';
import Header from './Header.vue';
import Footer from './Footer.vue';
</script>

<template>
	<!-- 此处样式撑满所有屏幕 -->
	<el-container style="height: 100%; width: 100%; position: absolute">
		<el-aside> <Aside /> </el-aside>
		<el-container>
			<el-header><Header /></el-header>
			<el-main class="ik-blc-main">
				<p>Main</p>
				<br />
				<RouterView />
			</el-main>
			<el-footer height="30px"><Footer /></el-footer>
		</el-container>
	</el-container>
</template>

<style lang="scss" scoped>
.ik-blc-main {
	background-color: aliceblue;
}
.el-header {
	padding: 0;
	margin: 0;
}
</style>
