/**
 * EvoLink 图片生成前端工具类
 * 可在任何页面使用
 */
class EvoLinkImageGenerator {
    
    /**
     * 创建图片生成器实例
     * @param {Object} options 配置选项
     * @param {Function} options.onProgress 进度回调 (progress, status)
     * @param {Function} options.onComplete 完成回调 (imageUrl)
     * @param {Function} options.onError 错误回调 (error)
     * @param {number} options.pollInterval 轮询间隔 (毫秒，默认 5000)
     * @param {number} options.timeout 超时时间 (毫秒，默认 300000)
     */
    constructor(options = {}) {
        this.onProgress = options.onProgress || (() => {});
        this.onComplete = options.onComplete || (() => {});
        this.onError = options.onError || (() => {});
        this.pollInterval = options.pollInterval || 5000;
        this.timeout = options.timeout || 300000;
        
        this.currentTaskId = null;
        this.pollTimer = null;
        this.startTime = null;
    }
    
    /**
     * 生成图片
     * @param {string} prompt 提示词
     * @param {string} size 尺寸 (可选，默认 "16:9")
     */
    async generate(prompt, size = "16:9") {
        if (!prompt || !prompt.trim()) {
            this.onError(new Error("提示词不能为空"));
            return;
        }
        
        // 验证尺寸
        if (!this.validateSize(size)) {
            this.onError(new Error("无效的尺寸格式"));
            return;
        }
        
        try {
            // 创建任务
            const response = await fetch('/api/image/generate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ prompt, size })
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.currentTaskId = data.taskId;
                this.startTime = Date.now();
                this.startPolling();
            } else {
                this.onError(new Error(data.error || '创建任务失败'));
            }
        } catch (error) {
            this.onError(new Error('网络错误：' + error.message));
        }
    }
    
    /**
     * 开始轮询
     */
    startPolling() {
        this.pollTimer = setInterval(async () => {
            try {
                const response = await fetch(`/api/image/status/${this.currentTaskId}`);
                const data = await response.json();
                
                if (data.success) {
                    this.onProgress(data.progress, data.status);
                    
                    if (data.status === 'completed') {
                        this.complete(data.imageUrl);
                    } else if (data.status === 'failed') {
                        this.error(new Error(data.error || '生成失败'));
                    }
                    
                    // 检查超时
                    if (Date.now() - this.startTime > this.timeout) {
                        this.error(new Error('生成超时'));
                    }
                } else {
                    this.error(new Error(data.error || '查询失败'));
                }
            } catch (error) {
                this.error(new Error('查询状态失败：' + error.message));
            }
        }, this.pollInterval);
    }
    
    /**
     * 完成
     */
    complete(imageUrl) {
        this.stopPolling();
        this.onComplete(imageUrl);
    }
    
    /**
     * 错误处理
     */
    error(err) {
        this.stopPolling();
        this.onError(err);
    }
    
    /**
     * 停止轮询
     */
    stopPolling() {
        if (this.pollTimer) {
            clearInterval(this.pollTimer);
            this.pollTimer = null;
        }
    }
    
    /**
     * 取消生成
     */
    cancel() {
        this.stopPolling();
        this.currentTaskId = null;
    }
    
    /**
     * 验证尺寸格式
     */
    validateSize(size) {
        if (!size) return false;
        
        // 检查比例格式
        if (size.includes(':')) {
            const validRatios = ['1:1', '2:3', '3:2', '3:4', '4:3', '9:16', '16:9', '1:2', '2:1'];
            return validRatios.includes(size);
        }
        
        // 检查自定义尺寸
        if (size.includes('x')) {
            const parts = size.split('x');
            if (parts.length !== 2) return false;
            
            try {
                const width = parseInt(parts[0]);
                const height = parseInt(parts[1]);
                return width >= 376 && width <= 1536 && 
                       height >= 376 && height <= 1536;
            } catch (e) {
                return false;
            }
        }
        
        return false;
    }
}

// 导出为全局变量 (方便在任何页面使用)
window.EvoLinkImageGenerator = EvoLinkImageGenerator;
