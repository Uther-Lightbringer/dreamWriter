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
