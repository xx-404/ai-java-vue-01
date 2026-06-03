import { defineStore } from 'pinia'
import { ref } from 'vue'
import { wsManager } from '@/utils/websocket'

export interface MessageNotification {
  id: number
  type: 'notice' | 'chat' | 'alert'
  title: string
  content: string
  time: number
  read: boolean
  senderId?: number
  groupId?: number
  alertType?: string
}

export const useMessageStore = defineStore('message', () => {
  const noticeCount = ref(0)
  const chatCount = ref(0)
  const alertCount = ref(0)
  const notifications = ref<MessageNotification[]>([])
  const showNotification = ref(false)
  const currentNotification = ref<MessageNotification | null>(null)

  function initWebSocket() {
    wsManager.on('notice', (data) => {
      const notification: MessageNotification = {
        id: Date.now(),
        type: 'notice',
        title: data.title || '系统通知',
        content: data.content || '',
        time: data.time || Date.now(),
        read: false
      }
      addNotification(notification)
      noticeCount.value++
    })

    wsManager.on('chat', (data) => {
      const notification: MessageNotification = {
        id: Date.now(),
        type: 'chat',
        title: data.senderName || '新消息',
        content: data.content || '',
        time: data.time || Date.now(),
        read: false,
        senderId: data.senderId
      }
      addNotification(notification)
      chatCount.value++
    })
    
    wsManager.on('groupChat', (data) => {
      const notification: MessageNotification = {
        id: Date.now(),
        type: 'chat',
        title: data.senderName ? `${data.senderName}(群消息)` : '群消息',
        content: data.content || '',
        time: data.time || Date.now(),
        read: false,
        groupId: data.groupId
      }
      addNotification(notification)
      chatCount.value++
    })

    wsManager.on('alert', (data) => {
      const notification: MessageNotification = {
        id: Date.now(),
        type: 'alert',
        title: data.title || '服务器告警',
        content: data.content || '',
        time: data.time || Date.now(),
        read: false,
        alertType: data.alertType
      }
      addNotification(notification)
      if (data.recovered) {
        alertCount.value = Math.max(0, alertCount.value - 1)
      } else {
        alertCount.value++
      }
    })

    wsManager.on('unread', (data) => {
      noticeCount.value = data.noticeCount || 0
      chatCount.value = data.chatCount || 0
    })

    wsManager.connect()
  }

  // 添加通知
  function addNotification(notification: MessageNotification) {
    notifications.value.unshift(notification)
    // 最多保留20条
    if (notifications.value.length > 20) {
      notifications.value.pop()
    }
    // 显示通知弹窗
    currentNotification.value = notification
    showNotification.value = true
    // 3秒后自动关闭
    setTimeout(() => {
      if (currentNotification.value?.id === notification.id) {
        showNotification.value = false
      }
    }, 5000)
  }

  // 设置未读数量
  function setUnreadCount(notice: number, chat: number) {
    noticeCount.value = notice
    chatCount.value = chat
  }

  // 清除通知未读
  function clearNoticeCount() {
    noticeCount.value = 0
  }

  // 清除聊天未读
  function clearChatCount() {
    chatCount.value = 0
  }

  // 关闭通知弹窗
  function closeNotification() {
    showNotification.value = false
    currentNotification.value = null
  }

  // 断开WebSocket
  function disconnectWebSocket() {
    wsManager.disconnect()
  }

  const totalUnread = () => noticeCount.value + chatCount.value + alertCount.value

  return {
    noticeCount,
    chatCount,
    alertCount,
    notifications,
    showNotification,
    currentNotification,
    initWebSocket,
    addNotification,
    setUnreadCount,
    clearNoticeCount,
    clearChatCount,
    closeNotification,
    disconnectWebSocket,
    totalUnread
  }
})
