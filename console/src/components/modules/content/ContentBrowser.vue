<script setup lang="ts">
import { Search } from '@element-plus/icons-vue';
import {
	ElButton,
	ElEmpty,
	ElInput,
	ElPagination,
	ElResult,
	ElSkeleton,
} from 'element-plus';

interface ContentBrowserProps {
	title: string;
	searchModelValue: string;
	searchPlaceholder: string;
	loading: boolean;
	error: string | null;
	emptyTitle: string;
	emptyDescription: string;
	page: number;
	size: number;
	total: number;
	pageSizes: number[];
}

defineProps<ContentBrowserProps>();

const emit = defineEmits<{
	(event: 'update:searchModelValue', value: string): void;
	(event: 'search'): void;
	(event: 'retry'): void;
	(event: 'update:page', value: number): void;
	(event: 'update:size', value: number): void;
}>();
</script>

<template>
	<section class="content-browser">
		<header class="content-browser__header">
			<h1 class="content-browser__title">{{ title }}</h1>
			<div v-if="$slots.actions" class="content-browser__actions">
				<slot name="actions" />
			</div>
		</header>

		<div v-if="$slots.breadcrumb" class="content-browser__breadcrumb">
			<slot name="breadcrumb" />
		</div>

		<el-input
			:model-value="searchModelValue"
			class="content-browser__search"
			:placeholder="searchPlaceholder"
			clearable
			@update:model-value="emit('update:searchModelValue', $event)"
			@keyup.enter="emit('search')"
			@clear="emit('search')"
		>
			<template #append>
				<el-button
					class="content-browser__search-button"
					:icon="Search"
					aria-label="搜索"
					@click="emit('search')"
				/>
			</template>
		</el-input>

		<div class="content-browser__body">
			<el-skeleton
				v-if="loading"
				class="content-browser__loading"
				:rows="6"
				animated
			/>
			<el-result
				v-else-if="error"
				class="content-browser__error"
				icon="error"
				title="加载失败"
				:sub-title="error"
			>
				<template #extra>
					<el-button type="primary" @click="emit('retry')">重试</el-button>
				</template>
			</el-result>
			<el-empty
				v-else-if="total === 0"
				class="content-browser__empty"
				:description="emptyDescription"
			>
				<h2 class="content-browser__empty-title">{{ emptyTitle }}</h2>
				<div v-if="$slots['empty-actions']" class="content-browser__empty-actions">
					<slot name="empty-actions" />
				</div>
			</el-empty>
			<div v-else class="content-browser__content">
				<slot />
			</div>
		</div>

		<el-pagination
			v-if="!loading && !error && total > 0"
			class="content-browser__pagination"
			:current-page="page"
			:page-size="size"
			:page-sizes="pageSizes"
			:total="total"
			background
			layout="total, sizes, prev, pager, next, jumper"
			@current-change="emit('update:page', $event)"
			@size-change="emit('update:size', $event)"
		/>
	</section>
</template>

<style lang="scss" scoped>
.content-browser {
	display: flex;
	flex-direction: column;
	gap: 20px;
}

.content-browser__header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16px;
}

.content-browser__title,
.content-browser__empty-title {
	margin: 0;
}

.content-browser__title {
	font-size: 24px;
}

.content-browser__actions {
	display: flex;
	flex-wrap: wrap;
	justify-content: flex-end;
	gap: 12px;
}

.content-browser__body {
	min-height: 160px;
}

.content-browser__empty-title {
	margin-bottom: 12px;
	font-size: 18px;
}

.content-browser__empty-actions {
	display: flex;
	justify-content: center;
	gap: 12px;
}

.content-browser__pagination {
	justify-content: flex-end;
}
</style>
