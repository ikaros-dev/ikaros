<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import { ElButton } from 'element-plus';
</script>

<template>
	<el-button type="danger" plain :icon="Refresh" class="refresh-button">
		<slot>刷新</slot>
	</el-button>
</template>
