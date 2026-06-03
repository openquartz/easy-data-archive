# 管理界面操作日志设计文档

> 日期: 2026-06-04
> 模块: easyarchive-starter
> 范围: 管理界面写操作审计

## 1. 目标

为管理界面发起的写操作建立一套面向运营人员的操作日志记录能力，统一记录：

- 按钮名称
- 操作时间
- 操作人
- 操作内容
- 请求结果
- 失败原因

其中操作内容必须输出为中文、可直接阅读的变更描述。修改类操作使用统一格式：

`"XX" 从 "xx" 修改为："yy"; "XX2" 从 "xx" 修改为："yy"`

首批实现需要完整覆盖以下模块：

- 数据源
- 归档分组
- 归档分组项

整体设计必须面向全部管理端写操作可扩展，后续模块接入时不重做框架，只补模块映射和内容模板。

## 2. 当前状态

项目已经具备以下基础：

- 数据库存在 `sys_operation_log` 表
- 存在 `@OperationLog` 注解
- 存在 `OperationLogAspect`
- Spring Security 已经能够通过 `DataPermissionService#getCurrentUser()` 获取当前登录用户

但当前实现仍有明显缺口：

- `OperationLogAspect` 仅输出控制台日志，不落库
- `sys_operation_log` 缺少按钮名称、中文操作内容、业务对象摘要、失败原因等字段
- 现有日志无法表达“字段从旧值改为新值”的业务语义
- 现有 AOP 不掌握最终写库值，无法正确处理 `mergeExisting()`、状态切换、触发任务、测试连接等非标准 CRUD 场景

因此本次设计不采用“纯 AOP 自动比对”的方案，而采用“统一采集 + 业务显式提供变更集”的方式。

## 3. 设计原则

### 3.1 面向运营可读

日志首先服务于运营人员，不输出字段英文名、枚举编码、数据库 ID 等难理解内容。所有核心内容必须转为中文展示。

### 3.2 请求级统一采集

一次 HTTP 写请求的操作者、时间、URI、方法、耗时、结果状态应由统一切面采集，不允许每个模块各自散落实现。

### 3.3 业务级显式变更

修改前后字段差异、按钮文案、业务对象摘要、敏感字段脱敏规则由业务层显式提供。最懂业务语义的位置是 service，而不是 AOP。

### 3.4 成功失败都记录

成功操作要记录变更内容；失败操作也要记录按钮、对象、失败原因，方便运营追溯。

### 3.5 增量接入

本次先完整打通“数据源 + 分组 + 分组项”，后续其它管理模块按相同模式接入，不引入第二套日志机制。

## 4. 方案选型

对比三个可选方案：

### 4.1 方案 A：纯 AOP 注解驱动

在 controller 方法上加注解，由切面统一取参数、查旧值、生成 diff、落库。

优点：

- 接入看起来最少
- controller 层统一

缺点：

- 无法稳定识别最终写入值
- 对部分更新、合并默认值、删除、测试连接、触发任务等场景支持差
- 复杂业务语义会导致切面臃肿且脆弱

### 4.2 方案 B：AOP 负责统一采集，业务层显式提供变更集

切面统一采集请求元信息，service 在真正写操作处查询旧值、构造最终新值、生成中文变更内容，最终仍由统一日志服务落库。

优点：

- 日志内容准确
- 易于测试
- 能稳定支持新增、修改、状态切换、删除、测试连接、触发任务
- 符合当前项目 service 层承担业务规则的现有结构

缺点：

- 首批接入会新增若干 presenter 和记录器类

### 4.3 方案 C：Mapper/Repository 级通用审计

在数据库写入前后统一抓实体快照并自动 diff。

优点：

- 理论覆盖范围大

缺点：

- 当前项目更新入口不统一，存在 partial update 和非 CRUD 写操作
- 很难生成面向运营的中文内容
- 首版成本和风险最高

### 4.4 最终选择

采用方案 B：

- `@OperationLog` 负责定义入口元信息
- `OperationLogAspect` 负责统一采集请求上下文和最终落库
- 业务 service 负责查旧值并生成 `OperationChangeSet`
- presenter 负责中文字段名、值翻译、内容模板和脱敏规则

## 5. 日志模型设计

### 5.1 复用主表

复用现有 `sys_operation_log` 作为管理界面操作日志主表，不再新增第二张运营日志表。原因：

- 该表已经覆盖用户、请求、耗时、结果等通用审计字段
- 可以在不破坏整体审计入口的前提下补足运营视角字段

### 5.2 表结构扩展

在 `sys_operation_log` 上新增以下字段：

