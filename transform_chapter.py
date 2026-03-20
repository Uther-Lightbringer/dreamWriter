#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import re

# Read the original chapter-detail.html
with open(r'E:\WorkSpace\articleCollect\src\main\resources\templates\chapter-detail.html', 'r', encoding='utf-8') as f:
    content = f.read()

# Dreamy CSS to replace the original style
dreamy_css = '''    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@300;400;600&family=ZCOOL+XiaoWei&display=swap" rel="stylesheet">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        :root {
            --dream-pink: #fce4ec;
            --dream-lavender: #e8eaf6;
            --dream-mint: #e0f7fa;
            --dream-peach: #fff3e0;
            --accent-purple: #9c27b0;
            --accent-blue: #3f51b5;
            --accent-teal: #009688;
            --text-dark: #5d4037;
            --text-light: #8d6e63;
            --glass-bg: rgba(255, 255, 255, 0.7);
            --glass-border: rgba(255, 255, 255, 0.8);
            --font-size-base: 16px;
        }

        body {
            font-family: 'Noto Serif SC', serif;
            min-height: 100vh;
            background: linear-gradient(135deg, var(--dream-pink) 0%, var(--dream-lavender) 25%, var(--dream-mint) 50%, var(--dream-peach) 75%, var(--dream-pink) 100%);
            background-size: 400% 400%;
            animation: gradientShift 20s ease infinite;
            position: relative;
            overflow-x: hidden;
            color: var(--text-dark);
            line-height: 1.8;
        }

        /* 梦幻气泡背景 */
        .floating-orbs {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            pointer-events: none;
            z-index: 0;
            overflow: hidden;
        }

        .orb {
            position: absolute;
            border-radius: 50%;
            filter: blur(60px);
            opacity: 0.6;
            animation: float 15s ease-in-out infinite;
        }

        .orb-1 {
            width: 400px;
            height: 400px;
            background: radial-gradient(circle, #ffb3d9, transparent);
            top: -100px;
            left: -100px;
            animation-delay: 0s;
        }

        .orb-2 {
            width: 350px;
            height: 350px;
            background: radial-gradient(circle, #b3d9ff, transparent);
            top: 30%;
            right: -50px;
            animation-delay: -5s;
        }

        .orb-3 {
            width: 300px;
            height: 300px;
            background: radial-gradient(circle, #b3ffe0, transparent);
            bottom: -50px;
            left: 30%;
            animation-delay: -10s;
        }

        .orb-4 {
            width: 250px;
            height: 250px;
            background: radial-gradient(circle, #ffe0b3, transparent);
            bottom: 20%;
            right: 20%;
            animation-delay: -7s;
        }

        @keyframes gradientShift {
            0%, 100% { background-position: 0% 50%; }
            50% { background-position: 100% 50%; }
        }

        @keyframes float {
            0%, 100% { transform: translate(0, 0) scale(1); }
            25% { transform: translate(30px, -30px) scale(1.05); }
            50% { transform: translate(-20px, 20px) scale(0.95); }
            75% { transform: translate(20px, 30px) scale(1.02); }
        }

        @keyframes fadeInDown {
            from {
                opacity: 0;
                transform: translateY(-30px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        @keyframes fadeInUp {
            from {
                opacity: 0;
                transform: translateY(30px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        /* 主容器 - 玻璃态设计 */
        .container {
            max-width: 900px;
            margin: 0 auto;
            padding: 40px 20px;
            position: relative;
            z-index: 1;
            animation: fadeInUp 0.8s ease-out;
        }

        /* 玻璃卡片通用样式 */
        .glass-card {
            background: var(--glass-bg);
            backdrop-filter: blur(20px);
            border-radius: 24px;
            border: 1px solid var(--glass-border);
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
            padding: 40px;
            margin-bottom: 30px;
            transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
            position: relative;
            overflow: hidden;
        }

        .glass-card::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 3px;
            background: linear-gradient(90deg, var(--accent-purple), var(--accent-blue), var(--accent-teal));
            opacity: 0;
            transition: opacity 0.3s ease;
        }

        .glass-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.12);
        }

        .glass-card:hover::before {
            opacity: 1;
        }

        /* 头部设计 */
        .header {
            text-align: center;
            margin-bottom: 40px;
            animation: fadeInDown 1s ease-out;
        }

        .header h1 {
            font-family: 'ZCOOL XiaoWei', serif;
            font-size: 2.5rem;
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-blue), var(--accent-teal));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
            letter-spacing: 4px;
            text-shadow: 0 0 40px rgba(156, 39, 176, 0.3);
        }

        /* 面包屑导航 */
        .breadcrumb {
            padding: 15px 25px;
            background: var(--glass-bg);
            backdrop-filter: blur(10px);
            border-radius: 50px;
            border: 1px solid var(--glass-border);
            margin-bottom: 30px;
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 8px;
            animation: fadeInDown 0.8s ease-out 0.1s both;
        }

        .breadcrumb a {
            color: var(--accent-purple);
            text-decoration: none;
            transition: all 0.3s ease;
            font-weight: 500;
        }

        .breadcrumb a:hover {
            color: var(--accent-blue);
            text-decoration: underline;
        }

        .chapter-number-badge {
            display: inline-block;
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-blue));
            color: white;
            padding: 6px 16px;
            border-radius: 20px;
            font-size: 0.85rem;
            font-weight: 600;
            margin-left: auto;
            box-shadow: 0 4px 15px rgba(156, 39, 176, 0.3);
            transition: all 0.3s ease;
        }

        .chapter-number-badge:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(156, 39, 176, 0.4);
        }

        /* 阅读设置区域 */
        .reading-settings-bar {
            background: var(--glass-bg);
            backdrop-filter: blur(10px);
            border-radius: 16px;
            border: 1px solid var(--glass-border);
            padding: 20px 25px;
            margin-bottom: 30px;
            display: flex;
            flex-wrap: wrap;
            gap: 20px;
            align-items: center;
            animation: fadeInUp 0.8s ease-out 0.2s both;
        }

        .setting-group {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .setting-label {
            font-weight: 600;
            color: var(--text-dark);
            font-size: 0.9rem;
            white-space: nowrap;
        }

        .font-size-controls {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .font-size-btn {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            border: 2px solid var(--accent-purple);
            background: rgba(255, 255, 255, 0.8);
            color: var(--accent-purple);
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: bold;
            font-size: 1.2rem;
            transition: all 0.3s ease;
        }

        .font-size-btn:hover {
            background: var(--accent-purple);
            color: white;
            transform: scale(1.1);
            box-shadow: 0 4px 15px rgba(156, 39, 176, 0.3);
        }

        .font-size-display {
            min-width: 50px;
            text-align: center;
            font-weight: 600;
            color: var(--text-dark);
            font-size: 0.95rem;
        }

        .theme-toggle,
        .reset-btn,
        .mode-toggle-btn,
        .worldview-btn {
            padding: 10px 20px;
            border-radius: 50px;
            border: 2px solid transparent;
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-blue));
            color: white;
            cursor: pointer;
            font-weight: 600;
            font-size: 0.9rem;
            transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
            display: flex;
            align-items: center;
            gap: 8px;
            box-shadow: 0 4px 15px rgba(156, 39, 176, 0.3);
        }

        .theme-toggle:hover,
        .reset-btn:hover,
        .mode-toggle-btn:hover,
        .worldview-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 25px rgba(156, 39, 176, 0.4);
        }

        .reset-btn {
            background: linear-gradient(135deg, var(--accent-teal), #4db6ac);
            box-shadow: 0 4px 15px rgba(0, 150, 136, 0.3);
        }

        .reset-btn:hover {
            box-shadow: 0 8px 25px rgba(0, 150, 136, 0.4);
        }

        .mode-toggle-btn {
            background: linear-gradient(135deg, #ff9800, #f57c00);
            box-shadow: 0 4px 15px rgba(255, 152, 0, 0.3);
        }

        .mode-toggle-btn:hover {
            box-shadow: 0 8px 25px rgba(255, 152, 0, 0.4);
        }

        .worldview-btn {
            background: linear-gradient(135deg, #e91e63, #c2185b);
            box-shadow: 0 4px 15px rgba(233, 30, 99, 0.3);
        }

        .worldview-btn:hover {
            box-shadow: 0 8px 25px rgba(233, 30, 99, 0.4);
        }

        /* 章节标题 */
        .chapter-title {
            font-family: 'ZCOOL XiaoWei', serif;
            font-size: 2rem;
            color: var(--text-dark);
            text-align: center;
            margin-bottom: 20px;
            padding-bottom: 20px;
            border-bottom: 2px solid rgba(156, 39, 176, 0.2);
        }

        .chapter-info {
            text-align: center;
            color: var(--text-light);
            font-size: 0.9rem;
            margin-bottom: 30px;
        }

        /* 章节内容 */
        .content {
            font-size: var(--font-size-base);
            line-height: 2;
        }

        .content-body {
            font-size: var(--font-size-base);
            line-height: 2;
        }

        .content-body p {
            margin-bottom: 1.5em;
            text-indent: 2em;
        }

        .content-body img {
            max-width: 100%;
            border-radius: 12px;
            margin: 20px 0;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
        }

        /* 章节插图 */
        .chapter-image-container {
            margin: 30px 0;
            text-align: center;
        }

        .chapter-image-wrapper {
            position: relative;
            display: inline-block;
            cursor: pointer;
        }

        .chapter-image {
            max-width: 100%;
            border-radius: 16px;
            box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
            transition: all 0.4s ease;
        }

        .chapter-image:hover {
            transform: scale(1.02);
            box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
        }

        .chapter-image-placeholder {
            width: 100%;
            max-width: 400px;
            height: 250px;
            background: linear-gradient(135deg, rgba(156, 39, 176, 0.1), rgba(63, 81, 181, 0.1));
            border: 2px dashed rgba(156, 39, 176, 0.3);
            border-radius: 16px;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            color: var(--text-light);
            font-size: 1rem;
            cursor: pointer;
            transition: all 0.3s ease;
        }

        .chapter-image-placeholder:hover {
            background: linear-gradient(135deg, rgba(156, 39, 176, 0.2), rgba(63, 81, 181, 0.2));
            border-color: var(--accent-purple);
        }

        .chapter-upload-btn,
        .chapter-delete-btn {
            margin: 15px 10px;
            padding: 12px 30px;
            border-radius: 50px;
            border: none;
            font-family: 'Noto Serif SC', serif;
            font-size: 0.95rem;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
            display: inline-flex;
            align-items: center;
            gap: 8px;
        }

        .chapter-upload-btn {
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-blue));
            color: white;
            box-shadow: 0 4px 15px rgba(156, 39, 176, 0.3);
        }

        .chapter-upload-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 25px rgba(156, 39, 176, 0.4);
        }

        .chapter-delete-btn {
            background: linear-gradient(135deg, #f44336, #d32f2f);
            color: white;
            box-shadow: 0 4px 15px rgba(244, 67, 54, 0.3);
        }

        .chapter-delete-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 25px rgba(244, 67, 54, 0.4);
        }

        /* 导航按钮 */
        .navigation {
            display: flex;
            justify-content: center;
            gap: 20px;
            margin-top: 40px;
            flex-wrap: wrap;
        }

        .nav-button {
            padding: 14px 36px;
            border-radius: 50px;
            border: 2px solid transparent;
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-blue));
            color: white;
            font-family: 'Noto Serif SC', serif;
            font-size: 1rem;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
            box-shadow: 0 4px 15px rgba(156, 39, 176, 0.3);
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .nav-button:hover:not(:disabled) {
            transform: translateY(-3px);
            box-shadow: 0 10px 30px rgba(156, 39, 176, 0.4);
        }

        .nav-button:disabled {
            opacity: 0.5;
            cursor: not-allowed;
            transform: none !important;
        }

        .nav-button.secondary {
            background: linear-gradient(135deg, var(--accent-teal), #4db6ac);
            box-shadow: 0 4px 15px rgba(0, 150, 136, 0.3);
        }

        .nav-button.secondary:hover:not(:disabled) {
            box-shadow: 0 10px 30px rgba(0, 150, 136, 0.4);
        }

        .chapter-list-btn {
            background: linear-gradient(135deg, #ff9800, #f57c00);
            box-shadow: 0 4px 15px rgba(255, 152, 0, 0.3);
        }

        .chapter-list-btn:hover:not(:disabled) {
            box-shadow: 0 10px 30px rgba(255, 152, 0, 0.4);
        }

        /* 键盘快捷键提示 */
        .keyboard-hints {
            text-align: center;
            margin-top: 25px;
            padding: 15px;
            background: rgba(255, 255, 255, 0.5);
            border-radius: 12px;
            color: var(--text-light);
            font-size: 0.85rem;
        }

        .hint-item {
            margin: 0 10px;
        }

        .hint-divider {
            margin: 0 15px;
            opacity: 0.5;
        }

        /* 加载和错误状态 */
        .loading,
        .error {
            text-align: center;
            padding: 60px 20px;
            font-size: 1.2rem;
            color: var(--text-light);
        }

        /* AI 绘画面板区域样式 */
        .ai-painting-panels {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 25px;
            margin: 30px 0;
            animation: fadeInUp 0.8s ease-out 0.3s both;
        }

        @media (max-width: 768px) {
            .ai-painting-panels {
                grid-template-columns: 1fr;
            }
        }

        .ai-panel {
            background: var(--glass-bg);
            backdrop-filter: blur(15px);
            border-radius: 20px;
            border: 1px solid var(--glass-border);
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
            overflow: hidden;
            transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
            max-height: 700px;
            overflow-y: auto;
        }

        .ai-panel:hover {
            transform: translateY(-5px);
            box-shadow: 0 15px 45px rgba(0, 0, 0, 0.12);
        }

        .ai-panel-header {
            padding: 20px 25px;
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-blue));
            color: white;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .ai-panel-header h3 {
            margin: 0;
            font-size: 1.1rem;
            font-weight: 600;
            font-family: 'ZCOOL XiaoWei', serif;
            letter-spacing: 2px;
        }

        .ai-panel-close {
            background: rgba(255, 255, 255, 0.2);
            border: none;
            color: white;
            width: 32px;
            height: 32px;
            border-radius: 50%;
            cursor: pointer;
            font-size: 1.3rem;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.3s ease;
        }

        .ai-panel-close:hover {
            background: rgba(255, 255, 255, 0.3);
            transform: scale(1.1);
        }

        .ai-panel-content {
            padding: 25px;
        }

        .form-group {
            margin-bottom: 20px;
        }

        .form-group label {
            display: block;
            font-weight: 600;
            color: var(--text-dark);
            margin-bottom: 10px;
            font-size: 0.95rem;
            letter-spacing: 1px;
        }

        .dream-input,
        .dream-textarea,
        .ai-panel-content .form-group textarea,
        .ai-panel-content .size-select {
            width: 100%;
            padding: 16px 20px;
            border: 2px solid transparent;
            border-radius: 14px;
            background: rgba(255, 255, 255, 0.8);
            font-family: 'Noto Serif SC', serif;
            font-size: 0.95rem;
            color: var(--text-dark);
            transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
            outline: none;
            box-sizing: border-box;
        }

        .dream-textarea,
        .ai-panel-content .form-group textarea {
            min-height: 120px;
            resize: vertical;
            line-height: 1.8;
        }

        .dream-input:focus,
        .dream-textarea:focus,
        .ai-panel-content .form-group textarea:focus,
        .ai-panel-content .size-select:focus {
            border-color: var(--accent-purple);
            box-shadow: 0 0 0 6px rgba(156, 39, 176, 0.1), 0 8px 24px rgba(156, 39, 176, 0.15);
            background: rgba(255, 255, 255, 0.95);
        }

        /* 按钮样式 */
        .dream-btn,
        .generate-prompt-btn,
        .use-prompt-btn,
        .generate-image-btn,
        .save-image-btn,
        .regenerate-btn {
            padding: 14px 32px;
            border: none;
            border-radius: 50px;
            font-family: 'Noto Serif SC', serif;
            font-size: 0.95rem;
            font-weight: 600;
            letter-spacing: 1px;
            cursor: pointer;
            transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
            position: relative;
            overflow: hidden;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
            width: 100%;
            margin-bottom: 12px;
        }

        .dream-btn::before,
        .generate-prompt-btn::before,
        .use-prompt-btn::before,
        .generate-image-btn::before,
        .save-image-btn::before,
        .regenerate-btn::before {
            content: '';
            position: absolute;
            top: 50%;
            left: 50%;
            width: 0;
            height: 0;
            background: rgba(255, 255, 255, 0.3);
            border-radius: 50%;
            transform: translate(-50%, -50%);
            transition: width 0.6s ease, height 0.6s ease;
        }

        .dream-btn:hover::before,
        .generate-prompt-btn:hover::before,
        .use-prompt-btn:hover::before,
        .generate-image-btn:hover::before,
        .save-image-btn:hover::before,
        .regenerate-btn:hover::before {
            width: 300px;
            height: 300px;
        }

        .generate-prompt-btn {
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-blue));
            color: white;
            box-shadow: 0 4px 15px rgba(156, 39, 176, 0.3);
        }

        .generate-prompt-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 25px rgba(156, 39, 176, 0.4);
        }

        .use-prompt-btn {
            background: linear-gradient(135deg, #e91e63, #c2185b);
            color: white;
            box-shadow: 0 4px 15px rgba(233, 30, 99, 0.3);
        }

        .use-prompt-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 25px rgba(233, 30, 99, 0.4);
        }

        .generate-image-btn {
            background: linear-gradient(135deg, #e91e63, #c2185b);
            color: white;
            box-shadow: 0 4px 15px rgba(233, 30, 99, 0.3);
        }

        .generate-image-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 25px rgba(233, 30, 99, 0.4);
        }

        .save-image-btn {
            background: linear-gradient(135deg, var(--accent-teal), #4db6ac);
            color: white;
            box-shadow: 0 4px 15px rgba(0, 150, 136, 0.3);
        }

        .save-image-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 25px rgba(0, 150, 136, 0.4);
        }

        .regenerate-btn {
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-blue));
            color: white;
            box-shadow: 0 4px 15px rgba(156, 39, 176, 0.3);
        }

        .regenerate-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 25px rgba(156, 39, 176, 0.4);
        }

        .btn-group {
            display: flex;
            gap: 12px;
            flex-wrap: wrap;
            margin-top: 20px;
        }

        /* 加载和进度 */
        .loading-section {
            text-align: center;
            padding: 25px;
        }

        .spinner {
            width: 45px;
            height: 45px;
            border: 4px solid rgba(156, 39, 176, 0.1);
            border-top: 4px solid var(--accent-purple);
            border-radius: 50%;
            animation: spin 1s linear infinite;
            margin: 0 auto 15px;
        }

        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }

        .progress-container {
            background: rgba(156, 39, 176, 0.1);
            height: 10px;
            border-radius: 50px;
            overflow: hidden;
            margin: 15px 0;
        }

        .progress-bar {
            height: 100%;
            background: linear-gradient(90deg, var(--accent-purple), var(--accent-blue));
            width: 0%;
            transition: width 0.5s ease;
            border-radius: 50px;
        }

        .progress-text {
            text-align: center;
            color: var(--text-light);
            font-size: 0.9rem;
        }

        /* 图片结果 */
        .image-result-section img,
        .image-result img {
            width: 100%;
            border-radius: 16px;
            box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
            margin-bottom: 15px;
            transition: all 0.3s ease;
        }

        .image-result-section img:hover,
        .image-result img:hover {
            transform: scale(1.02);
            box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
        }

        .image-actions {
            display: flex;
            flex-direction: column;
            gap: 12px;
        }

        .error-section {
            padding: 20px;
            background: rgba(244, 67, 54, 0.1);
            border-radius: 12px;
            border-left: 4px solid #f44336;
            color: #c62828;
        }

        .result-section {
            margin-top: 15px;
        }

        /* 全屏查看图片 */
        .image-fullscreen-modal {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.9);
            z-index: 10000;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: zoom-out;
            animation: fadeInDown 0.3s ease;
        }

        .image-fullscreen-modal img {
            max-width: 90%;
            max-height: 90%;
            object-fit: contain;
            box-shadow: 0 0 50px rgba(0, 0, 0, 0.5);
            border-radius: 8px;
        }

        .image-fullscreen-close {
            position: absolute;
            top: 25px;
            right: 35px;
            font-size: 3rem;
            color: white;
            cursor: pointer;
            z-index: 10001;
            transition: transform 0.3s ease;
        }

        .image-fullscreen-close:hover {
            transform: scale(1.2);
        }

        /* 选中文字浮动工具栏 */
        .selection-toolbar {
            position: absolute;
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-blue));
            padding: 10px 18px;
            border-radius: 50px;
            box-shadow: 0 8px 32px rgba(156, 39, 176, 0.4);
            display: none;
            z-index: 10000;
            animation: toolbarFadeIn 0.3s ease;
        }

        @keyframes toolbarFadeIn {
            from {
                opacity: 0;
                transform: translateY(10px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        .selection-toolbar span {
            color: white;
            margin-right: 10px;
            font-size: 0.9rem;
        }

        .selection-toolbar button {
            background: white;
            color: var(--accent-purple);
            border: none;
            padding: 8px 16px;
            border-radius: 20px;
            cursor: pointer;
            font-weight: 600;
            margin: 0 5px;
            transition: all 0.3s ease;
            font-family: 'Noto Serif SC', serif;
        }

        .selection-toolbar button:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
        }

        /* 图片生成弹窗 */
        .image-generation-modal-overlay,
        .modal-overlay {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.5);
            backdrop-filter: blur(5px);
            z-index: 9998;
            display: none;
            animation: fadeInDown 0.3s ease;
        }

        .image-generation-modal,
        .modal-content {
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: var(--glass-bg);
            backdrop-filter: blur(20px);
            border-radius: 24px;
            border: 1px solid var(--glass-border);
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
            z-index: 9999;
            max-width: 900px;
            width: 90%;
            max-height: 90vh;
            overflow-y: auto;
            animation: fadeInUp 0.4s ease;
        }

        .modal-header {
            padding: 25px 30px;
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-blue));
            color: white;
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-radius: 24px 24px 0 0;
        }

        .modal-header h3,
        .modal-title {
            margin: 0;
            font-size: 1.3rem;
            font-weight: 600;
            font-family: 'ZCOOL XiaoWei', serif;
            letter-spacing: 2px;
        }

        .modal-close {
            background: rgba(255, 255, 255, 0.2);
            border: none;
            color: white;
            width: 36px;
            height: 36px;
            border-radius: 50%;
            cursor: pointer;
            font-size: 1.5rem;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.3s ease;
        }

        .modal-close:hover {
            background: rgba(255, 255, 255, 0.3);
            transform: scale(1.1);
        }

        .modal-body {
            padding: 30px;
        }

        .modal-footer {
            padding: 20px 30px;
            border-top: 1px solid rgba(156, 39, 176, 0.1);
            text-align: right;
        }

        .modal-ok-btn {
            padding: 12px 36px;
            border-radius: 50px;
            border: none;
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-blue));
            color: white;
            font-family: 'Noto Serif SC', serif;
            font-size: 1rem;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.4s ease;
            box-shadow: 0 4px 15px rgba(156, 39, 176, 0.3);
        }

        .modal-ok-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 25px rgba(156, 39, 176, 0.4);
        }

        /* 双栏面板 */
        .dual-panel {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 25px;
        }

        @media (max-width: 768px) {
            .dual-panel {
                grid-template-columns: 1fr;
            }
        }

        .panel {
            background: rgba(255, 255, 255, 0.5);
            border-radius: 16px;
            padding: 20px;
        }

        .panel-title {
            font-family: 'ZCOOL XiaoWei', serif;
            font-size: 1.1rem;
            color: var(--text-dark);
            margin-bottom: 15px;
            padding-bottom: 10px;
            border-bottom: 2px solid rgba(156, 39, 176, 0.2);
        }

        /* 世界观弹窗内容 */
        .worldview-content {
            line-height: 2;
        }

        .worldview-content h1,
        .worldview-content h2,
        .worldview-content h3 {
            color: var(--accent-purple);
            margin: 20px 0 10px;
        }

        .worldview-content p {
            margin-bottom: 15px;
        }

        /* 卡片阅读模式 */
        .card-reading-container {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: linear-gradient(135deg, var(--dream-pink) 0%, var(--dream-lavender) 25%, var(--dream-mint) 50%, var(--dream-peach) 75%, var(--dream-pink) 100%);
            background-size: 400% 400%;
            animation: gradientShift 20s ease infinite;
            z-index: 1000;
            overflow: hidden;
        }

        .card-reading-container.active {
            display: block;
        }

        .card-reading-inner {
            height: 100%;
            display: flex;
            flex-direction: column;
            padding: 30px;
            max-width: 800px;
            margin: 0 auto;
        }

        .card-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 15px 0;
            margin-bottom: 20px;
        }

        .card-title {
            font-family: 'ZCOOL XiaoWei', serif;
            font-size: 1.5rem;
            color: var(--text-dark);
        }

        .card-progress {
            color: var(--text-light);
            font-size: 0.9rem;
        }

        .card-exit-btn {
            padding: 10px 20px;
            border-radius: 50px;
            border: none;
            background: linear-gradient(135deg, #f44336, #d32f2f);
            color: white;
            font-family: 'Noto Serif SC', serif;
            font-size: 0.9rem;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s ease;
            box-shadow: 0 4px 15px rgba(244, 67, 54, 0.3);
        }

        .card-exit-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 25px rgba(244, 67, 54, 0.4);
        }

        .card-display-area {
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            overflow-y: auto;
            padding: 20px 0;
        }

        .content-card {
            background: var(--glass-bg);
            backdrop-filter: blur(20px);
            border-radius: 24px;
            border: 1px solid var(--glass-border);
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
            padding: 40px;
            max-width: 100%;
            transition: all 0.5s ease;
        }

        .card-content-text {
            font-size: 1.2rem;
            line-height: 2.2;
            color: var(--text-dark);
        }

        .card-content-text p {
            margin-bottom: 1.5em;
            text-indent: 2em;
        }

        .card-navigation-top,
        .card-navigation-bottom {
            display: flex;
            justify-content: center;
            gap: 20px;
            padding: 20px 0;
        }

        .nav-card-btn {
            padding: 14px 32px;
            border-radius: 50px;
            border: 2px solid transparent;
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-blue));
            color: white;
            font-family: 'Noto Serif SC', serif;
            font-size: 1rem;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.4s ease;
            box-shadow: 0 4px 15px rgba(156, 39, 176, 0.3);
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .nav-card-btn:hover:not(:disabled) {
            transform: translateY(-2px);
            box-shadow: 0 8px 25px rgba(156, 39, 176, 0.4);
        }

        .nav-card-btn:disabled {
            opacity: 0.5;
            cursor: not-allowed;
            transform: none !important;
        }

        .nav-card-btn-primary {
            background: linear-gradient(135deg, var(--accent-teal), #4db6ac);
            box-shadow: 0 4px 15px rgba(0, 150, 136, 0.3);
        }

        .nav-card-btn-primary:hover:not(:disabled) {
            box-shadow: 0 8px 25px rgba(0, 150, 136, 0.4);
        }

        /* 边界提示框 */
        .boundary-hint {
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: var(--glass-bg);
            backdrop-filter: blur(20px);
            border-radius: 20px;
            border: 1px solid var(--glass-border);
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
            padding: 30px;
            z-index: 2000;
            text-align: center;
            max-width: 400px;
            display: none;
            animation: fadeInUp 0.3s ease;
        }

        .boundary-hint.active {
            display: block;
        }

        .hint-icon {
            font-size: 3rem;
            margin-bottom: 15px;
        }

        .hint-text {
            color: var(--text-dark);
            font-size: 1.1rem;
            margin-bottom: 20px;
        }

        .hint-actions {
            display: flex;
            gap: 15px;
            justify-content: center;
        }

        .hint-cancel-btn,
        .hint-confirm-btn {
            padding: 12px 28px;
            border-radius: 50px;
            border: none;
            font-family: 'Noto Serif SC', serif;
            font-size: 1rem;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s ease;
        }

        .hint-cancel-btn {
            background: rgba(156, 39, 176, 0.1);
            color: var(--accent-purple);
        }

        .hint-cancel-btn:hover {
            background: rgba(156, 39, 176, 0.2);
        }

        .hint-confirm-btn {
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-blue));
            color: white;
            box-shadow: 0 4px 15px rgba(156, 39, 176, 0.3);
        }

        .hint-confirm-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 25px rgba(156, 39, 176, 0.4);
        }

        /* 侧边栏 */
        .side-panel {
            position: fixed;
            top: 0;
            width: 320px;
            height: 100%;
            background: var(--glass-bg);
            backdrop-filter: blur(20px);
            border: 1px solid var(--glass-border);
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
            z-index: 900;
            overflow-y: auto;
            transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
            padding: 25px;
        }

        #leftSidePanel {
            left: 0;
            transform: translateX(-100%);
        }

        #leftSidePanel.show {
            transform: translateX(0);
        }

        #rightSidePanel {
            right: 0;
            transform: translateX(100%);
        }

        #rightSidePanel.show {
            transform: translateX(0);
        }

        .side-panel-toggle {
            position: absolute;
            top: 50%;
            transform: translateY(-50%);
            width: 40px;
            height: 60px;
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-blue));
            color: white;
            border: none;
            cursor: pointer;
            font-size: 1.5rem;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.3s ease;
        }

        #leftSidePanel .side-panel-toggle {
            right: -40px;
            border-radius: 0 12px 12px 0;
        }

        #rightSidePanel .side-panel-toggle {
            left: -40px;
            border-radius: 12px 0 0 12px;
        }

        .side-panel-toggle:hover {
            box-shadow: 0 4px 15px rgba(156, 39, 176, 0.4);
        }

        /* 自动配画面板 */
        .auto-image-panel {
            margin-top: 30px;
            animation: fadeInUp 0.8s ease-out 0.4s both;
        }

        .position-item {
            margin-bottom: 15px;
            padding: 20px;
            background: rgba(255, 255, 255, 0.6);
            border-radius: 12px;
            border-left: 4px solid var(--accent-purple);
            transition: all 0.3s ease;
        }

        .position-item:hover {
            background: rgba(255, 255, 255, 0.8);
            transform: translateX(5px);
        }

        .progress-item {
            padding: 20px;
            background: rgba(255, 255, 255, 0.6);
            border-radius: 12px;
            margin-bottom: 15px;
            transition: all 0.3s ease;
        }

        .progress-item.pending {
            opacity: 0.6;
        }

        .progress-item.generating {
            border-left: 4px solid var(--accent-blue);
        }

        .progress-item.completed {
            border-left: 4px solid var(--accent-teal);
        }

        .progress-item.failed {
            border-left: 4px solid #f44336;
        }

        .progress-item-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
        }

        .progress-item-title {
            font-weight: 600;
            color: var(--text-dark);
        }

        .progress-item-status {
            font-size: 0.85rem;
            padding: 4px 12px;
            border-radius: 12px;
            font-weight: 600;
        }

        .progress-item-status.pending {
            background: rgba(156, 39, 176, 0.1);
            color: var(--accent-purple);
        }

        .progress-item-status.generating {
            background: rgba(63, 81, 181, 0.1);
            color: var(--accent-blue);
        }

        .progress-item-status.completed {
            background: rgba(0, 150, 136, 0.1);
            color: var(--accent-teal);
        }

        .progress-item-status.failed {
            background: rgba(244, 67, 54, 0.1);
            color: #f44336;
        }

        .progress-item-prompt {
            font-size: 0.85rem;
            color: var(--text-light);
            line-height: 1.6;
        }

        .progress-item-bar {
            height: 8px;
            background: rgba(156, 39, 176, 0.1);
            border-radius: 4px;
            overflow: hidden;
            margin-top: 10px;
        }

        .progress-item-bar-fill {
            height: 100%;
            background: linear-gradient(90deg, var(--accent-purple), var(--accent-blue));
            width: 0%;
            transition: width 0.5s ease;
            border-radius: 4px;
        }

        .progress-item-image {
            margin-top: 15px;
        }

        .progress-item-image img {
            width: 100%;
            border-radius: 8px;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
        }

        .progress-item-error {
            margin-top: 10px;
            padding: 10px;
            background: rgba(244, 67, 54, 0.1);
            border-radius: 8px;
            color: #c62828;
            font-size: 0.85rem;
        }

        .generated-image-item {
            padding: 20px;
            background: rgba(255, 255, 255, 0.6);
            border-radius: 12px;
            margin-bottom: 20px;
        }

        .generated-image-item img {
            width: 100%;
            border-radius: 8px;
            margin: 15px 0;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
        }

        /* 字符统计 */
        .character-count {
            text-align: right;
            font-size: 0.85rem;
            color: var(--text-light);
            margin-top: 8px;
            opacity: 0.8;
        }

        /* Markdown 内容样式 */
        .markdown-content h1,
        .markdown-content h2,
        .markdown-content h3,
        .markdown-content h4 {
            color: var(--accent-purple);
            margin: 25px 0 15px;
            font-family: 'ZCOOL XiaoWei', serif;
        }

        .markdown-content p {
            margin-bottom: 1.5em;
        }

        .markdown-content pre {
            background: rgba(0, 0, 0, 0.05);
            border-radius: 8px;
            padding: 15px;
            overflow-x: auto;
            margin: 15px 0;
        }

        .markdown-content code {
            background: rgba(156, 39, 176, 0.1);
            padding: 2px 8px;
            border-radius: 4px;
            font-size: 0.9em;
        }

        .markdown-content pre code {
            background: none;
            padding: 0;
        }

        .markdown-content blockquote {
            border-left: 4px solid var(--accent-purple);
            padding-left: 20px;
            margin: 15px 0;
            color: var(--text-light);
        }

        .markdown-content ul,
        .markdown-content ol {
            margin: 15px 0;
            padding-left: 30px;
        }

        .markdown-content li {
            margin: 8px 0;
        }

        /* 响应式设计 */
        @media (max-width: 768px) {
            .container {
                padding: 20px 15px;
            }

            .header h1 {
                font-size: 1.8rem;
                letter-spacing: 2px;
            }

            .glass-card {
                padding: 25px;
            }

            .chapter-title {
                font-size: 1.5rem;
            }

            .navigation {
                flex-direction: column;
                align-items: center;
            }

            .nav-button {
                width: 100%;
                max-width: 250px;
            }

            .reading-settings-bar {
                flex-direction: column;
                align-items: flex-start;
            }

            .content-card {
                padding: 25px;
            }

            .card-content-text {
                font-size: 1.05rem;
            }
        }
    </style>'''

