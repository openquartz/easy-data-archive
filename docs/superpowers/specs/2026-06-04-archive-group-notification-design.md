# 归档分组完成通知设计文档

> 日期: 2026-06-04
> 模块: easyarchive-starter
> 范围: 归档分组终态通知配置 + 任务完成通知发送

## 1. 目标

为归档分组增加一套可配置的完成通知能力，在分组执行进入终态后自动发送结果通知，满足以下业务要求：

- 通知触发时机为归档分组执行完成后
- 分组可配置是否启用通知
- 每个分组当前只支持一个通知配置
- 通知方式支持飞书或企业微信二选一
- 支持配置对应通知连接地址
- 成功、失败、取消三种终态都发送通知
- 通知模板必须包含任务摘要与执行明细

通知内容至少覆盖以下字段：

- 执行任务 ID
- 此次执行开始时间
- 此次执行结束时间
- 执行状态
- 归档分组编码
- 归档分组名称
- 分组描述
- 归档分组总行数
- 具体负责人
- 具体执行明细

其中执行明细按规则逐条展示：

- 来源表 -> 目标表
- 执行行数
- 执行时间
- 执行结果
- 失败原因或取消原因

本次设计目标是以最小侵入方式接入当前归档执行链路，不重写核心执行器，不影响归档任务主流程稳定性。

## 2. 当前状态

项目已经具备以下基础：

- 核心层存在 `ArchiveEventPublisher`
- 核心层已经发布 `TaskStartEvent`、`TaskEndEvent`、`RuleStartEvent`、`RuleEndEvent`、`TaskProgressEvent`
- 平台层已经存在 `DbArchiveLogListener` 监听归档事件并将任务与规则日志落库
- 平台已经有归档分组、归档任务、归档任务日志、用户等基础管理模型
- 归档分组当前由 `ea_archive_group` 管理，且包含负责人字段 `owner_user_id`

现有缺口：

- 归档分组没有通知开关、渠道、webhook 地址配置
- 任务完成后没有外部通知能力
- 没有统一的通知领域模型和渠道发送抽象
- 现有管理界面无法配置通知参数

因此本次设计应复用现有事件机制和归档日志能力，在平台层旁路增加通知模块。

## 3. 设计原则

### 3.1 终态通知，不干扰主任务

通知属于归档任务的旁路能力。发送成功与否都不能改变任务最终状态，也不能导致归档主流程失败。

### 3.2 优先复用现有事件总线

项目已经存在归档执行事件与监听器模型，本次通知能力应优先接入 `TaskEndEvent`，避免在执行器中直接硬编码飞书或企业微信逻辑。

### 3.3 配置最小化

当前明确约束为“一个分组只支持一个通知配置”，因此首版不引入独立通知配置表，直接扩展分组模型与管理界面。

### 3.4 消息内容统一建模

飞书和企业微信只是不同发送渠道，业务语义应先抽象成统一通知内容模型，再由不同渠道渲染为各自请求体。

### 3.5 失败可观测

配置错误、数据缺失、webhook 调用失败、消息渲染异常都必须记录应用日志，便于定位，但不做首版重试与补偿队列。

## 4. 方案选型

对比三个可选方案：

### 4.1 方案 A：分组内嵌通知配置，事件监听发送

在 `ea_archive_group` 中直接增加通知字段；任务终态时由平台监听器汇总信息并发送。

优点：

- 与当前“一个通知配置”的业务约束完全匹配
- 表结构、Mapper、Service、UI 改动最小
- 通知逻辑和执行器解耦
- 复用现有 `TaskEndEvent`，接入成本低

缺点：

- 后续如果扩展为多个通知目标，需要将存储模型迁移到独立表

### 4.2 方案 B：独立通知配置表，一对一关联分组

新增通知配置表，由分组持有外键或按 `group_id` 关联。

优点：

- 结构更清晰
- 未来支持多目标、多模板、发送记录表时扩展性更好

缺点：

- 对当前需求偏重
- 首版会额外引入表、Mapper、Service 和更多前后端改造

### 4.3 方案 C：任务服务主动发送，不走事件监听

在归档任务结束的 service 或 dispatcher 中直接调用通知发送。

优点：

- 调用链直观

缺点：

- 侵入执行收尾链路
- 执行服务会混入通知编排与渠道细节
- 后续增加更多外部回调能力时会持续膨胀

### 4.4 最终选择

采用方案 A，并保留方案 B 的代码扩展边界：

- 存储层先直接扩展 `ea_archive_group`
- 服务层仍拆分为监听器、通知服务、消息构建器、渠道客户端
- 未来若改为多通知目标，仅迁移配置存储，不重写发送主链路

## 5. 数据模型设计

### 5.1 表结构扩展

在 `ea_archive_group` 表上新增以下字段：

| 字段 | 类型建议 | 说明 |
| --- | --- | --- |
| `notify_enabled` | `TINYINT` | 是否启用通知，`0-关闭 1-开启` |
| `notify_channel` | `VARCHAR(16)` | 通知渠道，`FEISHU` / `WECOM` |
| `notify_webhook_url` | `VARCHAR(500)` | 机器人 webhook 地址 |

