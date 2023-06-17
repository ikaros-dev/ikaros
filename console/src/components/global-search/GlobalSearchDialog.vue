<script setup lang="ts">
import { ref, nextTick, computed } from 'vue';
// eslint-disable-next-line no-unused-vars
import { Search } from '@element-plus/icons-vue';
import { apiClient } from '@/utils/api-client';
import { FileHint, SubjectHint } from 'packages/api-client/dist.ts';
import { ElMessage } from 'element-plus';

const props = withDefaults(
	defineProps<{
		visible: boolean;
	}>(),
	{
		visible: false,
	}
);

const router = useRouter();

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'close'): void;
}>();

const dialogVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

const search = ref({
	type: 'SUBJECT',
	keyword: '',
});

const subjectHits = ref<SubjectHint[]>();
const fileHits = ref<FileHint[]>();

const searchByKeyword = async () => {
	if (!search.value.keyword) {
		ElMessage.warning('请输入值进行查询');
		return;
	}
	const type = search.value.type;
	if ('SUBJECT' === type) {
		const { data } = await apiClient.indices.searchSubject({
			keyword: search.value.keyword,
			limit: 10,
		});
		if (!data.hits || data.hits.length === 0) {
			ElMessage.warning('未查询到条目数据');
		}
		subjectHits.value = data.hits;
	}

	if ('FILE' === type) {
		const { data } = await apiClient.indices.searchFile({
			keyword: search.value.keyword,
			limit: 10,
		});
		if (!data.hits || data.hits.length === 0) {
			ElMessage.warning('未查询到文件数据');
		}
		fileHits.value = data.hits;
	}
};

const toDetailPage = (url: string) => {
	dialogVisible.value = false;
	search.value.keyword = '';
	subjectHits.value = [];
	router.push(url);
};

const searchInputRef = ref();

const onOpened = () => {
	nextTick(() => {
		searchInputRef.value.focus();
	});
};

const activeTab = ref('SUBJECT');

const onselectionchange = (val: string) => {
	activeTab.value = val;
};

onMounted(() => {});
</script>

<template>
	<el-dialog
		v-model="dialogVisible"
		width="40%"
		:modal="true"
		top="5vh"
		:fullscreen="false"
		:show-close="false"
		@opened="onOpened"
	>
		<template #header>
			<el-input
				ref="searchInputRef"
				v-model="search.keyword"
				style="width: 100%"
				size="large"
				placeholder="请输入关键词，回车搜索。"
				@keydown.enter="searchByKeyword"
			>
				<template #prepend>
					<el-select
						v-model="search.type"
						style="width: 80px"
						size="large"
						@change="onselectionchange"
					>
						<el-option label="条目" value="SUBJECT" />
						<el-option label="文件" value="FILE" />
					</el-select>
				</template>
				<!-- <template #append>
					<el-button :icon="Search" @click="searchByKeyword" />
				</template> -->
			</el-input>
		</template>

		<el-tabs v-model="activeTab">
			<el-tab-pane label="条目" name="SUBJECT">
				<ul v-if="subjectHits && subjectHits.length > 0" class="ik-content-ul">
					<li
						v-for="subjectHit in subjectHits"
						:key="subjectHit.id"
						class="ik-content-ul-li"
						tabindex="0"
						@keydown.enter="
							toDetailPage('/subjects/subject/details/' + subjectHit.id)
						"
						@click="toDetailPage('/subjects/subject/details/' + subjectHit.id)"
					>
						<span class="ik-subject-name">
							<span>{{ subjectHit.name }} </span>
							<span class="grey">{{ subjectHit.nameCn }}</span>
						</span>
					</li>
				</ul>

				<span v-else> 暂无数据 </span>
			</el-tab-pane>
			<el-tab-pane label="文件" name="FILE">
				<ul v-if="fileHits && fileHits.length > 0" class="ik-content-ul">
					<li
						v-for="fileHit in fileHits"
						:key="fileHit.id"
						class="ik-content-ul-li"
						tabindex="0"
						@keydown.enter="
							toDetailPage('/files?searchFileName=' + fileHit.name)
						"
						@click="toDetailPage('/files?searchFileName=' + fileHit.name)"
					>
						<span class="ik-subject-name">
							<span>{{ fileHit.name }} </span>
							<span class="grey">{{ fileHit.originalName }}</span>
						</span>
					</li>
				</ul>

				<span v-else> 暂无数据 </span>
			</el-tab-pane>
		</el-tabs>

		<template #footer>
			<span> [Tab]-下一个 &nbsp; [Shift+Tab]-上一个 &nbsp; [Enter]-确认</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped>
.ik-content-ul {
	display: inline;
	width: 100%;
	margin: 0;
}

.ik-content-ul-li {
	// border: 1px solid rebeccapurple;
	border-radius: 5px;
	background-color: rgba(212, 226, 248, 0.244);
	cursor: pointer;
	min-height: 50px;
	height: 50px;
	line-height: 50px;
	padding: 0 5px;
	margin: 2px 0;
	display: block;
	// vertical-align: middle;
}

.ik-content-ul-li:hover {
	box-shadow: 0px 0px 5px #888888;
}

.ik-subject-name {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	height: 15px;
	.grey {
		font-size: 10px;
		color: #999;
	}
}
</style>
