<script setup lang="ts">
import { ElRow,ElCol,ElButton,ElTable,ElTableColumn } from 'element-plus';
</script>
<template>
  <div>

    <el-row>
      <el-col :span="24">
        <el-button >Add</el-button>
      </el-col>
    </el-row>

    <el-row>
      <el-col :span="24">
        <el-table  size="large">
          <el-table-column prop="id" label="ID" width="80"/>
          <el-table-column prop="name" label="Name" width="120" />
          <el-table-column prop="description" label="Description" />
          <el-table-column fixed="right" label="Operations" min-width="120">
            <template #default="scope">
              <el-button
              >
              Edit
              </el-button>
              <el-button type="primary" :disabled="scope.row.id === 1"
              >
              Details
              </el-button
              >
            </template>
          </el-table-column>
        </el-table>
      </el-col>
    </el-row>
  </div>
</template>
<style lang="scss" scoped></style>
