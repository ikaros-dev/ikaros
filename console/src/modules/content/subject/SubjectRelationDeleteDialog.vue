<script setup lang="ts">
import { computed, ref } from 'vue';
import {
	ElDialog,
	ElForm,
	ElFormItem,
	ElButton,
	ElInput,
	ElMessage,
	ElPopconfirm,
} from 'element-plus';
import { Tickets } from '@element-plus/icons-vue';
import SubjectRelactionDeleteDrawer from './SubjectRelactionDeleteDrawer.vue';
import { SubjectRelation } from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';
import { useSubjectStore } from '@/stores/subject';
import { useI18n } from 'vue-i18n';

const subjectStore = useSubjectStore();
const {t} = useI18n();

const props = withDefaults(
	defineProps<{
		visible: boolean;
		masterSubjectId: number;
		relationSubjects: SubjectRelation[];
	}>(),
	{
		visible: false,
		masterSubjectId: -1,
	}
);

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

const onClose = () => {
	dialogVisible.value = false;
	emit('close');
};

const subjectRelationDeleteDrawerVisible = ref(false);
const slaveSubjectTypesStr = ref('[]');
const slaveSubjectIdsStr = ref('[]');
const subjectRelations = ref<SubjectRelation[]>([]);
const onSelectionsChange = (subjectRels) => {
	// console.log('receive subjectRelations', subjectRelations);
	subjectRelations.value = subjectRels;
	let types = new Set<string>();
	let ids = new Set<number>();
	subjectRels.forEach((subRel) => {
		types.add(subRel.relation_type);
		subRel.relation_subjects.forEach((id) => ids.add(id));
	});
	let typeArr: string[] = [];
	let idArr: number[] = [];
	types.forEach((v) => typeArr.push(v));
	ids.forEach((v) => idArr.push(v));
	slaveSubjectTypesStr.value = JSON.stringify(typeArr);
	slaveSubjectIdsStr.value = JSON.stringify(idArr);
};

const reqRemoveRelactionBtnLoading = ref(false);
const reqRemoveRelaction = async () => {
	if (!subjectRelations.value || subjectRelations.value.length === 0) {
		ElMessage.warning(
			t('module.subject.relaction.dialog.delete.message.validate-fail')
		);
		return;
	}
	reqRemoveRelactionBtnLoading.value = true;
	// await apiClient.subjectRelation.removeSubjectRelation({
	// });
	await subjectRelations.value.forEach(async (subRel) => {
		const subIdSet = subRel.relation_subjects;
		let ids: number[] = [];
		subIdSet.forEach((v) => ids.push(v));
		await apiClient.subjectRelation.removeSubjectRelation({
			subjectId: subRel.subject,
			relationType: subRel.relation_type,
			relationSubjects: JSON.stringify(ids),
		});
		subjectStore.clearSubjectCacheById(subRel.subject);
	});
	ElMessage.success(t('module.subject.relaction.dialog.delete.message.remove-success'));
	reqRemoveRelactionBtnLoading.value = false;
	onClose();
};
</script>

<template>
	<SubjectRelactionDeleteDrawer
		v-model:visible="subjectRelationDeleteDrawerVisible"
		:masterSubjectId="props.masterSubjectId"
		:relationSubjects="props.relationSubjects"
		@selections-change="onSelectionsChange"
	/>
	<el-dialog
		v-model="dialogVisible"
		style="width: 40%"
		:title="t('module.subject.relaction.dialog.delete.title')"
		@close="onClose"
	>
		<el-form label-width="100px" style="max-width: 460px">
			<el-form-item :label="t('module.subject.relaction.dialog.delete.label.master-subject')">
				<el-input disabled :value="props.masterSubjectId" />
			</el-form-item>
			<el-form-item :label="t('module.subject.relaction.dialog.delete.label.slave-subject')">
				<el-input v-model="slaveSubjectIdsStr" disabled >
					<template #append>
						<el-button
							:icon="Tickets"
							@click="subjectRelationDeleteDrawerVisible = true"
						/>
					</template>
				</el-input>
			</el-form-item>

			<el-form-item :label="t('module.subject.relaction.dialog.delete.label.type')">
				<el-input
					v-model="slaveSubjectTypesStr"
					disabled
				/>
			</el-form-item>
		</el-form>
		<template #footer>
			<span>
				<el-button @click="dialogVisible = false">
					{{ t('module.subject.relaction.dialog.delete.button.cancel') }}
				</el-button>
				<el-popconfirm :title="t('module.subject.relaction.dialog.delete.popconfirm.title')" @confirm="reqRemoveRelaction">
					<template #reference>
						<el-button type="primary" :loading="reqRemoveRelactionBtnLoading">
							{{t('module.subject.relaction.dialog.delete.button.confirm')}}
						</el-button>
					</template>
				</el-popconfirm>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
