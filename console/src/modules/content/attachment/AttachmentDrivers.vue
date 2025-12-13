<script setup lang="ts">
import { AttachmentDriver } from '@runikaros/api-client';
import { onMounted, ref } from 'vue';



const attDrivers = ref<AttachmentDriver>()
const fetchAttDrivers = async() => {
    // const { data } = await apiClient.attachmentDriver.
    console.debug('attDrivers', attDrivers.value)
}


onMounted(fetchAttDrivers);
</script>
<template>
  <div>

    AttachmentDrivers


  </div>
</template>
<style lang="scss" scoped>
</style>