| 字段 | 类型建议 | 说明 |
| --- | --- | --- |
| `button_name` | `VARCHAR(128)` | 管理界面按钮名称，如“新增数据源” |
| `biz_type` | `VARCHAR(64)` | 业务对象类型，如 `DATASOURCE` |
| `biz_id` | `BIGINT` | 业务对象主键 |
| `biz_key` | `VARCHAR(255)` | 业务对象摘要，如数据源编码或分组编码 |
| `content` | `TEXT` | 中文操作内容 |
| `error_message` | `VARCHAR(500)` | 失败原因摘要 |

保留并继续使用原字段：

- `user_id`
- `module_code`
- `action_code`
- `request_uri`
- `request_method`
- `request_param`
- `response_code`
- `result_status`
- `cost_ms`
- `client_ip`
- `operate_time`
- `created_time`

建议补充索引：

- `idx_biz_type_id(biz_type, biz_id)`
- `idx_module_action_time(module_code, action_code, operate_time)`

### 5.3 实体与持久化层

新增以下组件：

- `SysOperationLog`
- `SysOperationLogMapper`
- `SysOperationLogMapper.xml`
- `SysOperationLogRepository` 或 `SysOperationLogService`

职责要求：

- 仅承担操作日志持久化
- 不在 mapper 或 repository 内拼接业务中文内容
- 不让业务 service 直接写 SQL

## 6. 记录内容规则

### 6.1 新增类

格式：

`新增数据源："数据源编码" 为 "mysql_archive"; "数据源名称" 为 "MySQL归档库"; "数据源类型" 为 "MySQL"`

只输出关键字段，不强制输出全部列。敏感字段按脱敏规则输出。

### 6.2 修改类

格式：

`"分组名称" 从 "订单归档" 修改为："订单归档华东"; "目标数据源" 从 "archive_old" 修改为："archive_new"`

规则：

- 只输出真正变化的字段
- 按业务重要性排序输出，不按 Java 字段顺序原样输出
- 旧值和新值都必须经过中文翻译和脱敏

### 6.3 状态切换类

格式：

`执行“启用分组”操作，将 "启用状态" 从 "停用" 修改为："启用"`

或

`"状态" 从 "正常" 修改为："禁用"`

### 6.4 删除类

格式：

`删除分组："分组编码" 为 "order_archive"; "分组名称" 为 "订单归档"`

删除日志以删除前快照为准，不尝试输出“修改为 null”。

### 6.5 动作类

用于测试连接、触发任务、取消任务等不存在成组字段 diff 的操作：

- `测试连接数据源："数据源编码" 为 "mysql_archive"; 结果为："成功"`
- `触发归档分组："分组编码" 为 "order_archive"; "分组名称" 为 "订单归档"`
- `取消运行任务："分组编码" 为 "order_archive"; "取消原因" 为 "人工终止"`

### 6.6 失败类

失败时仍写日志，格式示例：

`执行“编辑分组”失败，目标分组："order_archive"`

`error_message` 记录异常摘要，如：

`分组存在执行中的任务，无法编辑`

## 7. 中文字段映射与值翻译

### 7.1 字段中文名集中维护

字段中文名不得分散硬编码在多个 service 内。应由 presenter 或专用映射类集中维护。

首批字段映射至少包括：

#### 数据源

- `datasourceCode` -> `数据源编码`
- `datasourceName` -> `数据源名称`
- `datasourceType` -> `数据源类型`
- `jdbcUrl` -> `JDBC地址`
- `username` -> `用户名`
- `status` -> `状态`
- `remark` -> `备注`

#### 归档分组

- `groupCode` -> `分组编码`
- `groupName` -> `分组名称`
- `sourceDatasourceId` -> `源数据源`
- `targetDatasourceId` -> `目标数据源`
- `enableStatus` -> `启用状态`
- `remark` -> `备注`

#### 归档分组项

- `sourceTable` -> `来源表`
- `targetTable` -> `目标表`
- `priority` -> `优先级`
- `fetchSql` -> `查询SQL`
- `deleteWhere` -> `删除条件`
- `stepCount` -> `步长`
- `stepRounds` -> `滚动步长`
- `stepMinutes` -> `时间滚动步长`
- `pauseMs` -> `暂停时间`
- `enableWrite` -> `写入目标`
- `enableClean` -> `清理源表`
- `enableStatus` -> `启用状态`
- `idColumn` -> `ID字段`
- `startId` -> `开始ID`
- `endId` -> `结束ID`
- `startTime` -> `开始时间`
- `keepDay` -> `保留天数`

### 7.2 值翻译规则

以下值不能直接原样输出：

- 状态码
- 布尔型开关
- 枚举编码
- 数据源 ID
- 分组 ID

