# EasyArchive 项目 Harness 配置规范

本文档描述了 EasyArchive 项目的 Claude Code 自动化配置规范。

## 配置文件结构

项目包含两个级别的配置文件：

### 1. 项目级配置 (`.claude/settings.json`)
- **作用范围**: 整个项目团队共享
- **Git 管理**: ✅ 提交到版本控制
- **用途**: 定义项目标准的权限、钩子和行为规范

### 2. 本地配置 (`.claude/settings.local.json`)
- **作用范围**: 个人开发环境
- **Git 管理**: ❌ 被 `.gitignore` 排除
- **用途**: 个人偏好设置和环境变量

## 权限配置

### 允许的操作

#### Git 操作
```json
"Bash(git add *)",
"Bash(git commit *)",
"Bash(git push *)",
"Bash(git status)",
"Bash(git diff *)",
"Bash(git log *)",
"Bash(git checkout *)",
"Bash(git branch *)"
```

#### Maven 构建操作
```json
"Bash(mvn clean *)",
"Bash(mvn compile *)",
"Bash(mvn test *)",
"Bash(mvn install *)",
"Bash(mvn package *)"
```

#### 文件操作
```json
"Read(easyarchive-*/**/*.java)",
"Read(easyarchive-*/**/*.xml)",
"Read(easyarchive-*/**/*.properties)",
"Edit(easyarchive-*/**/*.java)",
"Edit(easyarchive-*/**/*.xml)",
"Edit(easyarchive-*/**/*.properties)",
"Write(easyarchive-*/**/*.java)",
"Write(easyarchive-*/**/*.xml)",
"Write(easyarchive-*/**/*.properties)"
```

### 禁止的操作
```json
"deny": [
  "Bash(rm -rf *)",
  "Bash(docker *)",
  "Bash(kubectl *)"
]
```

## 钩子配置

### SessionStart 钩子
在会话开始时自动执行：
- 显示项目环境信息
- 检查 Java 和 Maven 版本
- 显示 Git 状态

```json
"SessionStart": [
  {
    "hooks": [
      {
        "type": "command",
        "command": "echo '=== EasyArchive 项目环境 ===' && echo 'Java 版本:' && java -version 2>&1 | head -1 && echo 'Maven 版本:' && mvn -v | head -1 && echo '项目状态:' && git status --porcelain | wc -l && echo '个未提交文件'",
        "timeout": 15,
        "statusMessage": "检查项目环境..."
      }
    ]
  }
]
```

### PostToolUse 钩子
在文件写入或编辑后自动执行：
- 检测 Java、XML、Properties 文件变更
- 自动触发 Maven 编译验证
- 显示编译状态信息

```json
"PostToolUse": [
  {
    "matcher": "Write|Edit",
    "hooks": [
      {
        "type": "command",
        "command": "file=$(jq -r '.tool_response.filePath // .tool_input.file_path'); if [[ $file =~ \.(java|xml|properties)$ ]]; then echo '编译变更的文件...' && mvn compile -q -DskipTests -pl easyarchive-core -Dmaven.main.skip 2>/dev/null || true; fi",
        "timeout": 45,
        "statusMessage": "验证代码编译..."
      }
    ]
  }
]
```

### PreToolUse 钩子
在执行 Bash 命令前记录：
- 记录 Git 操作到日志文件
- 便于审计和追踪

```json
"PreToolUse": [
  {
    "matcher": "Bash",
    "hooks": [
      {
        "type": "command",
        "command": "echo '[DATE] Bash command executed' >> ~/.claude/bash-commands.log 2>/dev/null || true",
        "if": "Bash(git *)",
        "timeout": 5
      }
    ]
  }
]
```

### Stop 钩子
在会话结束时执行：
- 显示项目最终状态
- 统计未提交更改数量

```json
"Stop": [
  {
    "hooks": [
      {
        "type": "command",
        "command": "echo '会话已结束。项目状态:' && git status --porcelain | wc -l && echo '个未提交更改'",
        "timeout": 5
      }
    ]
  }
]
```

## 环境变量配置

```json
"env": {
  "JAVA_HOME": "/usr/lib/jvm/java-11-openjdk",
  "MAVEN_OPTS": "-Xmx2g -XX:+UseG1GC"
}
```

## UI 和体验配置

```json
{
  "todoFeatureEnabled": true,           // 启用任务追踪面板
  "verbose": true,                      // 显示详细输出
  "autoCompactEnabled": true,           // 启用自动压缩
  "autoCompactWindow": 200000,          // 压缩窗口大小
  "cleanupPeriodDays": 30,              // 清理周期（天）
  "respectGitignore": true,             // 遵守 .gitignore
  "spinnerTipsEnabled": true,           // 启用加载提示
  "syntaxHighlightingDisabled": false,  // 启用语法高亮
  "terminalProgressBarEnabled": true,   // 启用终端进度条
  "showMessageTimestamps": false,       // 不显示消息时间戳
  "showTurnDuration": true,             // 显示处理时长
  "fileCheckpointingEnabled": true      // 启用文件检查点
}
```

## 语言模型配置

```json
"language": "chinese",        // 响应语言：中文
"model": "claude-sonnet-4-6"   // 使用的模型
```

## 最佳实践

### 1. 权限管理
- **最小权限原则**: 只授予必要的操作权限
- **危险操作禁止**: 明确禁止 `rm -rf`、`docker` 等危险命令
- **默认模式**: 设置为 `default`，需要时手动授权

### 2. 钩子设计
- **超时设置**: 所有钩子都配置适当的超时时间
- **错误处理**: 使用 `|| true` 避免钩子失败影响主任务
- **状态消息**: 提供清晰的执行状态信息
- **条件执行**: 使用 `if` 条件限制钩子执行范围

### 3. 文件组织
- **项目配置**: 共享配置放在 `settings.json`
- **个人配置**: 个人偏好放在 `settings.local.json`
- **Git 忽略**: 本地配置文件被 `.gitignore` 排除

### 4. 性能优化
- **Maven 优化**: 使用 `-q` 静默模式减少输出
- **编译跳过**: 使用 `-DskipTests` 跳过测试
- **模块编译**: 使用 `-pl` 指定特定模块

## 使用指南

### 初始设置
1. 克隆项目后，Claude Code 会自动加载配置
2. 首次使用会显示环境检查信息
3. 所有 Git 和 Maven 操作都会被记录和验证

### 日常开发
1. **代码编辑**: 保存文件后自动触发编译验证
2. **构建测试**: Maven 命令会被记录到日志
3. **状态查看**: 通过任务面板查看待办事项
4. **会话管理**: 结束时显示项目状态摘要

### 故障排除

#### 钩子不执行
- 检查 `.claude/settings.json` 语法是否正确
- 验证命令路径和权限
- 查看超时设置是否合理

#### 权限问题
- 检查 `settings.local.json` 中的个人权限
- 确认项目级权限配置
- 验证命令匹配模式

## 安全考虑

1. **配置文件保护**: 本地配置文件不包含敏感信息
2. **命令过滤**: 危险命令被明确禁止
3. **环境隔离**: 使用项目特定的环境变量
4. **日志记录**: 所有操作都被记录用于审计

---

*此配置规范适用于 EasyArchive 项目的 Java/Spring Boot 开发环境*
*最后更新: 2026-05-24*