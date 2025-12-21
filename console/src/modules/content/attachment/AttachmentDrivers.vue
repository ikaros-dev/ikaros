<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { AttachmentDriver } from '@runikaros/api-client';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';


import {
	FolderAdd,
} from '@element-plus/icons-vue';
import {
	ElRow,
	ElCol,
	ElButton,
  ElTable,
  ElTableColumn,
  ElSwitch,
  ElPopconfirm,
} from 'element-plus';


// eslint-disable-next-line no-unused-vars
const { t } = useI18n();


const attDrivers = ref<AttachmentDriver[]>()
const fetchAttDrivers = async() => {
    const { data } = await apiClient.attachmentDriver.listDriversByCondition({
      page: 1,
      size: 10
    })
    attDrivers.value = data.items as AttachmentDriver[]
    console.debug('attDrivers', attDrivers.value)
}


const dialogDriverAddVisible = ref(false)

const isEnableButtonLoading = ref(false)
const chageAttDriverEnable = async (rowIndex, attDriver) => {
  // console.debug('chageAttDriverEnable', 'rowIndex', rowIndex, 'attDriver', attDriver)
  isEnableButtonLoading.value = true
  
  if (attDriver.enable) {
      await apiClient.attachmentDriver.enableDriver({
        id: attDriver.id
      })
      .finally(()=>{
        isEnableButtonLoading.value = false
      })
  } else {
      await apiClient.attachmentDriver.enableDriver1({
        id: attDriver.id
      })
      .finally(()=>{
        isEnableButtonLoading.value = false
      })
  }
  await fetchAttDrivers()
  isEnableButtonLoading.value = false
  
}


const deleteAttDriver = async (rowIndex, attDriver) => {
  console.debug('deleteAttDriver', 'rowIndex', rowIndex, 'attDriver', attDriver)
}



onMounted(fetchAttDrivers);
</script>
<template>
  <el-row>
		<el-col :span="24">
      <el-button :icon="FolderAdd" @click="dialogDriverAddVisible = true">
        {{ t('module.attachment.driver.button.new') }}
			</el-button>
    </el-col>
  </el-row>

  <!-- <hr /> -->

  <el-row>
		<el-col :span="24">
      <el-table :data="attDrivers" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="40" />
        <el-table-column prop="type" :label="t('module.attachment.driver.table.colum.label.type')"  width="80" />
        <el-table-column prop="name" :label="t('module.attachment.driver.table.colum.label.name')" width="80" />
        <el-table-column prop="mount_name" :label="t('module.attachment.driver.table.colum.label.mount_name')" width="120" />
        <el-table-column prop="remote_path" :label="t('module.attachment.driver.table.colum.label.mount_remote_path')" />
        <el-table-column prop="comment" :label="t('module.attachment.driver.table.colum.label.comment')" />
        <el-table-column prop="enable" :label="t('module.attachment.driver.table.colum.label.enable')" width="80" >
          <template #default="scope">
            <el-switch v-model="scope.row.enable" disabled />
          </template>
        </el-table-column>
        <el-table-column :label="t('module.attachment.driver.table.colum.label.operations')"  width="200">
          <template #default="scope">
            <el-popconfirm
              :title="
              scope.row.enable ?
              t('module.attachment.driver.table.colum.label.operation.popconfirm.disable_title')
              :
              t('module.attachment.driver.table.colum.label.operation.popconfirm.enable_title')
              "
              :confirm-button-text="t('common.button.confirm')"
              :cancel-button-text="t('common.button.cancel')"
              confirm-button-type="danger"
              width="350px"
              @confirm="chageAttDriverEnable(scope.$index, scope.row)"
            >
              <template #reference>
                <el-button :loading="isEnableButtonLoading">
                  <span v-if="scope.row.enable">
                    {{ t('module.attachment.driver.table.colum.label.operation.disable') }}
                  </span>
                  <span v-else>
                    {{ t('module.attachment.driver.table.colum.label.operation.enable') }}
                  </span>
                </el-button>
              </template>
            </el-popconfirm>


            <el-popconfirm
              :title="t('module.attachment.driver.table.colum.label.operation.popconfirm.title')"
              :confirm-button-text="t('common.button.confirm')"
              :cancel-button-text="t('common.button.cancel')"
              confirm-button-type="danger"
              width="350px"
              @confirm="deleteAttDriver(scope.$index, scope.row)"
            >
              <template #reference>
                <el-button type="danger">
                  {{ t('module.attachment.driver.table.colum.label.operation.delete') }}
                </el-button>
              </template>
            </el-popconfirm>

            
          </template>
        </el-table-column>  
      </el-table>
    </el-col>
  </el-row>

  
  <el-row>
		<el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
      <!-- {{ attDrivers }} -->
    </el-col>
  </el-row>
</template>
<style lang="scss" scoped>
</style>
