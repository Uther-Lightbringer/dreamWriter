import { Preferences } from '@capacitor/preferences';

const DEFAULT_SERVER_URL = 'http://192.168.1.94:38081';
const CONFIG_KEY = 'serverUrl';

export async function getServerUrl() {
  const { value } = await Preferences.get({ key: CONFIG_KEY });
  return value || DEFAULT_SERVER_URL;
}

export async function setServerUrl(url) {
  await Preferences.set({ key: CONFIG_KEY, value: url });
  await Preferences.set({ 
    key: 'lastConnectedAt', 
    value: new Date().toISOString() 
  });
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
  const { value } = await Preferences.get({ key: CONFIG_KEY });
  return !!value;
}
