import assert from "node:assert/strict";
import test from "node:test";
import { readdir, readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";

const home = await readFile(new URL("../src/router/modules/home.ts", import.meta.url), "utf8");
const card = await readFile(new URL("../src/views/console/PageCard.vue", import.meta.url), "utf8");
const viewsDir = new URL("../src/views/", import.meta.url);

test("legacy Console page trees are removed", async () => {
  const entries = await readdir(viewsDir, { withFileTypes: true });
  const dirs = entries.filter(entry => entry.isDirectory()).map(entry => entry.name).sort();
  assert.deepEqual(dirs, ["account", "console", "error", "login", "setup"]);
});

test("every routed Console page resolves to a page-card component", async () => {
  const imports = [...home.matchAll(/import\("(@\/views\/(?:console|account)\/[^"?]+\.vue)"\)/g)].map(match => match[1]);
  assert.ok(imports.length >= 50, `expected at least 50 routed page components, got ${imports.length}`);
  assert.equal(new Set(imports).size, imports.length, "each route should point to its own page file");
  for (const alias of imports) {
    const relative = alias.replace("@/views/", "../src/views/");
    const source = await readFile(new URL(relative, import.meta.url), "utf8");
    assert.match(source, /<PageCard(?:\s*\/\s*>|\s*>)/, `${alias} must use PageCard`);
  }
});

test("page card only renders the route title and responsibility subtitle", () => {
  assert.match(card, /route\.meta\.title/);
  assert.match(card, /route\.meta\.description/);
  assert.match(card, /<el-card/);
  assert.match(card, /<h1/);
  assert.match(card, /<p/);
});

test("canonical menu roots are Resources, Storage, Apps and System beneath Dashboard", () => {
  for (const route of ["/dashboard", "/resources", "/storage", "/apps", "/system"]) assert.ok(home.includes(`"${route}"`));
  for (const legacy of ["/library", "/add", "/activity"]) assert.doesNotMatch(home, new RegExp(`workspace\\(\\s*"${legacy}"`));
});
