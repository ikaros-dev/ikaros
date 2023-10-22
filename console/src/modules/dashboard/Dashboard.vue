<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { onMounted } from 'vue';
import { ref } from 'vue';
import { Files, Tickets, Postcard, User } from '@element-plus/icons-vue';
import { ElRow, ElCol, ElCard, ElIcon } from 'element-plus';

const actuatorInfo = ref();
const fetchActuatorInfo = async () => {
	const { data } = await apiClient.actuator.info();
	actuatorInfo.value = data;
};

onMounted(fetchActuatorInfo);
</script>

<template>
	<el-row :gutter="10">
		<el-col :xs="24" :sm="24" :md="12" :lg="6" :xl="6">
			<el-card shadow="hover">
				<el-row>
					<el-col :span="6">
						<el-icon size="50"><Files /></el-icon>
					</el-col>
					<el-col :span="18">
						<el-row>
							<el-col :span="24">
								<span style="font-size: x-small; color: gray">附件</span>
							</el-col>
							<br />
							<el-col :span="24">
								<span style="font-size: xx-large; font-weight: 600">
									{{ actuatorInfo?.attachment?.total }}
								</span>
							</el-col>
						</el-row>
					</el-col>
				</el-row>
			</el-card>
		</el-col>
		<el-col :xs="24" :sm="24" :md="12" :lg="6" :xl="6">
			<el-card shadow="hover">
				<el-row>
					<el-col :span="6">
						<el-icon size="50"><Tickets /></el-icon>
					</el-col>
					<el-col :span="18">
						<el-row>
							<el-col :span="24">
								<span style="font-size: x-small; color: gray">条目</span>
							</el-col>
							<br />
							<el-col :span="24">
								<span style="font-size: xx-large; font-weight: 600">
									{{ actuatorInfo?.subject?.total }}
								</span>
							</el-col>
						</el-row>
					</el-col>
				</el-row>
			</el-card>
		</el-col>
		<el-col :xs="24" :sm="24" :md="12" :lg="6" :xl="6">
			<el-card shadow="hover">
				<el-row>
					<el-col :span="6">
						<el-icon size="50"><Postcard /></el-icon>
					</el-col>
					<el-col :span="18">
						<el-row>
							<el-col :span="24">
								<span style="font-size: x-small; color: gray">收藏</span>
							</el-col>
							<br />
							<el-col :span="24">
								<span style="font-size: xx-large; font-weight: 600">
									{{ actuatorInfo?.character?.total }}
								</span>
							</el-col>
						</el-row>
					</el-col>
				</el-row>
			</el-card>
		</el-col>
		<el-col :xs="24" :sm="24" :md="12" :lg="6" :xl="6">
			<el-card shadow="hover">
				<el-row>
					<el-col :span="6">
						<el-icon size="50"><User /></el-icon>
					</el-col>
					<el-col :span="18">
						<el-row>
							<el-col :span="24">
								<span style="font-size: x-small; color: gray">在看</span>
							</el-col>
							<br />
							<el-col :span="24">
								<span style="font-size: xx-large; font-weight: 600">
									{{ actuatorInfo?.person?.total }}
								</span>
							</el-col>
						</el-row>
					</el-col>
				</el-row>
			</el-card>
		</el-col>
	</el-row>
</template>

<style lang="scss" scoped></style>
