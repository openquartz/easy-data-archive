# 归档分组站内通知设计文档

> 日期: 2026-06-05
> 模块: easyarchive-starter + easyarchive-ui
> 范围: 归档分组终态站内通知 + 顶栏铃铛提醒

## 1. 目标

在现有飞书、企业微信之外，为 EasyArchive 增加一套平台内可见的站内通知能力，用于在归档分组执行进入终态后，将通知发送到该分组配置的系统用户收件箱中，并通过界面右上角铃铛入口展示。

首版业务范围固定为：

- 只通知归档分组执行终态：`SUCCESS`、`FAILED`、`CANCELED`
- 每个归档分组显式维护一组站内通知成员
- 仅这些成员在自己的铃铛通知中看到该分组消息
- 用户可在铃铛中查看、标记已读、跳转到分组详情或任务详情

本次设计不包含：

- 运行中进度提醒
- 分组配置变更提醒
- 成员变更提醒
- 静音、免打扰、仅失败通知等偏好管理
- 面向管理员的代查他人通知能力

## 2. 当前状态

项目当前已经具备以下基础能力：

- 核心层存在归档事件发布机制，终态通知可以复用当前 `TaskEndEvent` 或等价任务终态事件
- 平台层已有外部完成通知设计与实现骨架，说明“任务终态旁路通知”是符合当前架构方向的
- 平台层已有归档分组、归档任务、归档任务日志、用户管理、权限控制等基础模型
- 管理端前端已有统一顶层布局 [`AppLayout.vue`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui/src/layouts/AppLayout.vue:1)，适合承载全局铃铛入口
- 前端已有任务详情、分组详情、工作标签页机制，可复用跳转与多标签激活能力

当前缺口：

- 没有独立的站内通知领域模型
- 没有用户维度的通知收件箱与已读状态存储
- 分组层没有站内通知开关与通知成员配置
- 顶栏没有全局通知入口

## 3. 设计原则

### 3.1 终态触发，旁路执行

站内通知与飞书、企业微信一样，属于归档任务主流程之外的旁路能力。通知生成或落库失败不能改变任务最终状态，也不能反向导致归档主流程失败。

### 3.2 事件驱动，复用现有触发点

站内通知必须复用当前任务终态事件，而不是在执行器、调度器或控制器中硬编码发送逻辑。这样可以保持与现有外部通知一致的触发语义。

### 3.3 通知内容按事件生成一次，用户状态分别维护

同一条业务通知应只生成一份内容快照，但每个接收人必须维护自己的已读状态。这是后续支持未读数、批量已读、消息中心的基础。

### 3.4 分组通知成员独立于权限模型

站内通知成员不是分组访问权限、不是负责人字段，也不是组织关系的推导结果。首版必须由归档分组显式维护通知成员列表，避免权限与通知订阅耦合。

### 3.5 首版克制，不提前抽象过度

首版只做一个清晰可用的铃铛通知系统，不引入 MQ、补偿队列、复杂渠道插件、规则引擎、静音策略等二期能力。

## 4. 方案选型

对比三种可行方案：

### 4.1 方案 A：通知主表 + 收件箱表 + 分组成员表

设计一条通知主记录，再为每个通知成员生成独立收件箱记录。

优点：

- 最符合“单条通知，多人接收”的业务结构
- 已读状态天然按用户维护
- 后续支持未读数、通知中心、批量已读都很顺
- 与外部通知共享触发点，但存储模型保持独立

缺点：

- 首版新增表数量较多
- 实现复杂度高于单表直接落库

### 4.2 方案 B：按用户直接落一张通知明细表

每个用户收到一条通知就插入一条完整消息。

优点：

- 查询简单
- 首版写起来最快

缺点：

- 同一消息正文会复制 N 份
- 去重、审计、模板升级、后续扩展都不优雅

### 4.3 方案 C：基于任务日志实时拼接伪通知

不落独立通知表，铃铛读取任务日志和任务状态后实时拼出“通知”。

优点：

- 改动最小

缺点：

- 不能稳定支持已读状态
- 不能保证通知快照一致性
- 本质上是日志筛选器，不是真正通知系统

### 4.4 最终选择

采用方案 A。

最终结构：

- `ea_archive_group` 扩展站内通知开关
- `ea_archive_group_notification_user` 保存分组通知成员
- `ea_in_app_notification` 保存业务通知主记录
- `ea_in_app_notification_recipient` 保存用户收件箱与已读状态

## 5. 数据模型设计

### 5.1 分组表扩展

在 `ea_archive_group` 上新增字段：

| 字段 | 类型建议 | 说明 |
| --- | --- | --- |
| `in_app_notify_enabled` | `TINYINT` | 是否启用站内通知，`0-关闭 1-开启` |

约束建议：