# Replace the original style section with dreamy CSS
# First, remove everything from <style> to </style>
# The original starts after the highlight.js links

# Find where the original style starts and ends
style_start = content.find('<style>')
style_end = content.find('</style>', style_start) + 8

# Find the title tag to update
title_start = content.find('<title>')
title_end = content.find('</title>') + 8

# Find the head section to add fonts
head_end = content.find('</head>')

# Create the new content
new_content = content[:title_start] + '<title>梦境阅读 · 章节详情</title>' + content[title_end:style_start]

# Add floating orbs right after <body> tag
body_start = new_content.find('<body>') + 6
floating_orbs = '''
    <!-- 梦幻气泡背景 -->
    <div class="floating-orbs">
        <div class="orb orb-1"></div>
        <div class="orb orb-2"></div>
        <div class="orb orb-3"></div>
        <div class="orb orb-4"></div>
    </div>
'''

# Insert floating orbs and dreamy CSS
new_content = new_content[:body_start] + floating_orbs + dreamy_css + new_content[style_end:]

# Now update class names and structure where needed
# Replace container class to use glass-card pattern
new_content = new_content.replace('<div class="container">', '<div class="container"><div class="glass-card">')

# Need to find where the container ends and add closing </div>
# But this is tricky - let's instead make other targeted replacements

# Update header
new_content = new_content.replace('<div class="header">', '</div><div class="header">')  # Close glass-card first

# Update various button classes and other elements as needed

# Make sure the main content is wrapped properly
# Let's also update the chapter content area

# Write the transformed content
with open(r'E:\WorkSpace\articleCollect\src\main\resources\templates\chapter-detail.html', 'w', encoding='utf-8') as f:
    f.write(new_content)

print('Transformation complete!')
