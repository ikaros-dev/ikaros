<script setup lang="ts">
import { computed, ref } from 'vue';
import { Subject } from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';
import { base64Encode } from '@/utils/string-util';
import {
	ElDrawer,
	ElRow,
	ElCol,
	ElInput,
	ElForm,
	ElFormItem,
	ElSelect,
	ElOption,
	ElTable,
	ElTableColumn,
	ElPagination,
} from 'element-plus';
import { onMounted } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const props = withDefaults(
	defineProps<{
		visible: boolean;
	}>(),
	{
		visible: false,
	}
);

const emit = defineEmits<{
	// eslint-disable-next-line no-unused-vars
	(event: 'update:visible', visible: boolean): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'close'): void;
	// eslint-disable-next-line no-unused-vars
	(event: 'selectionsChange', set: Set<number>);
}>();

const drawerVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

const handleClose = () => {
	emit('selectionsChange', selectionSubjectIdSet);
	drawerVisible.value = false;
};

interface SubjectsCondition {
	page: number;
	size: number;
	total: number;
	name: string;
	nameCn: string;
	nsfw: boolean;
	type?: 'ANIME' | 'COMIC' | 'GAME' | 'MUSIC' | 'NOVEL' | 'REAL' | 'OTHER';
}
const findSubjectsCondition = ref<SubjectsCondition>({
	page: 1,
	size: 10,
	total: 10,
	name: '',
	nameCn: '',
	nsfw: false,
	type: undefined,
});
const subjects = ref<Subject[]>([]);

const fetchSubjects = async () => {
	const { data } = await apiClient.subject.listSubjectsByCondition({
		page: findSubjectsCondition.value.page,
		size: findSubjectsCondition.value.size,
		name: base64Encode(findSubjectsCondition.value.name),
		nameCn: base64Encode(findSubjectsCondition.value.nameCn),
		nsfw: findSubjectsCondition.value.nsfw,
		type: findSubjectsCondition.value.type,
	});
	findSubjectsCondition.value.page = data.page;
	findSubjectsCondition.value.size = data.size;
	findSubjectsCondition.value.total = data.total;
	subjects.value = data.items as Subject[];
};

const selectionSubjectIdSet = new Set<number>();
const handleSelectionChange = (selection) => {
	// console.log('selection', selection);
	selectionSubjectIdSet.clear();
	selection.forEach((sub) => {
		selectionSubjectIdSet.add(sub.id);
	});
	// console.log('selectionSubjectIdSet', selectionSubjectIdSet);
};

onMounted(fetchSubjects);
</script>

<template>
	<el-drawer
		v-model="drawerVisible"
		:title="t('module.subject.drawer.select.title')"
		direction="rtl"
		:before-close="handleClose"
		size="50%"
	>
		<template #header>
			<div align="center">
				<h4>{{ t('module.subject.drawer.select.text.subject-select') }}</h4>
			</div>
		</template>
		<template #default>
			<el-form :inline="true" :model="findSubjectsCondition">
				<el-row>
					<el-col :span="24">
						<el-form-item
							:label="t('module.subject.drawer.select.label.name')"
							style="width: 95%"
						>
							<el-input
								v-model="findSubjectsCondition.name"
								:placeholder="
									t('module.subject.drawer.select.placeholder.name')
								"
								clearable
								@change="fetchSubjects"
							/>
						</el-form-item>
					</el-col>
					<el-col :span="24">
						<el-form-item
							:label="t('module.subject.drawer.select.label.name_cn')"
							style="width: 95%"
						>
							<el-input
								v-model="findSubjectsCondition.nameCn"
								:placeholder="
									t('module.subject.drawer.select.placeholder.name_cn')
								"
								clearable
								@change="fetchSubjects"
							/>
						</el-form-item>
					</el-col>
				</el-row>
				<el-row>
					<el-col :xs="24" :sm="24" :md="24" :lg="12" :xl="12">
						<el-form-item label="NSFW" style="width: 95%">
							<el-select
								v-model="findSubjectsCondition.nsfw"
								clearable
								@change="fetchSubjects"
							>
								<el-option
									:label="t('module.subject.drawer.select.optin.nsfw-true')"
									:value="true"
								/>
								<el-option
									:label="t('module.subject.drawer.select.optin.nsfw-false')"
									:value="false"
								/>
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :xs="24" :sm="24" :md="24" :lg="12" :xl="12">
						<el-form-item
							:label="t('module.subject.drawer.select.label.type')"
							style="width: 95%"
						>
							<el-select
								v-model="findSubjectsCondition.type"
								clearable
								@change="fetchSubjects"
							>
								<el-option
									:label="t('module.subject.drawer.select.type.anime')"
									value="ANIME"
								/>
								<el-option
									:label="t('module.subject.drawer.select.type.comic')"
									value="COMIC"
								/>
								<el-option
									:label="t('module.subject.drawer.select.type.game')"
									value="GAME"
								/>
								<el-option
									:label="t('module.subject.drawer.select.type.music')"
									value="MUSIC"
								/>
								<el-option
									:label="t('module.subject.drawer.select.type.novel')"
									value="NOVEL"
								/>
								<el-option
									:label="t('module.subject.drawer.select.type.real')"
									value="REAL"
								/>
								<el-option
									:label="t('module.subject.drawer.select.type.other')"
									value="OTHER"
								/>
							</el-select>
						</el-form-item>
					</el-col>
				</el-row>
			</el-form>

			<el-row>
				<el-col :span="24"
					><el-pagination
						v-model:page-size="findSubjectsCondition.size"
						v-model:current-page="findSubjectsCondition.page"
						background
						:total="findSubjectsCondition.total"
						layout="total, sizes, prev, pager, next, jumper"
						@current-change="fetchSubjects"
						@size-change="fetchSubjects"
				/></el-col>
			</el-row>
			<br />
			<el-row>
				<el-table
					:data="subjects"
					style="width: 100%"
					@selection-change="handleSelectionChange"
				>
					<el-table-column type="selection" width="50" show-overflow-tooltip />
					<el-table-column
						prop="id"
						label="ID"
						width="100"
						show-overflow-tooltip
					/>
					<el-table-column
						prop="name"
						:label="t('module.subject.drawer.select.table.label.name')"
						show-overflow-tooltip
					/>
					<el-table-column
						prop="name_cn"
						:label="t('module.subject.drawer.select.table.label.name_cn')"
						show-overflow-tooltip
					/>
					<el-table-column
						prop="type"
						:label="t('module.subject.drawer.select.table.label.type')"
						width="100"
						show-overflow-tooltip
					/>
				</el-table>
			</el-row>
		</template>
	</el-drawer>
</template>

<style lang="scss" scoped></style>
