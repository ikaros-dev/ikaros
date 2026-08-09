import { describe, expect, it, vi } from "vitest";
import {
  assertParamExists,
  createRequestFunction,
  serializeDataIfNeeded,
  setApiKeyToObject,
  setBasicAuthToObject,
  setBearerAuthToObject,
  setOAuthToObject,
  setSearchParams,
  toPathString,
} from "./common";

describe("API 请求参数转换", () => {
  it("校验必填参数", () => {
    expect(() => assertParamExists("getUser", "id", undefined)).toThrow(
      "Required parameter id was null or undefined when calling getUser.",
    );
    expect(() => assertParamExists("getUser", "id", "user-id")).not.toThrow();
  });

  it("将嵌套对象和数组展开为查询参数", () => {
    const url = new URL("https://example.com/resources?existing=value#section");

    setSearchParams(url, {
      filter: { state: "ACTIVE" },
      ids: ["1", "2"],
      empty: undefined,
    });

    expect(url.searchParams.get("existing")).toBe("value");
    expect(url.searchParams.get("filter.state")).toBe("ACTIVE");
    expect(url.searchParams.getAll("ids")).toEqual(["1", "2"]);
    expect(url.searchParams.has("empty")).toBe(false);
    expect(toPathString(url)).toBe(
      "/resources?existing=value&filter.state=ACTIVE&ids=1&ids=2#section",
    );
  });

  it("按内容类型序列化请求数据", () => {
    const jsonConfiguration = {
      isJsonMime: vi.fn().mockReturnValue(true),
    };
    const textConfiguration = {
      isJsonMime: vi.fn().mockReturnValue(false),
    };
    const requestOptions = { headers: { "Content-Type": "application/json" } };

    expect(
      serializeDataIfNeeded({ id: 1 }, requestOptions, jsonConfiguration),
    ).toBe('{"id":1}');
    expect(
      serializeDataIfNeeded(undefined, requestOptions, jsonConfiguration),
    ).toBe("{}");
    expect(
      serializeDataIfNeeded("raw", requestOptions, textConfiguration),
    ).toBe("raw");
  });
});

describe("API 认证配置", () => {
  it("设置同步和异步 API Key", async () => {
    const synchronousHeaders = {};
    const asynchronousHeaders = {};

    await setApiKeyToObject(synchronousHeaders, "X-API-Key", {
      apiKey: "sync-key",
    });
    await setApiKeyToObject(asynchronousHeaders, "X-API-Key", {
      apiKey: async () => "async-key",
    });

    expect(synchronousHeaders).toEqual({ "X-API-Key": "sync-key" });
    expect(asynchronousHeaders).toEqual({ "X-API-Key": "async-key" });
  });

  it("设置 Basic、Bearer 和 OAuth 认证信息", async () => {
    const requestOptions: Record<string, unknown> = {};

    setBasicAuthToObject(requestOptions, {
      username: "user",
      password: "secret",
    });
    await setBearerAuthToObject(requestOptions, {
      accessToken: async () => "bearer-token",
    });
    await setOAuthToObject(requestOptions, "oauth", ["read"], {
      accessToken: async (name, scopes) => `${name}-${scopes.join(",")}`,
    });

    expect(requestOptions.auth).toEqual({
      username: "user",
      password: "secret",
    });
    expect(requestOptions.Authorization).toBe("Bearer oauth-read");
  });

  it("支持字符串形式的 Bearer 和 OAuth 令牌", async () => {
    const requestOptions: Record<string, unknown> = {};

    await setBearerAuthToObject(requestOptions, {
      accessToken: "bearer-token",
    });
    expect(requestOptions.Authorization).toBe("Bearer bearer-token");

    await setOAuthToObject(requestOptions, "oauth", ["read"], {
      accessToken: "oauth-token",
    });
    expect(requestOptions.Authorization).toBe("Bearer oauth-token");
  });
});

describe("API 请求创建", () => {
  it("合并基础路径与请求参数", async () => {
    const request = vi.fn().mockResolvedValue({ data: { id: "1" } });
    const executeRequest = createRequestFunction(
      {
        url: "/v1/resources",
        options: { method: "GET", headers: { Accept: "application/json" } },
      },
      { request } as never,
      "https://example.com",
    );

    await executeRequest();

    expect(request).toHaveBeenCalledWith({
      method: "GET",
      headers: { Accept: "application/json" },
      url: "https://example.com/v1/resources",
    });
  });
});
