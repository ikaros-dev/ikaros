import { http } from "@/utils/http";

export type TokenInvalidationResult = {
  userId: string;
  securityVersion: number;
};

export const invalidateCurrentUserTokens = () =>
  http.request<TokenInvalidationResult>(
    "post",
    "/me/actions/invalidate-tokens"
  );
