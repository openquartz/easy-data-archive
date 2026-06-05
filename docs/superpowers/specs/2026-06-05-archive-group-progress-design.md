# 归档分组运行态进度展示设计

## 背景

当前 EasyArchive 已经具备任务运行进度采集能力：

1. `ArchiveExecutor` 在执行过程中持续上报 `TaskProgressEvent`。
2. `DbArchiveLogListener` 会把任务的 `processedRecords`、`processedSpeed`、`heartbeatTime` 写回 `ArchiveGroupExecuteTask`。
3. 任务详情页已经可以直接展示任务维度的迁移记录数与处理速度。

但归档分组页面仍然只能看到“是否存在活动任务”，缺少两个关键运行态信号：

1. 当前分组是否正在持续推进。
2. 当前活动任务已经迁移成功多少条记录。

本次需求是在归档分组场景中补上运行态展示，让用户在分组列表和分组详情中快速感知任务推进情况，而不是进入任务详情后才看到进度。

## 目标

1. 在归档分组列表和归档分组详情页展示当前活动任务的“已经迁移成功总数”。
2. 在归档分组页面增加一个运行态“模拟进度条”，表达任务正在推进，但不宣称真实完成百分比。
3. 复用现有任务进度采集链路，不修改数据库结构，不改执行主链。
4. 统一前端计算规则，保证列表页和详情页的模拟进度表现一致。

## 非目标

1. 不统计归档分组的历史累计迁移总数。
2. 不尝试计算真实完成百分比。
3. 不新增数据库字段保存模拟百分比。
4. 不改造任务详情页的数据来源与交互结构。
5. 不增加新的定时任务、轮询表或进度缓存层。

## 用户决策记录

本设计基于以下已确认决策：

1. “已经迁移成功总数”仅表示当前活动任务的累计成功迁移行数，取值口径为 `processedRecords`。
2. “进度条”采用类似 `NProgress` 的伪进度风格，只表达“正在推进”，不表达真实剩余量。
3. 后端只返回原始运行态字段，不返回伪造的展示百分比。
4. 无活动任务时，不回填历史累计值。

## 现状与约束

### 已有后端能力

1. [`ArchiveGroupView`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupView.java:1) 已包含活动任务基础快照字段：
   - `activeTaskId`
   - `activeTaskStatus`
   - `activeTaskStartTime`
2. [`ArchiveGroupServiceImpl`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java:1) 在分组列表和详情聚合中已查询活动任务。
3. [`ArchiveGroupExecuteTask`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveGroupExecuteTask.java:1) 已具备：
   - `processedRecords`
   - `processedSpeed`
   - `heartbeatTime`
4. [`DbArchiveLogListener`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/listener/DbArchiveLogListener.java:1) 已在任务运行过程中刷新这些字段。

### 已有前端基础

1. [`ArchiveGroupView.vue`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui/src/views/ArchiveGroupView.vue:1) 已展示分组列表与活动任务操作。
2. [`ArchiveGroupDetailView.vue`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui/src/views/ArchiveGroupDetailView.vue:1) 已展示分组详情与执行概览。
3. 任务相关 API 类型已经普遍支持 `processedRecords` 与 `processedSpeed` 展示模式。

### 当前缺口

1. 分组视图 DTO 没有暴露活动任务的运行态计数与速度快照。
2. 分组页面没有统一的“模拟进度”计算逻辑。
3. 分组页面无法直接展示“当前活动任务已迁移成功总数”。

## 方案比较

### 方案 A：后端返回展示态百分比

后端直接在分组接口中增加 `activeTaskProgressPercent` 这类字段。

优点：

1. 前端渲染最简单。
2. 列表与详情都能直接使用同一接口字段。

缺点：

1. 把纯展示规则固化在后端，不利于后续调整动画与视觉节奏。
2. 会把“不真实的百分比”伪装成接口事实数据。
3. 后端测试会被迫绑定前端体验细节。

### 方案 B：后端返回活动任务原始快照，前端计算模拟进度

后端只扩展 `ArchiveGroupView`，返回活动任务的 `processedRecords`、`processedSpeed`、`heartbeatTime`；前端统一计算模拟进度百分比。

优点：

1. 后端职责清晰，只暴露事实数据。
2. 前端可以灵活调整曲线、封顶值和终态展示。
3. 列表页和详情页可以通过同一个纯函数保持一致。
4. 不需要数据库迁移。

缺点：

1. 前端需要额外维护一层展示计算。

### 方案 C：持久化模拟进度

将模拟百分比作为任务运行字段写入数据库。

优点：

1. 前后端都容易读取。

缺点：

1. 数据语义错误，数据库中会长期保存不真实的百分比。
2. 增加持久化复杂度且没有业务收益。

## 结论

采用方案 B：后端返回活动任务原始运行态快照，前端统一计算模拟进度。

## 详细设计

### 1. 后端 DTO 扩展

在 [`ArchiveGroupView`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupView.java:1) 中新增以下字段：

1. `activeTaskProcessedRecords`
2. `activeTaskProcessedSpeed`
3. `activeTaskHeartbeatTime`

字段含义：

1. `activeTaskProcessedRecords`：当前活动任务累计已成功迁移的记录数，对应 `ArchiveGroupExecuteTask.processedRecords`。
2. `activeTaskProcessedSpeed`：当前活动任务当前统计速度，对应 `ArchiveGroupExecuteTask.processedSpeed`。
3. `activeTaskHeartbeatTime`：最近一次进度刷新心跳时间，对应 `ArchiveGroupExecuteTask.heartbeatTime`。

