<script setup lang="ts">
import { computed } from 'vue';
import {
	ElDescriptions,
	ElDescriptionsItem,
	ElDrawer,
	ElTag,
} from 'element-plus';
import { formatDate } from '@/utils/date';
import { useI18n } from 'vue-i18n';
import { episodeGroupLabelMap } from '@/modules/common/constants';

const { t } = useI18n();

const props = withDefaults(
	defineProps<{
		visible: boolean;
		rule: any;
	}>(),
	{
		visible: false,
		rule: null,
	}
);

const emit = defineEmits<{
	(event: 'update:visible', visible: boolean): void;
	(event: 'close'): void;
}>();

const drawerVisible = computed({
	get() {
		return props.visible;
	},
	set(value) {
		emit('update:visible', value);
	},
});

const getEpGroupLabel = (group: string) => {
	return episodeGroupLabelMap.get(group) || group || '-';
};
</script>

<template>
	<el-drawer
		v-model="drawerVisible"
		:title="t('module.regular.detail.title')"
		:size="500"
		@close="emit('close')"
	>
		<template v-if="rule">
			<el-descriptions :column="1" border direction="vertical">
				<el-descriptions-item :label="t('module.regular.dialog.label.name')">
					{{ rule.name }}
				</el-descriptions-item>
				<el-descriptions-item :label="t('module.regular.dialog.label.regex')">
					<el-tag type="info" effect="plain" style="font-family: monospace">
						{{ rule.regex }}
					</el-tag>
				</el-descriptions-item>
				<el-descriptions-item :label="t('module.regular.dialog.label.epGroup')">
					{{ getEpGroupLabel(rule.epGroup) }}
				</el-descriptions-item>
				<el-descriptions-item :label="t('module.regular.dialog.label.sequence')">
					{{ rule.sequence ?? '-' }}
				</el-descriptions-item>
				<el-descriptions-item :label="t('module.regular.dialog.label.priority')">
					{{ rule.priority }}
				</el-descriptions-item>
				<el-descriptions-item :label="t('module.regular.dialog.label.description')">
					{{ rule.description || '-' }}
				</el-descriptions-item>
				<el-descriptions-item :label="t('module.regular.dialog.label.enabled')">
					<el-tag :type="rule.enabled ? 'success' : 'info'">
						{{ rule.enabled ? '启用' : '禁用' }}
					</el-tag>
				</el-descriptions-item>
				<el-descriptions-item :label="t('module.regular.detail.label.createTime')">
					{{ rule.createTime ? formatDate(rule.createTime) : '-' }}
				</el-descriptions-item>
				<el-descriptions-item :label="t('module.regular.detail.label.updateTime')">
					{{ rule.updateTime ? formatDate(rule.updateTime) : '-' }}
				</el-descriptions-item>
			</el-descriptions>
		</template>
		<template v-else>
			{{ t('module.regular.detail.noData') }}
		</template>
	</el-drawer>
</template>

<style lang="scss" scoped></style>
