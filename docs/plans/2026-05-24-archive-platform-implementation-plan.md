# Archive Platform Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在当前 `easy-archive` 仓库基础上升级到目标技术栈，并补齐归档管理后台、权限体系、可视化前端和全链路治理能力。

**Architecture:** 保留 `easyarchive-common` 与 `easyarchive-core` 作为归档引擎，升级 `easyarchive-starter` 为 Spring Boot 2.7 管理后台，并新增 `easyarchive-ui` 作为 Vue3 + TS 前端。配置、权限、日志、告警通过 MyBatis 落表，执行链继续复用 `ArchiveExecutor` 与 `SyncExecutor`。

**Tech Stack:** JDK11, Spring Boot 2.7.x, Spring Security, MyBatis, MySQL 5.7, Maven, Vue3, TypeScript, Vite, Pinia, Vue Router, Axios, ECharts

---

### Task 1: 升级工程底座

**Files:**
- Modify: `pom.xml`
- Modify: `easyarchive-common/pom.xml`
- Modify: `easyarchive-core/pom.xml`
- Modify: `easyarchive-starter/pom.xml`
- Create: `easyarchive-starter/src/main/resources/application.yml`

**Step 1:** 升级根 `pom.xml` 到 Spring Boot 2.7.x，并统一 JDK11、MyBatis、Security、Validation、Actuator 依赖版本。

**Step 2:** 为 `easyarchive-starter` 引入 Web、Security、Validation、MyBatis、Actuator、Test 依赖。

**Step 3:** 新增 `application.yml`，拆分服务端口、数据源、MyBatis、归档配置。

**Step 4:** 运行 `mvn -q -DskipTests compile` 验证依赖升级成功。

**Step 5:** 提交底座升级 commit。

### Task 2: 建立配置库模型与建表脚本

**Files:**
- Create: `easyarchive-starter/src/main/resources/db/migration/V1__init_archive_platform.sql`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/entity/`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/`
- Create: `easyarchive-starter/src/main/resources/mapper/`

**Step 1:** 根据设计文档创建用户、角色、权限、数据源、分组、规则、任务、日志、告警等表。

**Step 2:** 创建 Entity、Mapper、Mapper XML。

**Step 3:** 为高频查询设计索引与分页 SQL。

**Step 4:** 运行最小 Mapper 集成测试。

### Task 3: 认证与权限体系

**Files:**
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/config/SecurityConfig.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/security/`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/AuthController.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/AuthService.java`

**Step 1:** 先为登录、登出、当前用户接口编写失败测试和契约测试。

**Step 2:** 实现密码校验、JWT/会话落库、权限装载。

**Step 3:** 实现统一认证过滤器与未授权处理器。

**Step 4:** 运行认证相关测试。

### Task 4: 用户、角色、权限与操作日志

**Files:**
- Create: `UserController.java`
- Create: `RoleController.java`
- Create: `PermissionController.java`
- Create: `OperationLogController.java`
- Create: `OperationLogAspect.java`

**Step 1:** 实现用户、角色、权限树、用户角色绑定 API。

**Step 2:** 通过 AOP 记录高危操作日志和普通操作日志。

**Step 3:** 为删除、授权、状态切换、任务控制增加权限校验。

**Step 4:** 运行用户与日志模块测试。

### Task 5: 数据源管理闭环

**Files:**
- Create: `ArchiveDatasourceController.java`
- Create: `ArchiveDatasourceService.java`
- Create: `DatasourceConnectionTester.java`

**Step 1:** 实现数据源新增、编辑、查询、启停 API。

**Step 2:** 实现连接测试，校验 JDBC 连通性、认证、Schema 可访问、基础 SQL 可执行。

**Step 3:** 实现密码加密、脱敏回显和“保持原值”逻辑。

**Step 4:** 运行数据源服务测试和集成测试。

### Task 6: 分组与规则管理

**Files:**
- Create: `ArchiveGroupController.java`
- Create: `ArchiveRuleController.java`
- Create: `ArchiveGroupService.java`
- Create: `ArchiveRuleService.java`
- Create: `RuleCompilerService.java`

**Step 1:** 实现分组树查询、分组 CRUD、分组启停。

**Step 2:** 实现规则 CRUD、条件明细保存、优先级冲突校验。

**Step 3:** 将后台规则编译为 `ArchiveGroupItemByTime` / `ArchiveGroupItemById`。

**Step 4:** 运行规则合法性测试和服务测试。

### Task 7: 增强归档执行链

