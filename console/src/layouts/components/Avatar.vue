<!-- 头部右边头像带下拉框 -->
<script setup lang="ts">
import { useLayoutStore } from '@/stores/layout';
import { useUserStore } from '@/stores/user';
import axios from 'axios';
import { AxiosError } from 'axios';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import {
	ElDropdown,
	ElDropdownItem,
	ElDropdownMenu,
	ElAvatar,
} from 'element-plus';

const layoutStore = useLayoutStore();
const { t } = useI18n();
const userStore = useUserStore();

const avatarSrcUrl = 'https://ikaros.run/logo.png';

const router = useRouter();
const toUserProfile = () => {
	const userProfileRoutePath = '/profile';
	router.push(userProfileRoutePath);
	layoutStore.updatecurrentActivePathByRoutePath(userProfileRoutePath);
};

const confirmLogout = async () => {
	ElMessageBox.confirm('您确定要登出吗? ', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning',
	})
		.then(() => {
			lougout();
			window.location.reload();
		})
		.catch(() => {
			ElMessage({
				type: 'info',
				message: '已取消',
			});
		});
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
				<el-dropdown-item @click="confirmLogout">退出</el-dropdown-item>
			</el-dropdown-menu>
		</template>
	</el-dropdown>
</template>

<style lang="scss" scoped></style>