翻译规则示例：

- `enableStatus: 0 -> 启用, 1 -> 停用`
- `enableWrite: 0 -> 是, 1 -> 否`
- `enableClean: 0 -> 是, 1 -> 否`
- `datasourceType: MYSQL -> MySQL`
- `status` 需按数据源状态字典翻译为中文
- `sourceDatasourceId` / `targetDatasourceId` 输出数据源名称或编码，而不是数值 ID

### 7.3 敏感与长文本字段

以下字段需要特殊处理：

- `passwordCipher`
  不记录旧值和新值，只输出：`"密码" 已更新`
- `fetchSql`
- `deleteWhere`
- 可能带敏感参数的 `jdbcUrl`

规则：

- 长文本统一截断到安全长度，例如 300 或 500 字符
- 密码类字段不落明文
- JDBC 地址若含敏感参数，输出前脱敏

## 8. 架构设计

### 8.1 统一切面层

升级现有 `OperationLogAspect`，职责如下：

- 拦截带 `@OperationLog` 的管理端写接口
- 初始化本次请求的日志上下文
- 采集：
  - 操作时间
  - 请求路径
  - HTTP 方法
  - 客户端 IP
  - 耗时
  - 当前用户
- 捕获成功或异常结果
- 在 `finally` 中统一落库并清理线程上下文

切面不负责：

- 查业务旧值
- 自动反射生成复杂 diff
- 字段中文翻译

### 8.2 请求上下文层

新增 `OperationLogContextHolder`，基于 `ThreadLocal` 保存本次请求的日志上下文。

建议上下文字段：

- `moduleCode`
- `actionCode`
- `buttonName`
- `bizType`
- `bizId`
- `bizKey`
- `content`
- `requestParamSummary`
- `responseCode`
- `resultStatus`
- `errorMessage`

规则：

- 由 AOP 初始化和清理
- 由业务 service 或 recorder 补全内容
- 若业务未显式写入 `content`，允许回退为注解描述或默认文案

### 8.3 业务记录层

新增轻量日志记录器组件，例如：

- `OperationLogRecorder`
- `OperationLogCommand`
- `OperationFieldChange`

职责：

- 将业务层变更描述写入当前上下文
- 对修改类操作，根据 `before`、`after` 和字段规则生成变更列表
- 对新增、删除、动作类操作按模板生成内容

### 8.4 模块展示层

新增 `presenter` 或 `formatter` 组件，负责模块级中文表达：

- `DatasourceOperationLogPresenter`
- `ArchiveGroupOperationLogPresenter`
- `ArchiveGroupItemOperationLogPresenter`

职责：

- 维护字段中文名映射
- 值翻译
- 脱敏与截断
- 业务摘要生成
- `buttonName`、`bizKey`、`content` 模板

presenter 不直接写数据库，只返回 `OperationLogCommand` 或等价结果。

## 9. 调用链设计

一次管理端写请求的调用链如下：

1. Controller 接收写请求
2. `OperationLogAspect` 创建请求级日志上下文
3. Service 进入业务逻辑
4. Service 查询旧值快照
5. Service 组装最终新值
6. Service 调用 presenter 生成 `OperationLogCommand`
7. Service 通过 `OperationLogRecorder` 写入当前上下文
8. Controller 返回成功或抛出异常
9. `OperationLogAspect` 在结束阶段统一落库到 `sys_operation_log`

这样可以保证：

- 元信息只采集一次
- 业务变更内容由最合适的位置决定
- 成功和失败共用统一落库路径

## 10. 首批接入范围

### 10.1 数据源

接口：

- `POST /api/v1/archive/datasources`
- `PUT /api/v1/archive/datasources/{id}`
- `PATCH /api/v1/archive/datasources/{id}/status`
- `POST /api/v1/archive/datasources/test`

### 10.2 归档分组

接口：

- `POST /api/v1/archive/groups`
- `PUT /api/v1/archive/groups/{id}`
- `PATCH /api/v1/archive/groups/{id}/status`
- `DELETE /api/v1/archive/groups/{id}`
- `POST /api/v1/archive/groups/{id}/trigger`
- `POST /api/v1/archive/groups/{id}/cancel-active-task`

### 10.3 归档分组项

按 ID 分组项：

- `POST /api/v1/archive/groups/{groupId}/items/id`
- `PUT /api/v1/archive/groups/{groupId}/items/id/{itemId}`
- `PATCH /api/v1/archive/groups/{groupId}/items/id/{itemId}/status`
- `DELETE /api/v1/archive/groups/{groupId}/items/id/{itemId}`

按时间分组项：

