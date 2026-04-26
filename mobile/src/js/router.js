const routes = {
  '/': 'home.html',
  '/novel': 'novel-detail.html',
  '/chapter': 'chapter-reader.html',
  '/creative': 'creative-guide.html',
  '/gallery': 'gallery.html',
  '/settings': 'settings.html',
};

export async function navigate(path, params = {}) {
  const container = document.getElementById('app-container');
  if (!container) return;

  const routePath = path.split('/')[1] ? `/${path.split('/')[1]}` : '/';
  const pageFile = routes[routePath] || 'home.html';
  
  window.location.hash = path;
  
  try {
    const response = await fetch(`./pages/${pageFile}`);
    const html = await response.text();
    container.innerHTML = html;
    
    const scripts = container.querySelectorAll('script');
    scripts.forEach(script => {
      const newScript = document.createElement('script');
      newScript.type = 'module';
      newScript.textContent = script.textContent;
      document.head.appendChild(newScript);
      document.head.removeChild(newScript);
    });
    
    window.currentRouteParams = params;
    
    window.dispatchEvent(new CustomEvent('pageShow', { detail: { path, params } }));
    
    updateNavState(path);
  } catch (error) {
    console.error('Navigation failed:', error);
    container.innerHTML = '<div class="error-page">页面加载失败，请检查网络连接</div>';
  }
}

function updateNavState(path) {
  const routePath = path.split('/')[1] ? `/${path.split('/')[1]}` : '/';
  document.querySelectorAll('.nav-item').forEach(el => {
    el.classList.toggle('active', el.dataset.route === routePath);
    el.classList.toggle('text-dream-purple', el.dataset.route === routePath);
  });
}

export function initRouter() {
  window.addEventListener('hashchange', () => {
    const hash = window.location.hash.slice(1) || '/';
    navigate(hash);
  });
  
  const initialHash = window.location.hash.slice(1) || '/';
  navigate(initialHash);
}
