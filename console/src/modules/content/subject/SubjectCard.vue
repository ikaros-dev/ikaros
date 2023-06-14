<script setup lang="ts">
const tmpCoverSrc =
	'https://lain.bgm.tv/r/400/pic/cover/l/75/e3/2617_7c8Cr.jpg';
</script>

<template>
	<el-card shadow="hover" class="container" :body-style="{ padding: '0px' }">
		<template #header>
			<div class="card-header">
				<span>天降之物 </span>
				<span class="grey">そらのおとしもの</span>
			</div>
		</template>
		<span>
			<img :src="tmpCoverSrc" style="width: 100%; border-radius: 5px" />
		</span>
	</el-card>
</template>

<style lang="scss" scoped>
.container {
	margin: 5px 0;
	border-radius: 5px;
	// border: 1px solid rebeccapurple;
	cursor: pointer;
}

.card-header {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	height: 15px;
	.grey {
		font-size: 10px;
		color: #999;
	}
}
</style>
