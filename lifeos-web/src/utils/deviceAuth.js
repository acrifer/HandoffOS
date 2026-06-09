const DEVICE_ID_KEY = 'handoff_device_id'
const DEVICE_TOKEN_KEY = 'handoff_device_token'
const LEGACY_TOKEN_KEY = 'lifeos_token'

const buildDeviceName = () => {
  const platform = navigator.platform || 'Browser'
  const language = navigator.language || 'zh-CN'
  return `${platform} · ${language}`
}

const createDeviceId = () => {
  if (crypto?.randomUUID) {
    return crypto.randomUUID()
  }
  return `device-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export const getDeviceId = () => {
  let deviceId = localStorage.getItem(DEVICE_ID_KEY)
  if (!deviceId) {
    deviceId = createDeviceId()
    localStorage.setItem(DEVICE_ID_KEY, deviceId)
  }
  return deviceId
}

export const getDeviceToken = () => {
  const token = localStorage.getItem(DEVICE_TOKEN_KEY)
  if (localStorage.getItem(LEGACY_TOKEN_KEY)) {
    localStorage.removeItem(LEGACY_TOKEN_KEY)
  }
  return token
}

export const clearDeviceToken = () => {
  localStorage.removeItem(DEVICE_TOKEN_KEY)
  localStorage.removeItem(LEGACY_TOKEN_KEY)
}

export const ensureDeviceSession = async () => {
  const currentToken = getDeviceToken()
  if (currentToken) {
    return currentToken
  }

  const response = await fetch('/api/auth/device-login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      deviceId: getDeviceId(),
      deviceName: buildDeviceName(),
      userAgent: navigator.userAgent || ''
    })
  })
  const payload = await response.json()
  if (!response.ok || payload.code !== 200 || !payload.data?.token) {
    throw new Error(payload.message || '设备演示登录失败')
  }
  localStorage.setItem(DEVICE_TOKEN_KEY, payload.data.token)
  localStorage.removeItem(LEGACY_TOKEN_KEY)
  localStorage.setItem('handoff_device_name', payload.data.deviceName || buildDeviceName())
  if (payload.data.quota) {
    localStorage.setItem('handoff_quota_snapshot', JSON.stringify(payload.data.quota))
  }
  return payload.data.token
}
