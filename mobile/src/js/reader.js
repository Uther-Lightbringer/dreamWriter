const READER_SETTINGS_KEY = 'readerSettings';

export function getReaderSettings() {
  const defaults = {
    fontSize: 18,
    theme: 'light',
    lineHeight: 'comfortable'
  };
  try {
    const saved = localStorage.getItem(READER_SETTINGS_KEY);
    return saved ? { ...defaults, ...JSON.parse(saved) } : defaults;
  } catch {
    return defaults;
  }
}

export function saveReaderSettings(settings) {
  localStorage.setItem(READER_SETTINGS_KEY, JSON.stringify(settings));
}

export function applyReaderSettings(container) {
  const settings = getReaderSettings();
  
  container.style.fontSize = `${settings.fontSize}px`;
  
  if (settings.theme === 'dark') {
    container.classList.add('dark');
  } else {
    container.classList.remove('dark');
  }
  
  const lineHeightMap = {
    compact: 1.5,
    comfortable: 1.8,
    relaxed: 2.2
  };
  container.style.lineHeight = lineHeightMap[settings.lineHeight] || 1.8;
}

export function saveReadingProgress(novelId, chapterId, chapterTitle, progress) {
  const data = {
    novelId,
    chapterId,
    chapterTitle,
    progress,
    timestamp: Date.now()
  };
  localStorage.setItem('lastRead', JSON.stringify(data));
}

export function calculateProgress(container) {
  const scrollTop = container.scrollTop;
  const scrollHeight = container.scrollHeight - container.clientHeight;
  return scrollHeight > 0 ? Math.round((scrollTop / scrollHeight) * 100) : 0;
}
