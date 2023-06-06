<script setup lang="ts">
import { useLayoutStore } from '@/stores/layout';
import { i18n } from '@/locales';

const t = i18n.global.t;
const route = useRoute();
const router = useRouter();
const layoutStore = useLayoutStore();

let breadcrumbList: Ref = ref([]);

const initBreadcrumbList = () => {
	console.log('route.matched: ', route.matched);
	breadcrumbList.value = route.matched;
};

const handleRedirect = (path) => {
	router.push(path);
	layoutStore.updateCurrentActionIdByRoutePath(path);
};

watch(
	route,
	() => {
		initBreadcrumbList();
	},
	{ deep: true, immediate: true }
);
</script>

<template>
	<el-breadcrumb separator=" ">
		<el-breadcrumb-item v-for="(item, index) in breadcrumbList" :key="index">
			<span v-if="index === breadcrumbList.length - 1" class="no-redirect">
				{{ t(item.meta.title) }}
			</span>
			<span v-else class="redirect" @click="handleRedirect(item.path)">
				{{ item?.meta?.title ? t(item.meta.title) : item.name }}
			</span>
		</el-breadcrumb-item>
	</el-breadcrumb>
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
