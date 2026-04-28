# 贡献指南

感谢你考虑为梦境文阁做出贡献！

## 如何贡献

### 报告 Bug

如果你发现了 bug，请通过 [GitHub Issues](https://github.com/zenithon/dream-library/issues) 提交报告。

在提交 bug 报告时，请包含：

- 清晰的标题和描述
- 复现步骤
- 预期行为和实际行为
- 截图（如果适用）
- 环境信息（操作系统、Java 版本等）

### 提出新功能

如果你有新功能的想法，请先创建一个 Issue 进行讨论。

### 提交代码

1. **Fork 本仓库**

2. **克隆你的 Fork**
   ```bash
   git clone https://github.com/YOUR_USERNAME/dream-library.git
   cd dream-library
   ```

3. **创建功能分支**
   ```bash
   git checkout -b feature/amazing-feature
   ```

4. **进行更改**
   - 确保代码风格一致
   - 添加必要的测试
   - 更新相关文档

5. **提交更改**
   ```bash
   git add .
   git commit -m "feat: add amazing feature"
   ```

   提交信息格式：
   - `feat:` 新功能
   - `fix:` Bug 修复
   - `docs:` 文档更新
   - `style:` 代码格式调整
   - `refactor:` 代码重构
   - `test:` 测试相关
   - `chore:` 构建/工具相关

6. **推送到你的 Fork**
   ```bash
   git push origin feature/amazing-feature
   ```

7. **创建 Pull Request**
   - 描述你的更改
   - 关联相关的 Issue
   - 等待代码审查

## 开发指南

### 环境设置

```bash
# 安装依赖
mvn clean install

# 运行应用
mvn spring-boot:run

# 运行测试
mvn test
```

### 代码风格

- 遵循 Java 编码规范
- 使用有意义的变量和方法名
- 添加必要的注释
- 保持方法简短

### 分支命名

- `feature/` - 新功能
- `fix/` - Bug 修复
- `docs/` - 文档更新
- `refactor/` - 代码重构

## 行为准则

- 尊重所有贡献者
- 接受建设性的批评
- 关注对社区最有利的事情

## 许可证

通过贡献代码，你同意你的代码将根据 [Apache License 2.0](LICENSE) 进行许可。

---

再次感谢你的贡献！
