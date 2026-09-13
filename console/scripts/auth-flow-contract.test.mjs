import assert from "node:assert/strict";
import test from "node:test";
import { readFile } from "node:fs/promises";

const login = await readFile(new URL("../src/views/login/index.vue", import.meta.url), "utf8");
const register = await readFile(new URL("../src/views/login/register.vue", import.meta.url), "utf8");

test("login displays rejected authentication errors and redirects after success", () => {
  assert.match(login, /catch\(\(e: any\)/);
  assert.match(login, /e\?\.response\?\.data\?\.detail/);
  assert.match(login, /router\.replace\(target\)/);
});

test("register keeps an inline error surface for rejected requests", () => {
  assert.match(register, /el-alert v-if="error"/);
  assert.match(register, /e\?\.response\?\.data\?\.detail/);
});
