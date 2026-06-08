import { request } from '@/utils/request'

// DOCX工具 API
export const docxApi = {
  merge(files: File[]) {
    const formData = new FormData()
    files.forEach((file) => {
      formData.append('files', file)
    })
    return request({
      url: '/tool/docx/merge',
      method: 'post',
      data: formData,
      responseType: 'blob',
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  split(file: File, separator?: string) {
    const formData = new FormData()
    formData.append('file', file)
    if (separator && separator.trim()) {
      formData.append('separator', separator)
    }
    return request({
      url: '/tool/docx/split',
      method: 'post',
      data: formData,
      responseType: 'blob',
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