- `POST /api/v1/archive/groups/{groupId}/items/time`
- `PUT /api/v1/archive/groups/{groupId}/items/time/{itemId}`
- `PATCH /api/v1/archive/groups/{groupId}/items/time/{itemId}/status`
- `DELETE /api/v1/archive/groups/{groupId}/items/time/{itemId}`

## 11. 旧值查询与内容生成策略

### 11.1 旧值查询位置

旧值必须在 service 内查询，不在 controller，不在 aspect。

原因：

- service 已掌握权限、参数校验和最终实体类型
- service 更靠近实际写入点
- 可直接复用 `mergeExisting()` 等最终合并结果
- 单元测试可控

### 11.2 最终新值快照

对于更新类接口，日志必须基于“最终写入数据库的值”生成，而不是原始请求体。

例如当前存在如下模式：

- 请求对象允许部分字段为空
- service 会用旧值补齐空字段
- service 在写库前完成字段标准化和默认值处理

日志生成点应在这些处理完成之后。

### 11.3 删除类操作

删除日志以删除前快照生成内容，不再构造 after 对象。

### 11.4 动作类操作

动作类不强制构造字段 diff，直接由 presenter 生成中文摘要。

## 12. 失败日志策略

操作失败时也必须记录。

要求：

- `result_status = 1`
- 若能识别按钮和对象，则仍写入 `button_name`、`biz_type`、`biz_id`、`biz_key`
- `content` 记录“尝试执行了什么”
- `error_message` 记录异常摘要

失败日志应覆盖以下典型场景：

- 分组存在执行中任务，禁止修改或删除
- 数据校验失败
- 权限校验失败
- 触发任务失败
- 测试连接失败

失败日志不要求输出完整字段 diff，但必须保证运营能知道：

- 谁点了哪个按钮
- 操作的是哪个对象
- 为什么失败

## 13. 首批文件落点

### 13.1 改造现有文件

- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/aspect/OperationLogAspect.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/annotation/OperationLog.java`

### 13.2 新增操作日志基础层

建议新增目录：

`easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/`

目录下包含：

- 上下文 holder
- recorder
- command / change model
- 通用 formatter
- repository / service
- presenter 基类和模块实现

### 13.3 新增持久化层

- `SysOperationLog`
- `SysOperationLogMapper`
- `SysOperationLogMapper.xml`
- 对应数据库 migration

### 13.4 首批接入业务文件

- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveConnectionServiceImpl.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImpl.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImpl.java`
- 必要时补充对应 controller 的注解元信息

## 14. 测试设计

### 14.1 单元测试

覆盖：

- 修改类 diff 仅输出变化字段
- 字段中文名映射正确
- 状态码、布尔值、枚举值翻译正确
- 密码与敏感字段脱敏正确
- 长 SQL 截断正确

### 14.2 Service 测试

覆盖：

- 旧值查询后生成正确 content
- `mergeExisting()` 后最终值被正确记录
- 删除类、状态切换类、动作类日志正确
- 失败场景下仍能生成失败日志上下文

### 14.3 Web/契约测试

覆盖：

- 典型写接口成功后，日志持久化被触发
- 典型写接口失败后，错误摘要被记录
- 按钮名称、模块编码、action 编码来源正确

### 14.4 核心断言

测试不只验证“插入了一条日志”，更要验证日志内容准确，例如：

- `"状态" 从 "正常" 修改为："禁用"`
- `"分组名称" 从 "订单归档" 修改为："订单归档华东"`
- `"目标表" 从 "t_order_archive" 修改为："t_order_archive_v2"; "步长" 从 "5000" 修改为："10000"`

## 15. 扩展路径

首批完成后，其余管理端写接口按同一模板接入：

1. controller 补 `@OperationLog`
2. service 查询旧值
3. 新增或复用 presenter
4. 构造 `OperationLogCommand`
5. 补测试

后续适合直接接入的模块包括：

- 用户管理
- 用户数据源权限
- 系统配置类接口
- 任务重试、任务终止等管理动作

## 16. 非目标

本次设计不包含以下内容：

- 历史日志查询页面改版
- 通用实体自动审计框架
- 所有旧模块一次性全部接入
- 细粒度字段级权限脱敏中心

## 17. 实施结论

本次功能应采用“请求级统一采集 + 业务层显式变更 + presenter 中文输出 + 主表复用扩字段”的方案。

该方案能够在当前项目结构下稳定满足以下要求：

- 记录按钮、操作时间、操作人、操作内容
- 输出运营可读中文格式
- 正确支持新增、修改、状态切换、删除、测试连接、触发/取消任务
- 成功失败都可追踪
- 后续模块扩展成本低
