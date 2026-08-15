import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => ({
	copyValue2Clipboard: vi.fn(),
	get: vi.fn(),
	post: vi.fn(),
	toDataURL: vi.fn(),
}));

vi.mock('axios', () => ({
	default: {
		get: mocks.get,
		post: mocks.post,
	},
}));
vi.mock('qrcode', () => ({ default: { toDataURL: mocks.toDataURL } }));
vi.mock('@/stores/user', () => ({
	useUserStore: () => ({ jwtToken: 'jwt-token' }),
}));
vi.mock('@/utils/string-util', () => ({
	copyValue2Clipboard: mocks.copyValue2Clipboard,
}));
vi.mock('vue-i18n', () => ({ useI18n: () => ({ t: (key: string) => key }) }));

import { CopyDocument } from '@element-plus/icons-vue';
import { ElButton, ElMessage } from 'element-plus';
import TotpManagement from './TotpManagement.vue';

const mountTotpManagement = () => mount(TotpManagement);

describe('TotpManagement', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		mocks.get.mockResolvedValue({ data: { enabled: false }, status: 200 });
		mocks.copyValue2Clipboard.mockResolvedValue(undefined);
		mocks.post.mockResolvedValue({
			data: {
				secret: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567',
				otpAuthUri: 'otpauth://totp/ikaros',
			},
			status: 200,
		});
		mocks.toDataURL.mockResolvedValue('data:image/png;base64,qrcode');
	});

	it('使用完整密钥、文本复制按钮和约定的操作按钮样式', async () => {
		const wrapper = mountTotpManagement();
		await flushPromises();
		await wrapper.getComponent(ElButton).trigger('click');
		await flushPromises();

		expect(wrapper.get('.m3-totp-page__secret-value').text()).toBe(
			'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
		);

		const buttons = wrapper.findAllComponents(ElButton);
		const copyButtonComponent = buttons.find((button) =>
			button.classes('m3-totp-page__copy-button')
		);
		expect(copyButtonComponent).toBeDefined();
		if (!copyButtonComponent) {
			throw new Error('未找到复制密钥按钮');
		}
		expect(copyButtonComponent.props('icon')).toBe(CopyDocument);
		expect(copyButtonComponent.props('text')).toBe(true);
		expect(copyButtonComponent.props('circle')).toBe(true);
		expect(copyButtonComponent.find('.el-icon svg').exists()).toBe(true);
		const successMessage = vi.spyOn(ElMessage, 'success');
		await copyButtonComponent.trigger('click');
		expect(mocks.copyValue2Clipboard).toHaveBeenCalledWith(
			'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
		);
		expect(successMessage).toHaveBeenCalledWith('密钥已复制');

		const actionButtons = buttons.slice(-2);
		expect(actionButtons[0].text()).toBe('取消');
		expect(actionButtons[0].props('type')).toBe('primary');
		expect(actionButtons[1].text()).toBe('验证并启用');
		expect(actionButtons[1].props('type')).toBe('');
		expect(actionButtons[1].props('disabled')).toBe(true);
	});

	it('复制失败时显示错误提示', async () => {
		mocks.copyValue2Clipboard.mockRejectedValue(new Error('复制失败'));
		const errorMessage = vi.spyOn(ElMessage, 'error');
		const wrapper = mountTotpManagement();
		await flushPromises();
		await wrapper.getComponent(ElButton).trigger('click');
		await flushPromises();
		await wrapper.getComponent('.m3-totp-page__copy-button').trigger('click');

		expect(errorMessage).toHaveBeenCalledWith('复制失败');
	});
});
