<script setup lang="ts">
import { Tag, TagTypeEnum } from '@runikaros/api-client';
import { computed, ref } from 'vue';
import { ElDialog, ElForm, ElFormItem, ElInput, ElColorPicker, ElMessage, ElButton } from 'element-plus';
import { apiClient } from '@/utils/api-client';


const props = withDefaults(
    defineProps<{
        visible: boolean;
        tag: Tag;
    }>(),
    {
        visible: false,
        tag: undefined,
    }
);

const emit = defineEmits<{
    // eslint-disable-next-line no-unused-vars
    (event: 'update:visible', visible: boolean): void;
    // eslint-disable-next-line no-unused-vars
    (event: 'update:tag', tag: Tag): void;
    // eslint-disable-next-line no-unused-vars
    (event: 'close'): void;
    // eslint-disable-next-line no-unused-vars
    (event: 'closeWithTag', tag: Tag): void;
}>();

const dialogVisible = computed({
    get() {
        return props.visible;
    },
    set(value) {
        emit('update:visible', value);
    },
});

const dialogTag = computed({
    get() {
        return props.tag;
    },
    set(value) {
        emit('update:tag', value);
    },
});


const updateTagRequesting = ref(false);
const onTagSubmit = async () => {
    updateTagRequesting.value = true;
    // 删除旧的标签
    if (props.tag.id) {
        await apiClient.tag.removeTagById({ id: props.tag.id as number });
    } else {
        await apiClient.tag.removeTagByCondition({
            type: props.tag.type,
            masterId: props.tag.masterId,
            name: props.tag.name
        })
    }
    // 创建新的标签
    const {data} = await apiClient.tag.createTag({
        tag: {
            type: TagTypeEnum.Subject,
            masterId: dialogTag.value.masterId,
            name: dialogTag.value.name,
            userId: dialogTag.value.userId,
            color: dialogTag.value.color,
        }
    })

    dialogTag.value = data;
    updateTagRequesting.value = false;
    ElMessage.success("更新标签[" + dialogTag.value.name + "]成功。");
    // 关闭并提交事件
    dialogVisible.value = false;
}
</script>
<template>
    <el-dialog v-model="dialogVisible" title="编辑标签">
        <el-form label-width="60px">
            <el-form-item label="名称" prop="name">
                <el-input v-model="dialogTag.name" placeholder="标签名称" size="large" />
            </el-form-item>
            <el-form-item label="颜色" prop="color">
                <el-color-picker v-model="dialogTag.color" size="large" />
            </el-form-item>
        </el-form>
        <template #footer>
            <span>
                <el-button plain :loading="updateTagRequesting" @click="onTagSubmit">
                    提交
                </el-button>
            </span>
        </template>
    </el-dialog>
</template>
<style lang="scss" scoped></style>
