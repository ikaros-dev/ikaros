import { http } from "@/utils/http";

export type UserResult = {
  success: boolean;
  data: {
    /** 头像 */
    avatar: string;
    /** 用户名 */
    username: string;
    /** 昵称 */
    nickname: string;
    /** 当前登录用户的角色 */
    roles: Array<string>;
    /** 按钮级别权限 */
    permissions: Array<string>;
    /** `token` */
    accessToken: string;
    /** 用于调用刷新`accessToken`的接口时所需的`token` */
    refreshToken: string;
    /** `accessToken`的过期时间（格式'xxxx/xx/xx xx:xx:xx'） */
    expires: Date;
  };
};

export type RefreshTokenResult = {
  success: boolean;
  data: {
    /** `token` */
    accessToken: string;
    /** 用于调用刷新`accessToken`的接口时所需的`token` */
    refreshToken: string;
    /** `accessToken`的过期时间（格式'xxxx/xx/xx xx:xx:xx'） */
    expires: Date;
  };
};

/** 登录 */
type AuthenticationView = { userId: string; sessionId: string; sessionToken: string; expiresAt: string; user?: { username?: string; displayName?: string; roleCodes?: string[] } };

const toUserResult = (result: AuthenticationView): UserResult => {
  return { success: true, data: { avatar: "", username: result.user?.username || "", nickname: result.user?.displayName || "", roles: result.user?.roleCodes || [], permissions: [], accessToken: result.sessionToken, refreshToken: result.sessionToken, expires: new Date(result.expiresAt) } };
};

export const getLogin = async (data?: object): Promise<UserResult> => {
  const result = await http.request<AuthenticationView>("post", "/auth/login", { data });
  return toUserResult(result);
};

/** 注册 */
export const registerUser = async (data: object): Promise<UserResult> => {
  const result = await http.request<AuthenticationView>("post", "/auth/register", { data });
  return toUserResult(result);
};

/** 刷新`token` */
export const refreshTokenApi = (data?: object) => {
  return http.request<RefreshTokenResult>("post", "/refresh-token", { data });
};
