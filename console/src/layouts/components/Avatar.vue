<!-- 头部右边头像带下拉框 -->
<script setup lang="ts">
import { useLayoutStore } from '@/stores/layout';
import { useUserStore } from '@/stores/user';
import axios from 'axios';
import { AxiosError } from 'axios';
import { ElMessage } from 'element-plus';
import { useI18n } from 'vue-i18n';

const layoutStore = useLayoutStore();
const { t } = useI18n();
const userStore = useUserStore();

const avatarSrcUrl =
	'https://gss0.baidu.com/7Po3dSag_xI4khGko9WTAnF6hhy/zhidao/pic/item/e7cd7b899e510fb3f001a7d2db33c895d1430c5f.jpg';

const router = useRouter();
const toUserProfile = () => {
	const userProfileRoutePath = '/profile';
	router.push(userProfileRoutePath);
	layoutStore.updateCurrentActionIdByRoutePath(userProfileRoutePath);
};

const lougout = async () => {
	try {
		const url = `${import.meta.env.VITE_API_URL}/logout`;
		await axios.post(url, null, {
			withCredentials: true,
		});

		await userStore.fetchCurrentUser();

		// Reload page
		window.location.reload();
	} catch (e: unknown) {
		console.error('Failed to logout', e);

		if (e instanceof AxiosError) {
			if (/Network Error/.test(e.message)) {
				ElMessage.error(t('core.common.exception.network_error'));
				return;
			}

			if (e.response?.status === 403) {
				ElMessage.warning(t('core.login.operations.submit.toast_csrf'));
				return;
			}

			ElMessage.error(t('core.login.operations.submit.toast_failed'));
		} else {
			ElMessage.error(t('core.common.exception.unknown_error_with_title'));
		}
	}
};
</script>

<template>
	<el-dropdown>
		<el-avatar :size="55" :src="avatarSrcUrl" />
		<template #dropdown>
			<el-dropdown-menu>
				<el-dropdown-item @click="toUserProfile"> 个人中心 </el-dropdown-item>
				<el-dropdown-item @click="lougout">退出</el-dropdown-item>
			</el-dropdown-menu>
		</template>
	</el-dropdown>
</template>

<style lang="scss" scoped></style>