**Files:**
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/executor/ArchiveExecutor.java`
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/ArchiveGroupExecutor.java`
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/DbArchiveRuleLoader.java`
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveGroupItemById.java`
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveGroupItemByTime.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveTaskService.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/support/ArchiveTaskDispatcher.java`

**Step 1:** 先为任务启动、进度更新、取消检查补充测试。

**Step 2:** 实现 `DbArchiveRuleLoader` 从数据库加载规则。

**Step 3:** 完成 `valid()`、`isEnableClean()`、`isEnableWrite()` 等规则方法。

**Step 4:** 实现任务状态流转、进度更新、日志持久化、取消检查。

**Step 5:** 运行成功、失败、取消三类场景测试。

### Task 8: 监控与告警

**Files:**
- Create: `MonitorRuleController.java`
- Create: `AlertController.java`
- Create: `MonitorRuleService.java`
- Create: `AlertDispatchService.java`
- Create: `AlertEvaluatorJob.java`

**Step 1:** 实现监控规则 CRUD。

**Step 2:** 实现失败、心跳超时、低速率等阈值检测。

**Step 3:** 实现邮件、Webhook 通知发送与结果回写。

**Step 4:** 运行监控与告警测试。

### Task 9: 统一接口规范与文档

**Files:**
- Create: `GlobalExceptionHandler.java`
- Create: `ApiResponse.java`
- Create: `PageResponse.java`
- Create: `OpenApiConfig.java`
- Create: `ErrorCode.java`

**Step 1:** 实现统一响应体、统一异常和统一错误码。

**Step 2:** 为全部 Controller 补齐接口注解与权限注解。

**Step 3:** 生成并校验 OpenAPI 文档。

### Task 10: 初始化前端工程

**Files:**
- Create: `easyarchive-ui/package.json`
- Create: `easyarchive-ui/tsconfig.json`
- Create: `easyarchive-ui/vite.config.ts`
- Create: `easyarchive-ui/src/main.ts`
- Create: `easyarchive-ui/src/router/index.ts`
- Create: `easyarchive-ui/src/store/`
- Create: `easyarchive-ui/src/api/`

**Step 1:** 使用 Vue3 + TS + Vite 初始化项目。

**Step 2:** 接入 Pinia、Vue Router、Axios、ECharts、基础布局。

**Step 3:** 封装请求拦截器、权限指令、全局枚举。

**Step 4:** 运行 `npm run build` 验证可构建。

### Task 11: 落地前端业务页面

**Files:**
- Create: `src/views/login/`
- Create: `src/views/system/user/`
- Create: `src/views/archive/datasource/`
- Create: `src/views/archive/group/`
- Create: `src/views/archive/rule/`
- Create: `src/views/archive/task/`
- Create: `src/views/dashboard/`

**Step 1:** 完成登录、菜单、首页框架。

**Step 2:** 完成数据源、分组、规则、任务中心页面。

**Step 3:** 实现任务进度轮询、取消确认、日志抽屉、监控图表。

**Step 4:** 运行前端构建和关键页面回归。

### Task 12: 研发治理与上线门禁

**Files:**
- Create: `docs/testing/archive-platform-test-matrix.md`
- Create: `docs/review/archive-platform-code-review-checklist.md`
- Modify: `README.md`

**Step 1:** 建立单元测试、集成测试、接口测试、前端构建测试矩阵。

**Step 2:** 约束上线前通过率：单元测试 100%、集成测试 100%、关键接口回归 100%。

**Step 3:** 建立代码评审检查项：安全、幂等、索引、权限、日志、异常、取消能力、状态一致性。

**Step 4:** 执行 `mvn test`、`npm run build`、预发联调与上线演练。

## 交付顺序建议
- 第一阶段：Task 1-4，先补技术底座与权限体系。
- 第二阶段：Task 5-7，完成归档配置和任务执行闭环。
- 第三阶段：Task 8-11，补齐监控、前端和可视化。
- 第四阶段：Task 12，完成测试、评审和上线门禁。

## 测试标准
- 单元测试覆盖：服务层、规则校验、权限判断、速率计算、任务状态流转。
- 集成测试覆盖：登录鉴权、Mapper CRUD、数据源测试、任务触发与取消。
- 回归测试覆盖：关键接口、权限边界、任务中心、日志查询、大盘展示。
- 上线要求：所有门禁项通过率 100%，禁止带已知阻断缺陷上线。

## 代码评审检查点
- 是否符合统一包结构和命名规范。
- 是否存在越权访问、敏感信息明文、SQL 注入风险。
- 是否补齐任务状态流转、日志留痕、取消检查、异常处理。
- 是否建立必要索引并避免全表扫描。
- 是否保持前后端状态枚举、接口字段、错误码一致。
- 是否补齐对应测试并有明确验证证据。