- 默认值为 `0`
- 仅表示是否启用站内通知，不与飞书、企业微信渠道字段复用

### 5.2 分组通知成员表

新增表：`ea_archive_group_notification_user`

| 字段 | 类型建议 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键 |
| `group_id` | `BIGINT` | 归档分组 ID |
| `user_id` | `BIGINT` | 用户 ID |
| `created_by` | `VARCHAR(64)` | 创建人 |
| `created_time` | `DATETIME` | 创建时间 |
| `updated_by` | `VARCHAR(64)` | 更新人 |
| `updated_time` | `DATETIME` | 更新时间 |

索引建议：

- 唯一索引：`uk_group_user(group_id, user_id)`
- 普通索引：`idx_user_id(user_id)`

设计说明：

- 这张表描述“谁会收到这个分组的站内通知”
- 不承载已读状态，不承载通知内容
- 后续如果增加静音、仅失败通知等用户偏好，可以在这张表上继续扩展

### 5.3 通知主表

新增表：`ea_in_app_notification`

| 字段 | 类型建议 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键 |
| `biz_type` | `VARCHAR(32)` | 业务类型，首版固定 `ARCHIVE_GROUP_TASK` |
| `biz_id` | `BIGINT` | 业务主键，首版建议存任务 ID |
| `group_id` | `BIGINT` | 归档分组 ID |
| `group_name` | `VARCHAR(128)` | 分组名称快照 |
| `task_id` | `BIGINT` | 任务 ID 快照 |
| `task_status` | `VARCHAR(16)` | `SUCCESS` / `FAILED` / `CANCELED` |
| `title` | `VARCHAR(200)` | 通知标题 |
| `content_summary` | `VARCHAR(500)` | 列表摘要 |
| `payload_json` | `TEXT` | 完整通知快照 |
| `source_time` | `DATETIME` | 业务事件发生时间 |
| `created_time` | `DATETIME` | 创建时间 |

索引建议：

- 唯一索引：`uk_biz_type_biz_id_status(biz_type, biz_id, task_status)`
- 普通索引：`idx_group_created(group_id, created_time desc)`
- 普通索引：`idx_task_id(task_id)`

`payload_json` 建议至少包含：

- 分组 ID、分组名称、分组编码
- 任务 ID、终态状态
- 开始时间、结束时间、执行耗时
- 总表数、成功表数、失败表数
- 处理总行数
- 失败原因摘要或取消原因
- 跳转参数：`groupId`、`taskId`

### 5.4 用户收件箱表

新增表：`ea_in_app_notification_recipient`

| 字段 | 类型建议 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` | 主键 |
| `notification_id` | `BIGINT` | 关联通知主表 |
| `recipient_user_id` | `BIGINT` | 接收人用户 ID |
| `read_status` | `TINYINT` | `0-未读 1-已读` |
| `read_time` | `DATETIME` | 已读时间 |
| `delivery_status` | `TINYINT` | 首版可固定 `1-已投递` |
| `created_time` | `DATETIME` | 创建时间 |

索引建议：

- 唯一索引：`uk_notification_user(notification_id, recipient_user_id)`
- 核心索引：`idx_user_read_created(recipient_user_id, read_status, created_time desc)`

设计说明：

- 铃铛未读数、通知列表、已读操作都围绕这张表查询
- 已读状态只影响当前用户，不影响其他接收人

## 6. 后端架构设计

### 6.1 触发链路

推荐链路：

1. 归档任务进入终态
2. 发布 `TaskEndEvent` 或当前等价终态事件
3. 外部通知链路继续处理飞书、企业微信
4. 新增站内通知监听器处理平台内收件箱写入

站内通知只监听：

- `SUCCESS`
- `FAILED`
- `CANCELED`

不监听运行中事件，不监听配置变更事件。

### 6.2 职责拆分

建议新增以下职责边界：

- `ArchiveInAppNotificationListener`
  - 监听任务终态事件
  - 调用站内通知应用服务
  - 捕获异常并记录日志

- `ArchiveInAppNotificationService`
  - 校验分组是否启用站内通知
  - 查询分组通知成员
  - 构建统一通知消息
  - 落库通知主记录
  - 批量落库收件箱记录

- `InAppNotificationMessageBuilder`
  - 将任务终态数据渲染为标题、摘要和快照
  - 不负责数据库访问

- `InAppNotificationMapper`
  - 通知主表持久化

- `InAppNotificationRecipientMapper`
  - 收件箱表持久化

- `ArchiveGroupNotificationUserMapper`
  - 分组通知成员配置持久化

- `InAppNotificationQueryService`
  - 提供未读数、列表、已读等查询能力

### 6.3 统一协调方式

当前项目已经存在外部通知模块。为了避免两个监听器在任务终态后各自散开处理，建议新增一个轻量协调层：

- `ArchiveCompletionNotificationCoordinator`

职责：

- 接收任务终态事件后的通知编排请求
- 调用外部通知服务
- 调用站内通知服务

如果当前代码风格更偏向独立 listener，也可以保留两个 listener 并行存在。首版两种都可落地，推荐以协调层统一语义，但不强制把外部通知与站内通知抽象成复杂插件体系。

## 7. 站内通知生成规则

### 7.1 触发条件

当且仅当以下条件同时满足时生成站内通知：

- 任务终态是 `SUCCESS`、`FAILED` 或 `CANCELED`
- 该任务关联的分组存在
- 分组 `in_app_notify_enabled = 1`
- 分组配置的通知成员列表非空

如果分组启用了站内通知但成员为空，视为非法配置。保存分组配置时应阻止该情况出现；如果历史脏数据仍然出现，运行时应记录告警日志并跳过投递。

### 7.2 幂等规则

幂等约束：

- 同一个 `task_id + terminal_status` 只允许生成一条主通知

依赖：

- `ea_in_app_notification.uk_biz_type_biz_id_status`
- `ea_in_app_notification_recipient.uk_notification_user`

如果事件被重复消费：

- 插入主通知冲突时视为已生成
- 插入收件箱冲突时视为已投递
- 记录 `info` 日志，不抛业务异常

### 7.3 用户有效性规则

投递前需要验证接收人用户是否有效：

- 用户存在
- 用户处于可用状态

对无效用户的处理：

- 历史通知不清理
- 新通知生成时跳过无效用户
- 日志记录被跳过的用户 ID

## 8. 通知文案与快照设计

### 8.1 标题模板

- 成功：`归档分组 {groupName} 执行成功`
- 失败：`归档分组 {groupName} 执行失败`
- 取消：`归档分组 {groupName} 已取消`

### 8.2 摘要模板

- 成功：`任务 #{taskId} 已完成，处理 {tableCount} 张表，归档 {rowCount} 行`
- 失败：`任务 #{taskId} 执行失败，失败位置：{failedTableName 或 reason}`
- 取消：`任务 #{taskId} 已取消，执行进度 {completedTableCount}/{tableCount}`

