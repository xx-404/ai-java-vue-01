<template>
  <div class="page-container">
    <n-card>
      <n-tabs type="line" animated>
        <n-tab-pane name="merge" tab="文档合并">
          <div class="panel-content">
            <n-alert type="info" style="margin-bottom: 20px">
              <template #header>使用说明</template>
              <ul style="margin: 0; padding-left: 18px; line-height: 1.8">
                <li>最多支持合并 3 个 DOCX 文件</li>
                <li>文件格式仅限 .docx</li>
                <li>合并后的文件会在不同文档之间自动分页</li>
              </ul>
            </n-alert>

            <n-form label-placement="left" label-width="100px">
              <n-form-item label="选择文件">
                <n-upload
                  v-model:file-list="mergeFileList"
                  :max="3"
                  accept=".docx"
                  :show-file-list="true"
                  :custom-request="() => {}"
                  multiple
                >
                  <n-upload-dragger>
                    <div style="margin-bottom: 12px">
                      <n-icon size="48" :depth="3"><CloudUploadOutline /></n-icon>
                    </div>
                    <n-text style="font-size: 16px">点击或拖拽文件到此处上传</n-text>
                    <n-p depth="3" style="margin: 8px 0 0 0">支持 .docx 格式，最多 3 个文件</n-p>
                  </n-upload-dragger>
                </n-upload>
              </n-form-item>
            </n-form>

            <div style="margin-top: 20px">
              <n-space>
                <n-button type="primary" :loading="mergeLoading" :disabled="mergeFileList.length === 0" @click="handleMerge">
                  <template #icon><n-icon><GitMergeOutline /></n-icon></template>
                  开始合并
                </n-button>
                <n-button @click="mergeFileList = []">
                  <template #icon><n-icon><RefreshOutline /></n-icon></template>
                  清空
                </n-button>
              </n-space>
            </div>
          </div>
        </n-tab-pane>

        <n-tab-pane name="split" tab="文档拆分">
          <div class="panel-content">
            <n-alert type="info" style="margin-bottom: 20px">
              <template #header>使用说明</template>
              <ul style="margin: 0; padding-left: 18px; line-height: 1.8">
                <li>根据文档中包含的分隔符将文档拆分为多个文件</li>
                <li>分隔符非必填，默认为 20 个连续的"-"符号</li>
                <li>拆分结果以 ZIP 压缩包形式下载</li>
              </ul>
            </n-alert>

            <n-form label-placement="left" label-width="100px">
              <n-form-item label="选择文件">
                <n-upload
                  v-model:file-list="splitFileList"
                  :max="1"
                  accept=".docx"
                  :show-file-list="true"
                  :custom-request="() => {}"
                >
                  <n-upload-dragger>
                    <div style="margin-bottom: 12px">
                      <n-icon size="48" :depth="3"><CloudUploadOutline /></n-icon>
                    </div>
                    <n-text style="font-size: 16px">点击或拖拽文件到此处上传</n-text>
                    <n-p depth="3" style="margin: 8px 0 0 0">支持 .docx 格式</n-p>
                  </n-upload-dragger>
                </n-upload>
              </n-form-item>
              <n-form-item label="分隔符">
                <n-input
                  v-model:value="separator"
                  placeholder="留空则使用默认分隔符（20个-）"
                  clearable
                  style="max-width: 400px"
                />
                <n-p depth="3" style="margin: 6px 0 0 0; font-size: 12px">
                  默认分隔符：--------------------
                </n-p>
              </n-form-item>
            </n-form>

            <div style="margin-top: 20px">
              <n-space>
                <n-button type="primary" :loading="splitLoading" :disabled="splitFileList.length === 0" @click="handleSplit">
                  <template #icon><n-icon><CutOutline /></n-icon></template>
                  开始拆分
                </n-button>
                <n-button @click="handleResetSplit">
                  <template #icon><n-icon><RefreshOutline /></n-icon></template>
                  清空
                </n-button>
              </n-space>
            </div>
          </div>
        </n-tab-pane>
      </n-tabs>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useMessage } from 'naive-ui'
import { CloudUploadOutline, RefreshOutline, GitMergeOutline, CutOutline } from '@vicons/ionicons5'
import type { UploadFileInfo } from 'naive-ui'
import { docxApi } from '@/api/docx'

const message = useMessage()

const mergeFileList = ref<UploadFileInfo[]>([])
const mergeLoading = ref(false)

const splitFileList = ref<UploadFileInfo[]>([])
const separator = ref('')
const splitLoading = ref(false)

function downloadBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  window.URL.revokeObjectURL(url)
}

async function handleMerge() {
  if (mergeFileList.value.length === 0) {
    message.warning('请至少选择一个文件')
    return
  }
  if (mergeFileList.value.length > 3) {
    message.warning('最多支持合并 3 个文件')
    return
  }

  const files = mergeFileList.value
    .map(f => f.file)
    .filter((f): f is File => f != null)

  if (files.length === 0) {
    message.warning('文件无效，请重新选择')
    return
  }

  mergeLoading.value = true
  try {
    const blob = await docxApi.merge(files)
    downloadBlob(blob as unknown as Blob, '合并文档.docx')
    message.success('合并成功')
  } catch (e) {
    console.error(e)
  } finally {
    mergeLoading.value = false
  }
}

function handleResetSplit() {
  splitFileList.value = []
  separator.value = ''
}

async function handleSplit() {
  if (splitFileList.value.length === 0) {
    message.warning('请选择一个文件')
    return
  }

  const file = splitFileList.value[0]?.file
  if (!file) {
    message.warning('文件无效，请重新选择')
    return
  }

  splitLoading.value = true
  try {
    const blob = await docxApi.split(file, separator.value.trim())
    const originalName = file.name.replace(/\.docx$/i, '')
    downloadBlob(blob as unknown as Blob, `${originalName}_拆分结果.zip`)
    message.success('拆分成功')
  } catch (e) {
    console.error(e)
  } finally {
    splitLoading.value = false
  }
}
</script>

<style scoped>
.panel-content {
  padding-top: 16px;
}
</style>
