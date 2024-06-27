<script setup lang="ts">
import {apiClient} from '@/utils/api-client';
import {Role} from '@runikaros/api-client';
import {onMounted, ref} from 'vue';
import {ElButton, ElCol, ElInput, ElMessage, ElRow, ElTable, ElTableColumn} from 'element-plus';


const roles = ref<Role[]>([])

const fetchRoles = async () => {
  const {data} = await apiClient.role.getRoles();
  roles.value = data;
}

const editRow = ref(false);
const editRowId = ref(-1);
const editBtn = ref({
  type: 'primary' as 'primary' | 'success',
  text: 'Edit',
  loading: false
})

const changeEditBtnStatus = async (id) => {
  if (editRowId.value === -1) {
    editRowId.value = id;
    editRow.value = true;
    editBtn.value.type = 'success' as 'success';
    editBtn.value.text = 'Submit'
  } else {
    var role = roles.value.find(obj => obj.id === editRowId.value);
    editBtn.value.loading = true
    await apiClient.role.updateRole({
      role: role as Role
    })
        .then(() => {
          ElMessage.success('Update Role success for id=' + editRowId.value);
          editRowId.value = -1
          editBtn.value.type = 'primary'
          editBtn.value.text = 'Edit'
        })
        .finally(() => {
          editBtn.value.loading = false;
        })
  }
}


onMounted(fetchRoles)
</script>
<template>
  <div>
    <el-row>
      <el-col :span="24">
        <el-button>Add</el-button>
        {{ roles }}
      </el-col>
    </el-row>

    <el-row>
      <el-col :span="24">
        <el-table :data="roles" size="large">
          <el-table-column prop="id" label="ID" width="80"/>
          <el-table-column prop="name" label="Name" width="120">
            <template #default="scope">
              <span v-if="editRow && editRowId === scope.row.id">
                <el-input v-model="scope.row.name"/>
              </span>
              <span v-else>
                {{ scope.row.name }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="Description">
            <template #default="scope">
              <span v-if="editRow && editRowId === scope.row.id">
                <el-input v-model="scope.row.description"/>
              </span>
              <span v-else>
                {{ scope.row.description }}
              </span>
            </template>
          </el-table-column>
          <el-table-column fixed="right" label="Operations" min-width="120">
            <template #default="scope">
              <el-button
                  :loading="editBtn.loading"
                  :type="(scope.row.id === editRowId) ? editBtn.type : 'primary'"
                  @click="changeEditBtnStatus(scope.row.id)">
                {{ (scope.row.id === editRowId) ? editBtn.text : 'Edit' }}
              </el-button>
              <el-button type="danger">Delete</el-button>
              <el-button type="primary">Authorities</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
    </el-row>
  </div>
</template>
<style lang="scss" scoped></style>
