<template>
  <el-dialog
    :model-value="modelValue"
    title="下载客户端"
    width="380px"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="download-list">
      <el-button
        v-for="item in CLIENT_DOWNLOADS"
        :key="item.platform"
        size="large"
        class="download-option"
        :loading="loadingPlatform === item.platform"
        @click="download(item.platform)"
      >
        {{ item.label }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CLIENT_DOWNLOADS, downloadClientInstaller } from '../download'

defineProps({
  modelValue: { type: Boolean, default: false }
})
defineEmits(['update:modelValue'])

const loadingPlatform = ref('')

async function download(platform) {
  loadingPlatform.value = platform
  try {
    await downloadClientInstaller(platform)
    ElMessage.success('已开始下载')
  } catch (e) {
    ElMessage.error(e.message || '下载失败')
  } finally {
    loadingPlatform.value = ''
  }
}
</script>

<style scoped>
.download-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.download-option {
  width: 100%;
  margin: 0;
  border-radius: 10px;
}
</style>
