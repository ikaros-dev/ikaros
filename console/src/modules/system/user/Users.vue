<script setup lang="ts">
import {apiClient} from '@/utils/api-client';
import {WarningFilled} from '@element-plus/icons-vue';
import {User, UserEntity} from '@runikaros/api-client';
import {onMounted, reactive, ref} from 'vue';
import {
  ElButton,
  ElCol,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElPopconfirm,
  ElRow,
  ElSwitch,
  ElTable,
  ElTableColumn,
  FormInstance,
  FormRules
} from 'element-plus';

const users = ref<User[]>([])
const fetchUsers = async ()=>{
  const {data} = await apiClient.user.getUsers();
  users.value = data;
}

const user = ref<UserEntity>({});
const userDetailsDialogVisible = ref(false)
const subitUserFrom = async (formEl: FormInstance | undefined) => {
  console.debug('formEl', formEl)
  if (!formEl) return;
  await formEl.validate(async (valid, fields) => {
    if (valid) {
      if (user.value.id && user.value.id >= 0) {
        await apiClient.user.updateUser({
          updateUserRequest: {
            username: user.value.username as string,
            nickname: user.value.nickname as string,
            introduce: user.value.introduce as string,
          }
        })
        ElMessage.success('Update user success for username=' + user.value.username)
      } else {
        await apiClient.user.postUser({
          createUserReqParams: {
            enabled: true,
            username: user.value.username as string,
            password: user.value.password as string,
          }
        })
        ElMessage.success('Create user success for username=' + user.value.username)
      }
      user.value = {};
      userDetailsDialogVisible.value = false;
      await fetchUsers();
    } else {
      console.log('error submit!', fields);
      ElMessage.error('Please check if any necessary items are missing');
    }
  });
}
const userElFormRef = ref<FormInstance>()
const userFormRules = reactive<FormRules>({
  username: [
    {
      required: true,
      message: 'Username is required.',
      trigger: 'blur',
    },
    {
      min: 1,
      max: 100,
      message: 'Username length min=1 and max=100.',
      trigger: 'blur',
    },
  ],
})

const doEditUser = (row: User) => {
  user.value = row.entity as UserEntity;
  userDetailsDialogVisible.value = true;
}

const changeUserEnableStatus = async (userEntity: UserEntity) => {
  console.debug('userEntity', userEntity);
  await apiClient.user.updateUser({
    updateUserRequest: {
      username: userEntity.username as string,
      enable: userEntity.enable as boolean
    }
  })
  ElMessage.success('Update user enable status success for username=' + userEntity.username);
  await fetchUsers();
}

const doDeleteUser = async (userId) => {
  console.debug('userId', userId)
  if (!userId) return;
  await apiClient.user.deleteById({id: userId});
  ElMessage.success('Delete user success for userId=' + userId);
  await fetchUsers();
}

onMounted(fetchUsers)
</script>
<template>
  <div>
    <el-dialog v-model="userDetailsDialogVisible" :title="'User ' + ((user.id && user.id >= 0)?'Edit':'Create') "
               width="500" @closed="user = {}">
      <el-form ref="userElFormRef" :model="user" :rules="userFormRules">
        <el-form-item v-if="user.id && user.id >= 0" label="ID">
          <el-input v-model="user.id" disabled/>
        </el-form-item>
        <el-form-item label="Username">
          <el-input v-model="user.username"/>
        </el-form-item>
        <el-form-item v-if="!(user.id)" label="Password">
          <el-input v-model="user.password" show-password/>
        </el-form-item>
        <el-form-item v-if="user.id && user.id >= 0" label="Nickname">
          <el-input v-model="user.nickname"/>
        </el-form-item>
        <el-form-item v-if="user.id && user.id >= 0" label="Introduce">
          <el-input v-model="user.introduce" type="textarea" :rows="2"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <div>
          <el-button @click="userDetailsDialogVisible = false">Cancel</el-button>
          <el-button type="primary" @click="subitUserFrom(userElFormRef)"> Submit</el-button>
        </div>
      </template>
    </el-dialog>

    <el-row>
      <el-col :span="24">
        <el-button @click="userDetailsDialogVisible = true">Add</el-button>
      </el-col>
    </el-row>

    <el-row>
      <el-col :span="24">
        <el-table :data="users"  size="large">
          <el-table-column prop="entity.id" label="ID" width="80"/>
          <el-table-column prop="entity.username" label="Username" width="120" />
          <el-table-column prop="entity.nickname" label="Nickname" />
          <el-table-column prop="entity.introduce" label="Introduce"/>
          <el-table-column prop="entity.enable" label="Enable" >
            <template #default="scope">
              <el-switch v-model="scope.row.entity.enable" :disabled="scope.row.entity.id === 1"
                         @change="changeUserEnableStatus(scope.row.entity)"/>
            </template>
          </el-table-column>

          <el-table-column fixed="right" label="Operations" min-width="120">
            <template #default="scope">
              <el-button type="primary" @click="doEditUser(scope.row)">
              Edit
              </el-button>
              <el-popconfirm
                  width="300"
                  confirm-button-text="Delete"
                  cancel-button-text="Cancel"
                  confirm-button-type="danger"
                  :icon="WarningFilled"
                  icon-color="red"
                  title="Are you sure to delete this user?"
                  @confirm="doDeleteUser(scope.row.entity.id)"
              >
                <template #reference>
                  <el-button type="danger" :disabled="scope.row.entity.id === 1">
                    Delete
                  </el-button>
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
