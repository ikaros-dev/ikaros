<script setup lang="ts">
const currentLanguage = 'zh';
const handleCommand = (val) => {
	console.log('command', val);
};
const hiht = () => {
	alert('暂时不支持国际化，无法进行中英文切换，请等待后续版本更新。');
};
</script>

<template>
	<el-dropdown @command="handleCommand">
		<el-icon :size="40"><Location /></el-icon>
		<template #dropdown>
			<el-dropdown-menu>
				<el-dropdown-item command="zh" :disabled="currentLanguage === 'zh'"
					>中文</el-dropdown-item
				>
				<el-dropdown-item
					command="en"
					:disabled="currentLanguage === 'en'"
					@click="hiht"
					>English</el-dropdown-item
				>
			</el-dropdown-menu>
		</template>
	</el-dropdown>
</template>

<style lang="scss" scoped></style>
