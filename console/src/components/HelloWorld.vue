<script setup lang="ts">
import { ref } from 'vue';

const count = ref(0);
</script>

<template>
	<h1>Hello Ikaros</h1>

	<div class="card">
		<p>Hello World</p>
		<el-button round @click="count++"> count is {{ count }} </el-button>
		<p>
			Edit
			<code>components/HelloWorld.vue</code> to test HMR
		</p>
	</div>
</template>
