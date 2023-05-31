<script setup lang="ts">
const currentLanguage = 'zh';
const handleCommand = (val) => {
	console.log('command', val);
};
</script>

<template>
	<el-dropdown @command="handleCommand">
		<el-icon :size="40"><Operation /></el-icon>
		<template #dropdown>
			<el-dropdown-menu>
				<el-dropdown-item command="zh" :disabled="currentLanguage === 'zh'"
					>中文</el-dropdown-item
				>
				<el-dropdown-item command="en" :disabled="currentLanguage === 'en'"
					>English</el-dropdown-item
				>
			</el-dropdown-menu>
		</template>
	</el-dropdown>
</template>

<style lang="scss" scoped></style>
