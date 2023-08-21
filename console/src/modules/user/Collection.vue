<script setup lang="ts">
import { ref } from 'vue';
import { ElRow, ElCol, ElFormItem, ElSelect, ElOption } from 'element-plus';

// eslint-disable-next-line no-unused-vars
const findSubjectCollection = ref({
	page: 1,
	size: 12,
	type: undefined,
	isPrivate: undefined,
});

const fetchCollections = async () => {};
</script>

<template>
	<el-row :gutter="10">
		<el-col :xs="24" :sm="24" :md="24" :lg="20" :xl="20">
			<el-form :inline="true" :model="findSubjectCollection">
				<el-row :gutter="1">
					<el-col :xs="24" :sm="24" :md="24" :lg="5" :xl="5">
						<el-form-item label="私有" style="width: 95%">
							<el-select
								v-model="findSubjectCollection.isPrivate"
								clearable
								@change="fetchCollections"
							>
								<el-option label="是" value="true" />
								<el-option label="非" value="false" />
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :xs="24" :sm="24" :md="24" :lg="5" :xl="5">
						<el-form-item label="所属类型" style="width: 95%">
							<el-select
								v-model="findSubjectCollection.type"
								clearable
								@change="fetchCollections"
							>
								<el-option label="动漫" value="ANIME" />
								<el-option label="漫画" value="COMIC" />
								<el-option label="游戏" value="GAME" />
								<el-option label="音声" value="MUSIC" />
								<el-option label="小说" value="NOVEL" />
								<el-option label="三次元" value="REAL" />
								<el-option label="其它" value="OTHER" />
							</el-select>
						</el-form-item>
					</el-col>
				</el-row>
			</el-form>
		</el-col>
	</el-row>
</template>

<style lang="scss" scoped></style>
