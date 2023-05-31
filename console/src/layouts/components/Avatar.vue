<!-- 头部右边头像带下拉框 -->
<script setup lang="ts">
const avatarSrcUrl =
	'https://gss0.baidu.com/7Po3dSag_xI4khGko9WTAnF6hhy/zhidao/pic/item/e7cd7b899e510fb3f001a7d2db33c895d1430c5f.jpg';
</script>

<template>
	<el-dropdown>
		<el-avatar :size="55" :src="avatarSrcUrl" />
		<template #dropdown>
			<el-dropdown-menu>
				<el-dropdown-item>退出</el-dropdown-item>
			</el-dropdown-menu>
		</template>
	</el-dropdown>
</template>

<style lang="scss" scoped></style>
