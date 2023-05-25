import type { App } from "vue";
import { createI18n } from "vue-i18n";


const i18n = createI18n({
legacy: false,
locale: "zh-CN",
fallbackLocale: "zh-CN"
});


export function setupI18n(app: App) {
app.use(i18n);
}

export { i18n };