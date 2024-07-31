<script setup lang="ts">
import Hamburger from './components/Hamburger.vue';
import Avatar from './components/Avatar.vue';
import Breadcrumb from './components/Breadcrumb.vue';
import LanguageSelect from './components/LanguageSelect.vue';
import Wellcome from './components/Wellcome.vue';
</script>

<template>
	<div class="navbar">
		<Hamburger />
		<Breadcrumb />

		<div class="navbar-right">
      <Wellcome/>
      &nbsp;&nbsp;&nbsp;&nbsp;
			<LanguageSelect />
			&nbsp;&nbsp;&nbsp;&nbsp;
			<Avatar class="navbar-item" />
		</div>
	</div>
</template>

<style lang="scss" scoped>
.navbar {
	width: 100%;
	height: 60px;
	overflow: hidden;
	background-color: #fff;
	box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
	padding-right: 16px;
	display: flex;
	align-items: center;
	box-sizing: border-box;
	position: relative;
	.navbar-right {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: flex-end;
		::deep .navbar-item {
			display: inline-block;
			margin-left: 18px;
			font-size: 22px;
			color: #5a5e66;
			box-sizing: border-box;
			cursor: pointer;
		}
	}
}
</style>
