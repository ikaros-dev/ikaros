<script setup lang="ts">
import { ref } from 'vue';

defineOptions({ inheritAttrs: false });

withDefaults(
	defineProps<{
		id: string;
		label: string;
		type?: string;
		required?: boolean;
	}>(),
	{
		type: 'text',
		required: false,
	}
);

const modelValue = defineModel<string>({ required: true });
const inputRef = ref<HTMLInputElement | null>(null);

const focus = () => inputRef.value?.focus();

defineExpose({ focus });
</script>

<template>
	<div class="filled-text-field">
		<div class="filled-text-field__container">
			<!-- eslint-disable-next-line vue/no-restricted-html-elements -- 原生输入框用于实现可复用的浮动标签输入框 -->
			<input
				:id="id"
				ref="inputRef"
				v-model="modelValue"
				v-bind="$attrs"
				:type="type"
				class="filled-text-field__input"
				:placeholder="label"
				:required="required"
			/>
			<label :for="id" class="filled-text-field__label">{{ label }}</label>
			<div class="filled-text-field__underline"></div>
		</div>
	</div>
</template>

<style scoped>
.filled-text-field {
	width: 100%;
}

.filled-text-field__container {
	position: relative;
	display: flex;
	align-items: center;
	height: 56px;
	background: var(--m3-surface-container-highest, #eef3f8);
	border-radius: 4px 4px 0 0;
	cursor: text;
	transition: background 0.15s ease;
}

.filled-text-field__container:hover {
	background: var(--m3-surface-container-high, #f8faff);
}

.filled-text-field__container:focus-within {
	background: var(--m3-surface-container-highest, #eef3f8);
}

.filled-text-field__input {
	width: 100%;
	height: 100%;
	padding: 24px 16px 8px;
	border: none;
	outline: none;
	background: transparent;
	box-sizing: border-box;
	color: var(--m3-on-surface, #1f1f1f);
	font-family: 'Roboto', system-ui, sans-serif;
	font-size: 16px;
	font-weight: 400;
	line-height: 24px;
	letter-spacing: 0.5px;
}

.filled-text-field__input::placeholder {
	color: transparent;
}

.filled-text-field__input:-webkit-autofill,
.filled-text-field__input:-webkit-autofill:hover,
.filled-text-field__input:-webkit-autofill:focus {
	-webkit-box-shadow: 0 0 0 1000px var(--m3-surface-container-highest, #eef3f8)
		inset;
	-webkit-text-fill-color: var(--m3-on-surface, #1f1f1f);
	caret-color: var(--m3-primary, #409eff);
	border-radius: 4px 4px 0 0;
}

.filled-text-field__label {
	position: absolute;
	top: 50%;
	left: 16px;
	transform: translateY(-50%);
	transform-origin: left top;
	color: var(--m3-on-surface-variant, #44474f);
	font-family: 'Roboto', system-ui, sans-serif;
	font-size: 16px;
	font-weight: 400;
	line-height: 24px;
	letter-spacing: 0.5px;
	pointer-events: none;
	transition: all 0.15s ease;
}

.filled-text-field__input:focus + .filled-text-field__label,
.filled-text-field__input:not(:placeholder-shown) + .filled-text-field__label {
	top: 8px;
	transform: translateY(0) scale(0.75);
	color: var(--m3-primary, #409eff);
	font-weight: 500;
}

.filled-text-field__underline {
	position: absolute;
	right: 0;
	bottom: 0;
	left: 0;
	height: 1px;
	background: var(--m3-outline, #c4c6d0);
	transition: all 0.15s ease;
}

.filled-text-field__container:hover .filled-text-field__underline {
	height: 1px;
	background: var(--m3-on-surface, #1f1f1f);
}

.filled-text-field__container:focus-within .filled-text-field__underline {
	height: 2px;
	background: var(--m3-primary, #409eff);
}
</style>
