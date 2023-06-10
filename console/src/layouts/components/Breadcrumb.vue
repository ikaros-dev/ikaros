<script setup lang="ts">
import { useLayoutStore } from '@/stores/layout';
import { i18n } from '@/locales';

const t = i18n.global.t;
const route = useRoute();
const router = useRouter();
const layoutStore = useLayoutStore();

let breadcrumbList: Ref = ref([]);

const initBreadcrumbList = () => {
	// console.log('route.matched: ', route.matched);
	breadcrumbList.value = route.matched;
};

const handleRedirect = (path) => {
	if (path) {
		router.push(path);
		layoutStore.updatecurrentActivePathByRoutePath(path);
	}
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
	<el-breadcrumb separator="/">
		<el-breadcrumb-item v-for="(item, index) in breadcrumbList" :key="index">
			<span
				v-if="item && ((item.meta && item.meta.title) || item.name)"
				:class="
					index === breadcrumbList.length - 1 ? 'no-redirect' : 'redirect'
				"
				@click="handleRedirect(item?.path)"
			>
				{{
					item?.meta?.title
						? i18n.global.te(item?.meta?.title)
							? t(item?.meta?.title)
							: item?.meta?.title
						: i18n.global.te(item?.name)
						? t(item?.name)
						: item?.name
				}}
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
