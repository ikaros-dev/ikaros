const STORAGE_KEY = "ikaros-verification-grant";

/** 校验有效期安全边界，避免边界处被服务端判为过期。 */
const SKEW_MS = 30_000;

let currentGrant = "";
let expiresAt = 0;

function readExpiry(grant: string): number {
  const payload = grant.split(".")[1];
  if (!payload) return 0;
  try {
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized + "=".repeat((4 - (normalized.length % 4)) % 4);
    const claims = JSON.parse(atob(padded)) as { exp?: number };
    return typeof claims.exp === "number" ? claims.exp * 1000 : 0;
  } catch {
    return 0;
  }
}

function isValid(skewMs: number): boolean {
  return Boolean(currentGrant) && expiresAt - skewMs > Date.now();
}

function persist(): void {
  try {
    if (currentGrant && expiresAt > 0) {
      window.localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ grant: currentGrant, exp: expiresAt })
      );
    } else {
      window.localStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    // 存储不可用（隐私模式、配额）时退化为仅内存持有
  }
}

/** 页面加载时恢复仍然有效的 Grant，避免每次刷新都要重新获取验证码。 */
function restore(): void {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return;
    const stored = JSON.parse(raw) as { grant?: unknown; exp?: unknown };
    if (typeof stored?.grant !== "string") {
      window.localStorage.removeItem(STORAGE_KEY);
      return;
    }
    currentGrant = stored.grant;
    expiresAt =
      typeof stored.exp === "number" ? stored.exp : readExpiry(stored.grant);
    if (!isValid(SKEW_MS)) clearVerificationGrant();
  } catch {
    // 忽略损坏的存储值
  }
}

if (typeof window !== "undefined") restore();

export function setVerificationGrant(value: string) {
  currentGrant = value ?? "";
  expiresAt = currentGrant ? readExpiry(currentGrant) : 0;
  persist();
}

/**
 * 返回当前仍有效的 Grant；已过期的 Grant 不返回。
 * 服务端对过期 Grant 会直接判 401，因此不能把过期值继续下发。
 */
export function getVerificationGrant() {
  return isValid(SKEW_MS) ? currentGrant : "";
}

/** 当前 Verification Grant 是否仍在校验有效期内。 */
export function hasValidVerificationGrant(skewMs = SKEW_MS) {
  return isValid(skewMs);
}

export function clearVerificationGrant() {
  currentGrant = "";
  expiresAt = 0;
  persist();
}