字段约束建议：

- `notify_enabled` 默认值为 `0`
- 当 `notify_enabled = 1` 时，`notify_channel` 与 `notify_webhook_url` 必须有值
- `notify_webhook_url` 不做厂商强耦合格式校验，只做长度与非空控制

### 5.2 领域模型扩展

`ArchiveGroup` 增加以下字段：

- `Integer notifyEnabled`
- `String notifyChannel`
- `String notifyWebhookUrl`

`ArchiveGroupMapper.xml` 需要同步更新：

- `BaseResultMap`
- `Base_Column_List`
- `insert`
- `update`
- 列表与详情查询 SQL

### 5.3 渠道枚举

平台层新增 `NotificationChannelEnum`：

- `FEISHU`
- `WECOM`

职责：

- 统一约束后端存储值
- 提供前端下拉字典值
- 作为渠道客户端选择依据

## 6. 模块结构设计

推荐在 `easyarchive-starter` 新增通知模块，按如下职责拆分：

- `ArchiveNotificationListener`
- `ArchiveNotificationService`
- `NotificationMessageBuilder`
- `NotificationClient`
- `FeishuNotificationClient`
- `WeComNotificationClient`

### 6.1 ArchiveNotificationListener

职责：

- 监听 `TaskEndEvent`
- 识别任务已进入 `SUCCESS`、`FAILED`、`CANCELLED` 三种终态
- 调用 `ArchiveNotificationService`
- 捕获并记录通知链路异常，避免影响事件总线

约束：

- 不直接查询数据库
- 不直接拼消息模板
- 不直接发 HTTP 请求

### 6.2 ArchiveNotificationService

职责：

- 根据 `groupId` 读取分组通知配置
- 判断是否启用通知
- 查询任务摘要、分组信息、负责人、规则执行明细
- 选择通知渠道客户端
- 驱动消息构建和发送

该服务是通知领域主入口，负责“是否发、发什么、走哪个渠道”。

### 6.3 NotificationMessageBuilder

职责：

- 先组装统一通知内容模型
- 再将统一模型渲染为渠道消息文本

统一内容模型建议包含：

- 任务摘要
- 分组信息
- 负责人信息
- 规则明细列表
- 失败或取消原因

这样可以避免业务字段在不同客户端中重复拼装。

### 6.4 NotificationClient

定义统一发送接口，例如：

- 入参：webhook 地址、渲染后的消息内容
- 出参：发送结果对象或直接抛出异常

渠道实现：

- `FeishuNotificationClient`
- `WeComNotificationClient`

职责边界：

- 仅处理请求体组装和 HTTP 调用
- 不负责业务数据查询
- 不负责分组配置判断

## 7. 通知内容设计

### 7.1 统一摘要字段

通知摘要固定包含：

- 执行任务 ID
- 执行开始时间
- 执行结束时间
- 执行状态
- 归档分组编码
- 归档分组名称
- 分组描述
- 归档分组总行数
- 负责人姓名

其中：

- 失败通知额外展示失败原因
- 取消通知额外展示取消原因

### 7.2 执行明细字段

明细按规则逐条展示：

- `来源表 -> 目标表`
- 执行行数
- 执行耗时
- 执行结果
- 失败原因

若单条规则无失败原因，则不显示该字段。

### 7.3 首版消息样式

首版统一使用文本或 markdown 可读样式，不做复杂卡片。

原因：

- 首要目标是信息准确送达
- 文本/markdown 对飞书与企业微信都更容易兼容
- 后续升级卡片时可在不改变业务内容模型的前提下单独演进

## 8. 数据来源设计

通知所需数据不新增通知专用持久化表，直接复用现有平台数据：

- 任务头信息：`ea_archive_task`
- 分组信息：`ea_archive_group`
- 负责人信息：`sys_user`
- 规则执行结果：优先聚合现有归档任务日志与任务明细

建议取数策略如下：

### 8.1 任务摘要

从 `ArchiveGroupExecuteTask` 对应的任务记录中获取：

- `id`
- `groupId`
- `startTime`
- `endTime`
- `processedRecords`
- `executeStatus`
- `errorMsg`
- `cancelReason`

### 8.2 分组信息

从 `ArchiveGroup` 获取：

- `groupCode`
- `groupName`
- `remark`
- `ownerUserId`
- 通知配置字段

### 8.3 负责人

通过 `ownerUserId` 查询 `sys_user`，展示负责人姓名。若未配置负责人或用户不存在，消息中展示“未知”。

### 8.4 规则明细

规则明细优先从任务执行结果聚合，目标是最终能展示：

- 来源表
- 目标表
- 执行行数
- 执行耗时
- 执行结果
- 失败原因

由于当前 `RuleEndEvent` 已包含来源表、目标表、处理行数、耗时、错误信息，首版建议优先基于已有规则结束日志聚合展示；若某些字段无法直接由任务明细表提供，则补查归档规则配置表完善表名信息。

## 9. 执行流程设计

推荐流程如下：

