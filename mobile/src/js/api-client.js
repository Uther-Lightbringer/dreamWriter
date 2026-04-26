import { getServerUrl } from './config.js';

class ApiClient {
  async request(method, path, body = null) {
    const baseUrl = await getServerUrl();
    const options = {
      method,
      headers: {
        'Content-Type': 'application/json',
      },
    };
    if (body) {
      options.body = JSON.stringify(body);
    }
    
    const response = await fetch(`${baseUrl}${path}`, options);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    return response.json();
  }

  get(path) { return this.request('GET', path); }
  post(path, body) { return this.request('POST', path, body); }
  put(path, body) { return this.request('PUT', path, body); }
  delete(path) { return this.request('DELETE', path); }
}

export const api = new ApiClient();

export const getNovels = () => api.get('/api/novels');
export const getNovel = (id) => api.get(`/api/novels/${id}`);
export const getChapters = (novelId) => api.get(`/api/novels/${novelId}/chapters`);
export const getChapter = (novelId, chapterId) => 
  api.get(`/api/chapters/detail/${novelId}/${chapterId}`);
export const getCharacterCards = (novelId) => 
  api.get(`/api/novels/${novelId}/character-cards`);
export const getWorldview = (novelId) => 
  api.get(`/api/novels/${novelId}/worldview`);
export const getImageHistory = () => api.get('/api/image/history');

// 创作会话 API
export const getCreativeSessions = () => api.get('/api/creative-sessions');
export const getCreativeSession = (sessionId) => api.get(`/api/creative-sessions/${sessionId}`);
export const createCreativeSession = (title) => api.post('/api/creative-sessions', { title });
export const deleteCreativeSession = (sessionId) => api.delete(`/api/creative-sessions/${sessionId}`);
export const chatWithSession = (sessionId, content, onMessage, onError) => {
  return new Promise(async (resolve, reject) => {
    try {
      const baseUrl = await getServerUrl();
      const response = await fetch(`${baseUrl}/api/creative-sessions/${sessionId}/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content })
      });
      
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        
        const text = decoder.decode(value);
        const lines = text.split('\n');
        
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6);
            if (data === '[DONE]') {
              resolve();
              return;
            }
            try {
              const parsed = JSON.parse(data);
              onMessage(parsed);
            } catch (e) {
              onMessage({ content: data });
            }
          }
        }
      }
      
      resolve();
    } catch (error) {
      onError(error);
      reject(error);
    }
  });
};
