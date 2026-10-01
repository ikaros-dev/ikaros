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

export function setVerificationGrant(value: string) {
  currentGrant = value ?? "";
  expiresAt = currentGrant ? readExpiry(currentGrant) : 0;
}

export function getVerificationGrant() {
  return currentGrant;
}

/** 当前 Verification Grant 是否仍在校验有效期内；预留 30s 安全边界，避免边界处被服务端判为过期。 */
export function hasValidVerificationGrant(skewMs = 30_000) {
  return Boolean(currentGrant) && expiresAt - skewMs > Date.now();
}

export function clearVerificationGrant() {
  currentGrant = "";
  expiresAt = 0;
}
