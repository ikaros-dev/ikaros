<script setup lang="ts">
// import { useRoute } from 'vue-router';
// const route = useRoute();
// const router = useRouter();

// const breadcrumbList = ref([]);

// const initBreadcrumbList = () => {
// 	console.log(route.matched);
// 	breadcrumbList.value = route.matched;
// };
// // const handleRedirect = (path) => {
// // 	router.push(path);
// // };

// watch(
// 	route,
// 	() => {
// 		initBreadcrumbList();
// 	},
// 	{ deep: true, immediate: true }
// );
</script>

<template>
	<el-breadcrumb separator="/">
		<el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
		<el-breadcrumb-item
			><a href="/">promotion management</a></el-breadcrumb-item
		>
		<el-breadcrumb-item>promotion list</el-breadcrumb-item>
		<el-breadcrumb-item>promotion detail</el-breadcrumb-item>
	</el-breadcrumb>
	<!-- <el-breadcrumb separator="/">
		<el-breadcrumb-item v-for="(item, index) in breadcrumbList" :key="index">
			<span class="no-redirect" v-if="index === breadcrumbList.length - 1">{{
				$t(`menus.${item.name}`)
			}}</span>
			<span class="redirect" v-else @click="handleRedirect(item.path)">{{
				$t(`menus.${item.name}`)
			}}</span>
		</el-breadcrumb-item>
	</el-breadcrumb> -->
</template>

<style lang="scss" scoped>
@import '@/styles/variables.module.scss';
.no-redirect {
	color: #97a8be;
	cursor: text;
}
.redirect {
	color: #666;
	font-weight: 600;
	cursor: pointer;
	&:hover {
		color: $menuBg;
	}
}
</style>
