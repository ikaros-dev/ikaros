<script setup lang="ts">
import { ElRow, ElCol,ElButton,ElTable,ElTableColumn } from 'element-plus';
import { ref } from 'vue';


const roles = ref([
  {
    id: 0,
    name: "MASTER"
  },
  {
    id: 1,
    name: "FRIEND"
  },
])
</script>
<template>
  <div>
      <el-row>
        <el-col :span="24">
          <el-button>Refresh</el-button>
          <el-button>Add</el-button>
        </el-col>
      </el-row>
      
      <el-row>
        <el-col :span="24">
          <el-table :data="roles" size="large">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="Name"  />
          </el-table>
        </el-col>
      </el-row>
  </div>
</template>
<style lang="scss" scoped>
</style>
