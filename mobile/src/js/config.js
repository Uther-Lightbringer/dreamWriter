// 配置管理 - 支持 Capacitor 和浏览器环境

const DEFAULT_SERVER_URL = 'http://192.168.1.94:38081';
const CONFIG_KEY = 'serverUrl';

// 检测是否在 Capacitor 环境中
const isCapacitor = () => {
  return typeof Capacitor !== 'undefined' && Capacitor.isNativePlatform && Capacitor.isNativePlatform();
};

// 获取 Preferences 实例（Capacitor）或使用 localStorage（浏览器）
const getStorage = () => {
  if (isCapacitor()) {
    // 动态导入 Capacitor Preferences
    return import('@capacitor/preferences').then(m => m.Preferences);
  }
  // 浏览器环境使用 localStorage
  return Promise.resolve({
    get: async ({ key }) => {
      const value = localStorage.getItem(key);
      return { value };
    },
    set: async ({ key, value }) => {
      localStorage.setItem(key, value);
    }
  });
};

let preferencesInstance = null;

const getPreferences = async () => {
  if (!preferencesInstance) {
    preferencesInstance = await getStorage();
  }
  return preferencesInstance;
};

export async function getServerUrl() {
  try {
    const prefs = await getPreferences();
    const { value } = await prefs.get({ key: CONFIG_KEY });
    return value || DEFAULT_SERVER_URL;
  } catch (error) {
    console.error('Failed to get server URL:', error);
    return DEFAULT_SERVER_URL;
  }
}

export async function setServerUrl(url) {
  try {
    const prefs = await getPreferences();
    await prefs.set({ key: CONFIG_KEY, value: url });
    await prefs.set({ 
      key: 'lastConnectedAt', 
      value: new Date().toISOString() 
    });
  } catch (error) {
    console.error('Failed to set server URL:', error);
    // 后备方案：使用 localStorage
    localStorage.setItem(CONFIG_KEY, url);
  }
}

export async function testConnection(url) {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000);
    const response = await fetch(`${url}/api/novels`, { 
      signal: controller.signal,
      method: 'GET'
    });
    clearTimeout(timeoutId);
    return response.ok;
  } catch (error) {
    console.error('Connection test failed:', error);
    return false;
  }
}

export async function hasConfiguredServer() {
  try {
    const prefs = await getPreferences();
    const { value } = await prefs.get({ key: CONFIG_KEY });
    return !!value;
  } catch (error) {
    console.error('Failed to check configuration:', error);
    return !!localStorage.getItem(CONFIG_KEY);
  }
}
