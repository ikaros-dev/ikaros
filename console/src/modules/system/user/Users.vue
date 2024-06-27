<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import {WarningFilled} from '@element-plus/icons-vue';
import { ElRow,ElCol,ElButton,ElTable,ElTableColumn,ElSwitch,ElPopconfirm } from 'element-plus';
import { User } from '@runikaros/api-client';
import { onMounted, ref } from 'vue';

const users = ref<User[]>([])
const fetchUsers = async ()=>{
  const {data} = await apiClient.user.getUsers();
  users.value = data;
}

onMounted(fetchUsers)
</script>
<template>
  <div>

    <el-row>
      <el-col :span="24">
        <el-button >Add</el-button>
        {{ users }}
      </el-col>
    </el-row>

    <el-row>
      <el-col :span="24">
        <el-table :data="users"  size="large">
          <el-table-column prop="entity.id" label="ID" width="80"/>
          <el-table-column prop="entity.username" label="Username" width="120" />
          <el-table-column prop="entity.nickname" label="Nickname" />
          <el-table-column prop="entity.introduce" label="Iintroduce" />
          <el-table-column prop="entity.enable" label="Enable" >
            <template #default="scope">
              <el-switch v-model="scope.row.entity.enable" :disabled="scope.row.entity.id === 1"/>
            </template>
          </el-table-column>

          <el-table-column fixed="right" label="Operations" min-width="120">
            <template #default="scope">
              <el-button
              >
              Edit
              </el-button>
              <el-button type="primary" 
              >
              Details
              </el-button
              >
              <el-popconfirm
                  width="300"
                  confirm-button-text="Delete"
                  cancel-button-text="Cancel"
                  confirm-button-type="danger"
                  :icon="WarningFilled"
                  icon-color="red"
                  title="Are you sure to delete this role?"
              >
                <template #reference>
                  <el-button type="danger" :disabled="scope.row.entity.id === 1"
                  >Delete
                  </el-button
                  >
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
    </el-row>
  </div>
</template>
<style lang="scss" scoped></style>