1. `ArchiveGroupExecutor` 发布 `TaskEndEvent`
2. `ArchiveNotificationListener` 收到事件
3. `ArchiveNotificationService` 读取分组通知配置
4. 若 `notify_enabled != 1`，直接结束
5. 若渠道为空或 webhook 为空，记录错误日志并结束
6. 读取任务摘要、分组信息、负责人、规则明细
7. `NotificationMessageBuilder` 构建统一通知内容并渲染渠道文本
8. 根据 `notify_channel` 选择 `FeishuNotificationClient` 或 `WeComNotificationClient`
9. 发起 HTTP POST
10. 记录发送结果日志

流程要求：

- 通知失败不影响任务终态
- 通知异常不向上抛回执行器
- 同一终态事件只触发一次通知编排

## 10. 渠道接入设计

### 10.1 飞书

依据飞书 webhook 文档，首版采用简单 webhook 文本消息发送方式。

接入要求：

- 使用分组配置中的 webhook 地址
- 通过 HTTP POST 提交 JSON 请求体
- 消息内容由 `NotificationMessageBuilder` 渲染为文本

不纳入首版范围：

- 交互式卡片
- @ 指定人
- 签名校验扩展参数

### 10.2 企业微信

依据企业微信机器人 webhook 能力，首版采用文本或 markdown 消息发送方式。

接入要求：

- 使用分组配置中的 webhook 地址
- 通过 HTTP POST 提交 JSON 请求体
- 使用与飞书一致的统一消息语义模型

不纳入首版范围：

- 图片、图文或复杂富媒体消息
- 重试队列

## 11. 错误处理设计

通知链路定义以下失败处理策略：

### 11.1 配置错误

场景：

- 渠道为空
- webhook 为空
- 渠道值不支持

处理：

- 记录应用日志
- 跳过发送
- 不改动任务状态

### 11.2 数据缺失

场景：

- 负责人不存在
- 部分规则明细缺失
- 分组备注为空

处理：

- 使用“未知”或“无”做降级展示
- 尽量发送可用通知

### 11.3 webhook 调用失败

场景：

- 超时
- 4xx
- 5xx
- 网络异常

处理：

- 记录错误日志
- 本次不重试
- 不影响主任务

### 11.4 模板渲染异常

处理：

- 捕获异常
- 输出错误日志
- 不中断其他监听器执行

## 12. 管理端改造设计

### 12.1 后端接口

复用现有归档分组新增、编辑、详情、列表接口，增加以下字段：

- `notifyEnabled`
- `notifyChannel`
- `notifyWebhookUrl`

要求：

- 新增和编辑时进行基本参数校验
- 列表和详情接口返回通知配置，便于前端展示与回填

首版不新增以下接口：

- 测试发送通知
- 手工重发通知
- 通知发送记录查询

### 12.2 前端页面

在归档分组新增/编辑弹窗中增加“通知配置”区域，包含：

- 是否通知
- 通知方式
- 通知地址

交互规则：

- 关闭通知时，渠道与 webhook 可为空
- 开启通知时，渠道与 webhook 必填
- webhook 做长度与非空校验

归档分组详情页可展示通知配置摘要，但不是首版强制项；若当前详情页已有基础信息区域，可一并补充显示。

## 13. 测试策略

### 13.1 单元测试

覆盖以下核心组件：

- `NotificationMessageBuilder`
- `ArchiveNotificationService`
- `FeishuNotificationClient`
- `WeComNotificationClient`

重点场景：

- 成功通知渲染
- 失败通知渲染
- 取消通知渲染
- 通知关闭时不发送
- 配置不完整时跳过发送
- 规则明细部分缺失时降级发送

### 13.2 集成测试

验证以下链路：

- `TaskEndEvent` 到通知监听器的完整触发
- 成功任务发送通知
- 失败任务发送通知
- 取消任务发送通知
- webhook 异常不影响任务状态

### 13.3 回归测试

确保以下能力无回归：

- 归档分组新增、编辑、查询
- 归档执行主链路
- 现有归档日志监听器

## 14. 非目标

以下内容明确不纳入本次范围：

- 一个分组配置多个通知目标
- 同时发送飞书和企业微信
- 通知模板可视化编辑
- 通知发送记录持久化表
- 失败重试、补偿队列、死信处理
- 卡片消息、富媒体消息
- 手工测试通知接口

## 15. 最终方案

本次通知模块采用“分组内嵌配置 + 任务终态监听 + 渠道客户端发送”的方案，具体如下：

- 在 `ea_archive_group` 增加通知开关、通知渠道、webhook 地址
- 在 `ArchiveGroup` 与归档分组管理接口中同步新增通知字段
- 新增 `ArchiveNotificationListener` 监听 `TaskEndEvent`
- 新增 `ArchiveNotificationService` 作为通知主入口
- 新增 `NotificationMessageBuilder` 统一组装消息内容
- 新增 `NotificationClient` 及飞书、企业微信实现
- 对成功、失败、取消三种终态都发送通知
- 通知失败只记日志，不影响任务终态

该方案与当前项目事件架构和平台管理模型保持一致，能以较小改动满足当前业务目标，并为后续扩展多通知目标保留清晰边界。
