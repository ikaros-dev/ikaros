<script setup lang="ts">
import {computed, ref} from 'vue';
import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElSelect,
} from 'element-plus';
import {Tickets} from '@element-plus/icons-vue';
import SubjectSelectDrawer from './SubjectSelectDrawer.vue';
import {apiClient} from '@/utils/api-client';
import {useI18n} from 'vue-i18n';

const { t } = useI18n();

const props = withDefaults(
	defineProps<{
		visible: boolean;
		masterSubjectId: number;
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
const slaveSubjectIdsStr = ref('[]');
const selectSubjectReactionType = ref();

const subjectSelectDrawerVisible = ref(false);

const onSelectionsChange = (set) => {
	let arr: number[] = [];
	set.forEach((v) => arr.push(v));
	slaveSubjectIdsStr.value = JSON.stringify(arr);
};

const reqCreateRelactionBtnLoading = ref(false);
const reqCreateRelaction = async () => {
	if (slaveSubjectIdsStr.value === '[]') {
		ElMessage.warning(
			t('module.subject.relaction.dialog.post.message.validate-fail')
		);
		return;
	}
	reqCreateRelactionBtnLoading.value = true;
	await apiClient.subjectRelation.createSubjectRelation({
		subjectRelation: {
			subject: props.masterSubjectId as number,
			relation_subjects: JSON.parse(slaveSubjectIdsStr.value),
			relation_type: selectSubjectReactionType.value,
		},
	});
	ElMessage.success(
		t('module.subject.relaction.dialog.post.message.add-success')
	);
	reqCreateRelactionBtnLoading.value = false;
	onClose();
};
</script>

<template>
	<subject-select-drawer
		v-model:visible="subjectSelectDrawerVisible"
    :filter="[props.masterSubjectId as number]"
		@selections-change="onSelectionsChange"
	/>
	<el-dialog
		v-model="dialogVisible"
		style="width: 40%"
		:title="t('module.subject.relaction.dialog.post.title')"
		@close="onClose"
	>
		<el-form label-width="100px" style="max-width: 460px">
			<el-form-item
				:label="t('module.subject.relaction.dialog.post.label.master-subject')"
			>
				<el-input disabled :value="props.masterSubjectId" />
			</el-form-item>
			<el-form-item
				:label="t('module.subject.relaction.dialog.post.label.slave-subject')"
			>
				<el-input
					v-model="slaveSubjectIdsStr"
					disabled
					:placeholder="
						t('module.subject.relaction.dialog.post.placeholder.slave-subject')
					"
				>
					<template #append>
						<el-button
							:icon="Tickets"
							@click="subjectSelectDrawerVisible = true"
						/>
					</template>
				</el-input>
			</el-form-item>
			<el-form-item
				:label="t('module.subject.relaction.dialog.post.label.type')"
			>
				<el-select v-model="selectSubjectReactionType" clearable>
					<el-option
						:label="t('module.subject.relaction.dialog.post.type.after')"
						value="AFTER"
					/>
					<el-option
						:label="t('module.subject.relaction.dialog.post.type.before')"
						value="BEFORE"
					/>
					<el-option
						:label="
							t('module.subject.relaction.dialog.post.type.same-worldview')
						"
						value="SAME_WORLDVIEW"
					/>
					<el-option
						:label="t('module.subject.relaction.dialog.post.type.ost')"
						value="ORIGINAL_SOUND_TRACK"
					/>
					<el-option
						:label="t('module.subject.relaction.dialog.post.type.anime')"
						value="ANIME"
					/>
					<el-option
						:label="t('module.subject.relaction.dialog.post.type.comic')"
						value="COMIC"
					/>
					<el-option
						:label="t('module.subject.relaction.dialog.post.type.game')"
						value="GAME"
					/>
					<el-option
						:label="t('module.subject.relaction.dialog.post.type.music')"
						value="MUSIC"
					/>
					<el-option
						:label="t('module.subject.relaction.dialog.post.type.novel')"
						value="NOVEL"
					/>
					<el-option
						:label="t('module.subject.relaction.dialog.post.type.real')"
						value="REAL"
					/>
					<el-option
						:label="t('module.subject.relaction.dialog.post.type.other')"
						value="OTHER"
					/>
				</el-select>
			</el-form-item>
		</el-form>
		<template #footer>
			<span>
				<el-button @click="dialogVisible = false">
					{{ t('module.subject.relaction.dialog.post.button.cancel') }}
				</el-button>
				<el-button
					type="primary"
					:loading="reqCreateRelactionBtnLoading"
					@click="reqCreateRelaction"
				>
					{{ t('module.subject.relaction.dialog.post.button.confirm') }}
				</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<style lang="scss" scoped></style>
