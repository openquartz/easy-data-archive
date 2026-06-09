# 归档任务时间格式标准化

- **日期**: 2026-06-09
- **状态**: 已批准

## 背景与目标

当前归档任务接口返回的时间字段（`startTime`、`endTime`、`heartbeatTime`、`logTime` 等）格式为 ISO 8601 UTC 字符串（如 `2026-06-07T06:12:04.000+00:00`），不直观且时区为 UTC 与中国用户使用习惯不符。

目标：后端统一返回 `yyyy-MM-dd HH:mm:ss` 格式，时区为东八区（`GMT+8`），前端直接展示无需任何处理。

## 方案选择

### 方案 A：全局 Jackson 配置修改（不选）
- **做法**: 修改 `GeneralJacksonHandler` 中的 `SimpleDateFormat` 和 `TimeZone`
- **优点**: 一处改动，全局生效
- **缺点**: 改动影响范围不可控，所有 Date 字段均受影响，无法针对特定接口定制

### 方案 B：Entity 加 `@JsonFormat` 注解（不选）
- **做法**: 直接在 `ArchiveGroupExecuteTask`、`ArchiveTaskLog` 上加注解
- **优点**: 改动最少
- **缺点**: Entity 同时承担持久化和 API 响应职责，违反分层原则

### 方案 C：新增 VO 类 + Service 层转换（采用）
- **做法**: 新增 `TaskVO`、`TaskLogVO`，Service 层 Entity → VO 转换
- **优点**: 分层清晰，Entity 保持纯净，VO 专用于 API 响应
- **缺点**: 略多几个文件

## 实现计划

### 1. 新增 VO 类

路径: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/`

#### TaskVO.java
对应 `ArchiveGroupExecuteTask`，包含所有需要返回给前端的字段，时间字段加 `@JsonFormat` 注解：

```java
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
private Date startTime;
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
private Date endTime;
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
private Date heartbeatTime;
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
private Date createdTime;
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
private Date updatedTime;
```

#### TaskLogVO.java
对应 `ArchiveTaskLog`：

```java
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
private Date logTime;
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
private Date createdTime;
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
private Date updatedTime;
```

### 2. 新增转换工具方法

在 `easyarchive-starter` 模块下新增 `TaskConvertUtils.java`，提供：

- `TaskVO fromEntity(ArchiveGroupExecuteTask entity)`
- `TaskLogVO fromEntity(ArchiveTaskLog entity)`

### 3. Service 层改造

`ArchiveTaskLogServiceImpl.java`：

- `queryTasks()`: `List<ArchiveGroupExecuteTask>` → `List<TaskVO>` 后放入 result map
- `queryTaskById()`: 返回类型从 `Object` 改为 `TaskVO`
- `queryLogsByTaskId()`: `List<ArchiveTaskLog>` → `List<TaskLogVO>` 后放入 result map

Service 接口 `ArchiveTaskLogService.java` 对应签名同步更新。

### 4. Controller 层类型收窄

`ArchiveTaskLogController.java`：

- `getTask()` 返回类型从 `ApiResponse<Object>` 改为 `ApiResponse<TaskVO>`

### 5. 前端

零改动。前端类型定义已是 `string`，Jackson 序列化为 `yyyy-MM-dd HH:mm:ss` 后直接渲染即可。

## 文件清单

| 操作 | 文件路径 |
|------|---------|
| 新增 | `starter/.../model/dto/TaskVO.java` |
| 新增 | `starter/.../model/dto/TaskLogVO.java` |
| 新增 | `starter/.../utils/TaskConvertUtils.java` |
| 修改 | `starter/.../service/ArchiveTaskLogService.java` |
| 修改 | `starter/.../service/impl/ArchiveTaskLogServiceImpl.java` |
| 修改 | `starter/.../controller/ArchiveTaskLogController.java` |

## 风险与影响

- 仅影响 `/api/v1/task-log/*` 接口，其他接口不受影响
- Entity 层无任何改动，持久化逻辑不变
- 前端无需修改
