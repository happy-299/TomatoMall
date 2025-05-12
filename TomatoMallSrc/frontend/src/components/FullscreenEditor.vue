<template>
  <el-dialog
      :model-value="modelValue"
      fullscreen
      :title="title"
      @close="handleClose"
  >
    <el-input
        v-model="localContent"
        type="textarea"
        :rows="20"
        resize="none"
        placeholder="请输入笔记内容..."
        class="fullscreen-editor"
    />
    <template #footer>
      <el-button type="primary" @click="saveContent">保存并返回</el-button>
      <el-button @click="cancelEdit">取消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'

const props = defineProps({
  modelValue: Boolean,
  content: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    default: '全屏编辑'
  }
})

const emit = defineEmits(['update:modelValue', 'update:content'])

const localContent = ref(props.content)

watch(() => props.content, (newVal) => {
  localContent.value = newVal
})

const saveContent = () => {
  emit('update:content', localContent.value)
  emit('update:modelValue', false)
}

const cancelEdit = () => {
  emit('update:modelValue', false)
}

const handleClose = () => {
  if (localContent.value !== props.content) {
    ElMessageBox.confirm('内容尚未保存，确定要离开吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      emit('update:modelValue', false)
    }).catch(() => {})
  } else {
    emit('update:modelValue', false)
  }
}
</script>