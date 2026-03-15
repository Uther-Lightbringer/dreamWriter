package org.zenithon.articlecollect.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * BOM 字符过滤器，用于移除 HTTP 请求体中的 UTF-8 BOM 标记
 */
@Component
@Order(1)
public class BOMFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest) {
            // 只对 POST/PUT/PATCH 请求处理（这些请求有请求体）
            String method = httpRequest.getMethod();
            if ("POST".equalsIgnoreCase(method) ||
                    "PUT".equalsIgnoreCase(method) ||
                    "PATCH".equalsIgnoreCase(method)) {

                BOMRequestWrapper wrappedRequest = new BOMRequestWrapper(httpRequest);
                chain.doFilter(wrappedRequest, response);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Request 包装器，用于移除请求体中的 BOM 字符
     */
    static class BOMRequestWrapper extends jakarta.servlet.http.HttpServletRequestWrapper {

        private byte[] bodyBytes;

        public BOMRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (bodyBytes == null) {
                bodyBytes = readAndCleanBody(super.getInputStream());
            }
            return new CachedServletInputStream(bodyBytes);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            if (bodyBytes == null) {
                bodyBytes = readAndCleanBody(super.getInputStream());
            }
            return new BufferedReader(new InputStreamReader(
                    new CachedServletInputStream(bodyBytes), StandardCharsets.UTF_8));
        }

        private byte[] readAndCleanBody(ServletInputStream inputStream) throws IOException {
            if (inputStream == null) {
                return new byte[0];
            }

            // 读取原始字节
            byte[] originalBytes = inputStream.readAllBytes();

            if (originalBytes.length < 3) {
                return originalBytes;
            }

            // 检查并移除 UTF-8 BOM (0xEF 0xBB 0xBF)
            if ((originalBytes[0] & 0xFF) == 0xEF &&
                    (originalBytes[1] & 0xFF) == 0xBB &&
                    (originalBytes[2] & 0xFF) == 0xBF) {

                byte[] cleanBytes = new byte[originalBytes.length - 3];
                System.arraycopy(originalBytes, 3, cleanBytes, 0, cleanBytes.length);
                return cleanBytes;
            }

            return originalBytes;
        }
    }

    /**
     * 缓存的 ServletInputStream 实现
     */
    static class CachedServletInputStream extends ServletInputStream {

        private final byte[] data;
        private int offset = 0;

        public CachedServletInputStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() throws IOException {
            if (offset >= data.length) {
                return -1;
            }
            return data[offset++] & 0xFF;
        }

        @Override
        public boolean isFinished() {
            return offset >= data.length;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("Not supported");
        }
    }
}