本次不增加 `activeTaskProgressPercent` 字段。

### 2. 后端组装逻辑

在 [`ArchiveGroupServiceImpl`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java:1) 的 `toView(group, activeTask)` 中：

1. 保留现有 `activeTaskId`、`activeTaskStatus`、`activeTaskStartTime` 赋值逻辑。
2. 当存在活动任务时，同时写入新增的三个活动任务快照字段。
3. 当不存在活动任务时，这些字段保持 `null`。

列表接口 `findAll()` 与概览接口 `findOverview()` 复用同一 DTO 组装逻辑，不新增独立查询。

### 3. 前端数据模型扩展

在 [`easyarchive-ui/src/api/archiveGroup.ts`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui/src/api/archiveGroup.ts:1) 的分组类型定义中新增与后端一致的三个字段：

1. `activeTaskProcessedRecords?: number`
2. `activeTaskProcessedSpeed?: number`
3. `activeTaskHeartbeatTime?: string`

不在 API 类型中定义服务端返回的进度百分比字段。

### 4. 模拟进度计算规则

前端新增统一纯函数：

`resolveArchiveGroupRuntimeProgress(group): number`

输入基于分组当前活动任务状态和 `activeTaskProcessedRecords`，输出 0 到 100 的展示百分比。

规则如下：

1. 无活动任务时返回 `0`。
2. `processedRecords` 为空或小于等于 `0` 时返回 `0`。
3. `STATUS_RUNNING` 且有处理量时，进度进入可见区间并采用非线性增长。
4. `STATUS_RUNNING` 下进度最大不超过 `95`。
5. `STATUS_CANCELLING` 固定展示 `95`。
6. `STATUS_SUCCESS` 展示 `100`。
7. `STATUS_FAILED` 与 `STATUS_CANCELLED` 不补满到 `100`，仅基于当时可推导值展示。

推荐的运行态曲线：

1. 当 `processedRecords > 0` 时，最小展示值为 `12`。
2. 运行中百分比采用对数型增长规则，例如 `12 + min(83, floor(ln(processedRecords + 1) * 8))`。
3. 该公式属于前端展示实现，不进入后端契约。

设计约束只有两个：

1. 运行中进度必须单调不减。
2. 运行中进度不得显示为 `100`。

### 5. 页面展示落点

#### 分组列表页

文件：[`ArchiveGroupView.vue`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui/src/views/ArchiveGroupView.vue:1)

展示建议：

1. 在“当前任务”相关单元格内补充一个紧凑进度条。
2. 在进度条下方或同单元格内展示“已迁移成功总数”。
3. 若无活动任务，不展示进度条，已迁移成功总数显示 `0`。

该页以“快速识别运行态”为主，信息密度保持轻量。

#### 分组详情页

文件：[`ArchiveGroupDetailView.vue`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui/src/views/ArchiveGroupDetailView.vue:1)

展示建议：

1. 在当前活动任务卡片中增加进度条。
2. 展示“已迁移成功总数”。
3. 可同时展示当前速度和最近心跳时间。

该页可以比列表页展示更多运行态上下文，但不替代任务详情页。

### 6. 文案与语义

新增展示文案应明确这是“运行态进度”，避免用户把它理解为真实百分比。

推荐文案：

1. `运行进度`
2. `已迁移成功总数`

不推荐直接使用：

1. `完成率`
2. `任务完成百分比`

### 7. 错误处理与降级

1. 若活动任务存在但快照字段为空，前端按 `0` 或 `-` 降级显示。
2. 若后端返回的活动任务状态与快照存在短暂不一致，前端以状态优先，避免渲染异常。
3. 若进度计算函数收到未知状态，统一回落为 `0`，避免渲染出具有误导性的高进度。

## 测试策略

### 后端测试

补充 [`ArchiveGroupServiceImplTest`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java:1)：

1. 验证存在活动任务时，`ArchiveGroupView` 会带出新增的活动任务快照字段。
2. 验证不存在活动任务时，这些字段为空。

补充或更新 [`ArchiveGroupControllerContractTest`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java:1)：

1. 验证分组列表接口返回新增字段。
2. 验证分组概览接口中的 `group` 对象返回新增字段。

### 前端测试

补充或新增 TypeScript 单元测试：

1. 无活动任务时返回 `0`。
2. 运行中且 `processedRecords > 0` 时返回大于 `0` 且小于 `96`。
3. `processedRecords` 增长时进度单调不减。
4. `STATUS_CANCELLING` 返回 `95`。
5. `STATUS_SUCCESS` 返回 `100`。
6. `STATUS_FAILED` 与 `STATUS_CANCELLED` 不返回 `100`。

必要时扩展 [`easyarchive-ui/tests/archive-group-runtime.test.ts`](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui/tests/archive-group-runtime.test.ts:1) 或新增独立测试文件，保持运行态规则可回归。

## 实施边界

本设计只覆盖以下范围：

1. 归档分组列表页运行态展示增强。
2. 归档分组详情页运行态展示增强。
3. 分组接口 DTO 的活动任务快照扩展。

明确不包含：

1. 历史累计迁移总数统计。
2. 任务进度真实性推导。
3. 数据库表结构变更。
4. 任务执行链路与事件机制调整。

## 预期结果

实现后，用户在归档分组页面即可看到：

1. 当前是否存在运行中的任务。
2. 当前任务已经迁移成功多少条记录。
3. 一个稳定、一致、不会误导为真实完成率的运行态进度条。

这样可以降低进入任务详情页查看运行态的频率，同时保持接口语义清晰，避免把模拟值固化为后端业务数据。