### 8.3 完整快照内容

完整快照建议包含：

- 分组基础信息
- 任务基础信息
- 任务起止时间
- 执行耗时
- 表统计信息
- 行数统计信息
- 失败原因或取消原因
- 跳转参数

首版铃铛列表只展示标题和摘要，不直接展示完整快照。完整快照主要用于未来通知详情页或排障审计。

## 9. 接口设计

### 9.1 铃铛未读数

`GET /api/in-app-notifications/unread-count`

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "unreadCount": 3
  }
}
```

### 9.2 铃铛最近通知列表

`GET /api/in-app-notifications`

请求参数：

- `limit`，默认 `20`
- `readStatus`，可选

返回字段建议：

- `notificationId`
- `title`
- `summary`
- `taskStatus`
- `groupId`
- `groupName`
- `taskId`
- `readStatus`
- `createdTime`

### 9.3 单条标记已读

`POST /api/in-app-notifications/{id}/read`

规则：

- 只能标记当前用户自己的收件箱记录
- 幂等，多次调用结果一致

### 9.4 全部标记已读

`POST /api/in-app-notifications/read-all`

规则：

- 仅影响当前登录用户
- 不区分分组

### 9.5 分组站内通知配置回显

`GET /api/archive-groups/{id}/notification-members`

返回建议：

- `inAppNotifyEnabled`
- `recipientUserIds`
- `recipientUsers`

### 9.6 分组站内通知配置保存

`PUT /api/archive-groups/{id}/notification-members`

请求体建议：

- `inAppNotifyEnabled`
- `recipientUserIds`

也可以将此配置合并进现有分组新增、编辑接口，但无论接口形式如何，后端保存逻辑都应由 `ArchiveGroupService` 统一编排。

## 10. 权限与校验规则

### 10.1 铃铛查询权限

- 只能查询当前登录用户自己的站内通知
- 首版不提供管理员查看他人收件箱能力

### 10.2 已读操作权限

- 只能修改当前用户自己的收件箱记录

### 10.3 分组配置编辑权限

- 站内通知配置的编辑权限与“编辑归档分组”保持一致

### 10.4 保存校验

服务端保存时必须保证：

- `inAppNotifyEnabled = 1` 时，`recipientUserIds` 至少有一个
- 所有 `recipientUserIds` 都是合法有效用户
- 重复用户 ID 去重处理

服务层业务异常必须使用项目已有错误码与自定义异常，不在业务逻辑中引入新的 `IllegalArgumentException` 或 `IllegalStateException`。

## 11. 前端交互设计

### 11.1 顶栏铃铛位置

基于 [`AppLayout.vue`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui/src/layouts/AppLayout.vue:1) 顶栏结构，铃铛建议放在：

- 账号信息右侧
- 退出按钮左侧

推荐顺序：

- 账号
- 铃铛
- 退出
- 语言切换

### 11.2 铃铛状态

铃铛组件至少包含 4 种状态：

- 无未读：正常线性图标
- 有未读：显示红点或数字角标
- 加载中：轻量骨架或淡入
- 失败：面板局部展示加载失败提示，不触发全局错误弹窗

未读数展示建议：

- `1-9` 显示数字
- `10+` 显示 `9+`

### 11.3 下拉面板

建议规格：

- 宽度 `360px`
- 最大高度 `480px`
- 超出内容滚动

结构：

1. 面板头部
   - 标题：`通知`
   - 操作：`全部标记已读`
   - 可选：`查看全部`

2. 列表区
   - 未读状态点
   - 标题
   - 任务编号
   - 完成时间
   - 摘要
   - `查看分组`
   - `查看任务`

3. 底部
   - 若后续增加通知中心，可放 `查看全部通知`

### 11.4 卡片交互

交互规则：

- 点击卡片主体：默认跳转任务详情
- 如果任务详情不可用：回退跳转分组详情
- 点击 `查看分组`：跳分组详情
- 点击 `查看任务`：跳任务详情

已读策略：

- 用户点击卡片主体或跳转按钮时，自动将该条通知标记为已读
- 用户仅展开铃铛面板，不自动标记全部已读

### 11.5 空态与排序

空态文案：

- 没有历史通知：`暂时没有通知`
- 辅助说明：`归档分组执行完成后会显示在这里`

排序规则：

- 按 `createdTime desc` 倒序展示

列表容量：

- 铃铛下拉默认只展示最近 `20` 条

### 11.6 窄屏适配

小屏下宽度建议：

- `min(360px, calc(100vw - 24px))`

仍保持右对齐并展示在顶栏下方，避免溢出视口。

### 11.7 与工作标签页配合

通知跳转应复用现有标签页机制：

- 分组详情标签已存在则激活
- 任务详情标签已存在则激活
- 不存在则新开标签

## 12. 分组配置界面设计

在 [`ArchiveGroupFormDialog.vue`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui/src/components/ArchiveGroupFormDialog.vue:1) 中新增独立区块：

- 标题：`站内通知`
- 开关：`启用站内通知`
- 成员选择器：`通知成员`

设计要求：

- 与现有飞书、企业微信配置区块分离
- 不把“界面通知”塞进 `notifyChannel` 下拉
- 成员选择器支持多选
- 展示值建议为 `真实姓名 + 用户名`
- 保存时提交 `recipientUserIds`

如果当前 UI 缺少成熟多选组件，首版允许使用简单可维护的多选弹层或复选列表，不为此引入额外大型组件库。

## 13. 测试策略

### 13.1 后端单元测试

覆盖：

- `InAppNotificationMessageBuilder`
- `ArchiveInAppNotificationService`
- 站内通知配置保存校验

### 13.2 监听器测试

覆盖：

- 成功终态生成通知
- 失败终态生成通知
- 取消终态生成通知
- 分组未启用时不生成
- 成员为空时跳过并记录日志
- 重复终态事件不会重复投递

### 13.3 控制器契约测试

覆盖：

- 未读数接口
- 列表接口
- 单条已读接口
- 全部已读接口
- 分组通知成员配置回显与保存

### 13.4 前端测试

覆盖：

- 顶栏铃铛未读角标展示
- 下拉列表渲染
- 空态与失败态
- 单条已读与全部已读行为
- 通知跳转行为
- 分组通知成员配置表单回显与提交

## 14. 实施顺序

推荐落地顺序：

1. 数据库迁移与实体层扩展
2. 分组通知成员持久化
3. 站内通知生成链路
4. 铃铛查询与已读接口
5. 分组配置 UI
6. 顶栏铃铛与下拉面板

## 15. 非目标与后续扩展

首版明确不做：

- 通知中心独立页面
- 通知详情页
- 静音、免打扰、仅失败通知
- 运行中异常提醒
- WebSocket 实时推送
- 通知补偿与重试队列

后续可平滑扩展的方向：

- 通知中心分页列表
- 仅失败通知或成员级偏好
- 分组级静音
- 实时推送替代轮询
- 更多事件类型接入

## 16. 最终结论

本方案以任务终态事件为统一触发点，新增分组通知成员、通知主记录和用户收件箱三层模型，在不干扰归档主流程的前提下，为平台提供一套真实可用的站内铃铛通知系统。

这套设计与现有飞书、企业微信通知形成并列关系，但不复用其渠道模型；与现有权限体系保持协作，但不耦合；与前端当前顶层布局、任务详情和工作标签页机制保持一致，具备明确的一期交付边界和二期扩展路径。
