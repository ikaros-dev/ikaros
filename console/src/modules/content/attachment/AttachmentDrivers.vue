<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { AttachmentDriver } from '@runikaros/api-client';
import { onMounted, ref } from 'vue';



const attDrivers = ref<AttachmentDriver[]>()
const fetchAttDrivers = async() => {
    const { data } = await apiClient.attachmentDriver.listDriversByCondition({
      page: 1,
      size: 10
    })
    attDrivers.value = data.items as AttachmentDriver[]
    console.debug('attDrivers', attDrivers.value)
}


onMounted(fetchAttDrivers);
</script>
<template>
  <div>

    AttachmentDrivers

    <hr />
     {{ attDrivers }}

  </div>
</template>
<style lang="scss" scoped>
</style>
