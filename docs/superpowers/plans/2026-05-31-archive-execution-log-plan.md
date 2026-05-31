# 归档任务执行日志功能 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现归档任务执行日志功能，基于事件驱动模式在关键节点记录结构化日志到数据库，提供 REST API 查询。

**Architecture:** 事件驱动（Observer/Publisher Pattern）。执行器发布不可变事件对象，监听器负责持久化。core 模块定义事件模型和接口，starter 模块提供 MyBatis 实现和 REST API。

**Tech Stack:** Java 11, Spring Boot 2.3.2, MyBatis 3.5.0, Lombok

**Spec:** `docs/superpowers/specs/2026-05-31-archive-execution-log-design.md`

**前置条件:** V1 迁移脚本已执行，`ea_archive_task_log` 表已存在（`log_type` 为 `VARCHAR(32)`）。

---

## 文件结构总览

### easyarchive-core 模块（新增）

| 文件 | 职责 |
|---|---|
| `core/event/ArchiveEventType.java` | 事件类型枚举 |
| `core/event/ArchiveEvent.java` | 事件基类 |
| `core/event/TaskStartEvent.java` | 任务开始事件 |
| `core/event/TaskEndEvent.java` | 任务结束事件 |
| `core/event/RuleStartEvent.java` | 规则开始事件 |
| `core/event/RuleEndEvent.java` | 规则结束事件 |
| `core/event/ArchiveEventPublisher.java` | 发布器接口 |
| `core/event/DefaultArchiveEventPublisher.java` | 默认发布器实现 |
| `core/event/NoOpArchiveEventPublisher.java` | 空发布器 |
| `core/listener/ArchiveEventListener.java` | 监听器接口 |
| `core/repository/ArchiveLogRepository.java` | 日志仓储接口 |

### easyarchive-core 模块（修改）

| 文件 | 改动 |
|---|---|
| `core/property/ArchiveConfig.java` | 新增 logEnabled、logRetentionDays |
| `core/rule/entity/ArchiveTaskLog.java` | logType 从 Integer 改为 String |
| `core/ArchiveGroupExecutor.java` | 新增 publisher，发布事件 |
| `core/executor/ArchiveExecutor.java` | 新增 publisher，发布事件，修正 bug |

### easyarchive-starter 模块（新增）

| 文件 | 职责 |
|---|---|
| `resources/db/migration/V2__add_archive_log_tables.sql` | 新增表 + 修改表 |
| `starter/mapper/ArchiveGroupExecuteTaskMapper.java` | 任务执行 Mapper |
| `starter/mapper/ArchiveTaskLogMapper.java` | 任务日志 Mapper |
| `resources/mapper/ArchiveGroupExecuteTaskMapper.xml` | 任务执行 SQL |
| `resources/mapper/ArchiveTaskLogMapper.xml` | 任务日志 SQL |
| `starter/repository/JdbcArchiveLogRepository.java` | JDBC 仓储实现 |
| `starter/listener/DbArchiveLogListener.java` | 数据库监听器 |
| `starter/service/ArchiveTaskLogService.java` | 查询服务接口 |
| `starter/service/impl/ArchiveTaskLogServiceImpl.java` | 查询服务实现 |
| `starter/controller/ArchiveTaskLogController.java` | REST API 控制器 |
| `starter/task/ArchiveLogCleanupTask.java` | 定时清理 |

### easyarchive-starter 模块（修改）

| 文件 | 改动 |
|---|---|
| `starter/config/EasyArchiveAutoConfiguration.java` | 注册 Bean + @EnableScheduling |
| `starter/config/SecurityConfig.java` | 白名单 task-log API |
| `resources/application.yml` | 新增日志配置项 |

---

## Task 1: 数据库迁移脚本

**Files:**
- Create: `easyarchive-starter/src/main/resources/db/migration/V2__add_archive_log_tables.sql`

**说明:** V1 已创建 `ea_archive_task_log` 表，但缺少 `deleted`、`updated_time`、`creator_id`、`updater_id` 列（这些是 `BaseEntity` 的字段）。同时需要新增 `ea_archive_group_execute_task` 表。

- [ ] **Step 1: 创建 V2 迁移脚本**

```sql
-- V2: 新增归档执行任务表 + 补全日志表字段

CREATE TABLE IF NOT EXISTS `ea_archive_group_execute_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_id` BIGINT NOT NULL COMMENT '归档分组 ID',
    `start_time` DATETIME NULL COMMENT '执行开始时间',
    `end_time` DATETIME NULL COMMENT '执行结束时间',
    `execute_status` VARCHAR(16) NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING/RUNNING/SUCCESS/FAILED',
    `error_msg` VARCHAR(1000) NULL COMMENT '执行异常信息',
    `processed_records` BIGINT NOT NULL DEFAULT 0 COMMENT '已处理记录数',
    `processed_speed` DECIMAL(18,2) NULL COMMENT '处理速度(记录/秒)',
    `heartbeat_time` DATETIME NULL COMMENT '最新心跳时间',
    `finished_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '0-未完成, 否则为id',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` VARCHAR(64) NULL COMMENT '创建人ID',
    `updater_id` VARCHAR(64) NULL COMMENT '更新人ID',
    `deleted` BIGINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    INDEX `idx_group_id` (`group_id`),
    INDEX `idx_execute_status` (`execute_status`),
    INDEX `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档执行任务表';

-- 补全 ea_archive_task_log 表缺少的 BaseEntity 字段
ALTER TABLE `ea_archive_task_log`
    ADD COLUMN `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_time`,
    ADD COLUMN `creator_id` VARCHAR(64) NULL COMMENT '创建人ID' AFTER `updated_time`,
    ADD COLUMN `updater_id` VARCHAR(64) NULL COMMENT '更新人ID' AFTER `creator_id`,
    ADD COLUMN `deleted` BIGINT NOT NULL DEFAULT 0 COMMENT '删除标记' AFTER `updater_id`;
```

- [ ] **Step 2: 提交**

```bash
git add easyarchive-starter/src/main/resources/db/migration/V2__add_archive_log_tables.sql
git commit -m "feat: add V2 migration for archive execution task and log tables"
```

---

## Task 2: 事件模型（Event Model）

**Files:**
- Create: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/ArchiveEventType.java`
- Create: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/ArchiveEvent.java`
- Create: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/TaskStartEvent.java`
- Create: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/TaskEndEvent.java`
- Create: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/RuleStartEvent.java`
- Create: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/RuleEndEvent.java`

- [ ] **Step 1: 创建事件类型枚举**

```java
package com.openquartz.easyarchive.core.event;

public enum ArchiveEventType {
    TASK_START,
    TASK_END,
    RULE_START,
    RULE_END
}
```

- [ ] **Step 2: 创建事件基类**

```java
package com.openquartz.easyarchive.core.event;

import java.util.UUID;

public abstract class ArchiveEvent {

    private final String eventId;
    private final ArchiveEventType type;
    private final Long taskId;
    private final Long groupId;
    private final long timestamp;

    protected ArchiveEvent(ArchiveEventType type, Long taskId, Long groupId) {
        this.eventId = UUID.randomUUID().toString();
        this.type = type;
        this.taskId = taskId;
        this.groupId = groupId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getEventId() { return eventId; }
    public ArchiveEventType getType() { return type; }
    public Long getTaskId() { return taskId; }
    public Long getGroupId() { return groupId; }
    public long getTimestamp() { return timestamp; }
}
```

- [ ] **Step 3: 创建 TaskStartEvent**

```java
package com.openquartz.easyarchive.core.event;

public class TaskStartEvent extends ArchiveEvent {

    private final int ruleCount;

    public TaskStartEvent(Long taskId, Long groupId, int ruleCount) {
        super(ArchiveEventType.TASK_START, taskId, groupId);
        this.ruleCount = ruleCount;
    }

    public int getRuleCount() { return ruleCount; }
}
```

- [ ] **Step 4: 创建 TaskEndEvent**

```java
package com.openquartz.easyarchive.core.event;

public class TaskEndEvent extends ArchiveEvent {

    private final boolean success;
    private final long totalRows;
    private final long elapsedMs;
    private final String errorMsg;

    public TaskEndEvent(Long taskId, Long groupId,
                        boolean success, long totalRows,
                        long elapsedMs, String errorMsg) {
        super(ArchiveEventType.TASK_END, taskId, groupId);
        this.success = success;
        this.totalRows = totalRows;
        this.elapsedMs = elapsedMs;
        this.errorMsg = errorMsg;
    }

    public boolean isSuccess() { return success; }
    public long getTotalRows() { return totalRows; }
    public long getElapsedMs() { return elapsedMs; }
    public String getErrorMsg() { return errorMsg; }
}
```

- [ ] **Step 5: 创建 RuleStartEvent**

```java
package com.openquartz.easyarchive.core.event;

public class RuleStartEvent extends ArchiveEvent {

    private final String sourceTable;
    private final String targetTable;
    private final String ruleType;

    public RuleStartEvent(Long taskId, Long groupId,
                          String sourceTable, String targetTable,
                          String ruleType) {
        super(ArchiveEventType.RULE_START, taskId, groupId);
        this.sourceTable = sourceTable;
        this.targetTable = targetTable;
        this.ruleType = ruleType;
    }

    public String getSourceTable() { return sourceTable; }
    public String getTargetTable() { return targetTable; }
    public String getRuleType() { return ruleType; }
}
```

- [ ] **Step 6: 创建 RuleEndEvent**

```java
package com.openquartz.easyarchive.core.event;

public class RuleEndEvent extends ArchiveEvent {

    private final String sourceTable;
    private final String targetTable;
    private final String ruleType;
    private final boolean success;
    private final long processedRows;
    private final long elapsedMs;
    private final String errorMsg;

    public RuleEndEvent(Long taskId, Long groupId,
                        String sourceTable, String targetTable,
                        String ruleType, boolean success,
                        long processedRows, long elapsedMs,
                        String errorMsg) {
        super(ArchiveEventType.RULE_END, taskId, groupId);
        this.sourceTable = sourceTable;
        this.targetTable = targetTable;
        this.ruleType = ruleType;
        this.success = success;
        this.processedRows = processedRows;
        this.elapsedMs = elapsedMs;
        this.errorMsg = errorMsg;
    }

    public String getSourceTable() { return sourceTable; }
    public String getTargetTable() { return targetTable; }
    public String getRuleType() { return ruleType; }
    public boolean isSuccess() { return success; }
    public long getProcessedRows() { return processedRows; }
    public long getElapsedMs() { return elapsedMs; }
    public String getErrorMsg() { return errorMsg; }
}
```

- [ ] **Step 7: 编译验证**

Run: `mvn compile -pl easyarchive-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: 提交**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/ArchiveEventType.java \
       easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/ArchiveEvent.java \
       easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/TaskStartEvent.java \
       easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/TaskEndEvent.java \
       easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/RuleStartEvent.java \
       easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/RuleEndEvent.java
git commit -m "feat: add archive event model classes"
```

---

## Task 3: 事件发布器与监听器接口

**Files:**
- Create: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/ArchiveEventPublisher.java`
- Create: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/DefaultArchiveEventPublisher.java`
- Create: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/NoOpArchiveEventPublisher.java`
- Create: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/listener/ArchiveEventListener.java`

- [ ] **Step 1: 创建监听器接口**

```java
package com.openquartz.easyarchive.core.listener;

import com.openquartz.easyarchive.core.event.ArchiveEvent;

public interface ArchiveEventListener {
    void onEvent(ArchiveEvent event);
}
```

- [ ] **Step 2: 创建发布器接口**

```java
package com.openquartz.easyarchive.core.event;

import com.openquartz.easyarchive.core.listener.ArchiveEventListener;

public interface ArchiveEventPublisher {
    void publish(ArchiveEvent event);
    void registerListener(ArchiveEventListener listener);
}
```

- [ ] **Step 3: 创建默认发布器**

```java
package com.openquartz.easyarchive.core.event;

import com.openquartz.easyarchive.core.listener.ArchiveEventListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultArchiveEventPublisher implements ArchiveEventPublisher {

    private final List<ArchiveEventListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void publish(ArchiveEvent event) {
        for (ArchiveEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.error("[ArchiveEventPublisher] listener error, eventType:{}",
                    event.getType(), e);
            }
        }
    }

    @Override
    public void registerListener(ArchiveEventListener listener) {
        listeners.add(listener);
    }
}
```

- [ ] **Step 4: 创建空发布器**

```java
package com.openquartz.easyarchive.core.event;

import com.openquartz.easyarchive.core.listener.ArchiveEventListener;

public class NoOpArchiveEventPublisher implements ArchiveEventPublisher {

    public static final NoOpArchiveEventPublisher INSTANCE = new NoOpArchiveEventPublisher();

    @Override
    public void publish(ArchiveEvent event) { }

    @Override
    public void registerListener(ArchiveEventListener listener) { }
}
```

- [ ] **Step 5: 编译验证**

Run: `mvn compile -pl easyarchive-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/ArchiveEventPublisher.java \
       easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/DefaultArchiveEventPublisher.java \
       easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/NoOpArchiveEventPublisher.java \
       easyarchive-core/src/main/java/com/openquartz/easyarchive/core/listener/
git commit -m "feat: add event publisher and listener interfaces"
```

---

## Task 4: 仓储接口 + 实体修复 + 配置扩展

**Files:**
- Create: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/repository/ArchiveLogRepository.java`
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveTaskLog.java`
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/property/ArchiveConfig.java`

- [ ] **Step 1: 创建 ArchiveLogRepository 接口**

```java
package com.openquartz.easyarchive.core.repository;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import java.util.List;

public interface ArchiveLogRepository {

    void saveTaskExecution(ArchiveGroupExecuteTask task);

    void updateTaskExecution(ArchiveGroupExecuteTask task);

    void saveTaskLog(ArchiveTaskLog log);

    ArchiveGroupExecuteTask queryTaskById(Long taskId);

    List<ArchiveGroupExecuteTask> queryTasks(int page, int size, String status);

    int countTasks(String status);

    List<ArchiveTaskLog> queryLogsByTaskId(Long taskId, int page, int size, String executePhase);

    int countLogsByTaskId(Long taskId, String executePhase);

    int deleteByRetentionDays(int retentionDays);
}
```

- [ ] **Step 2: 修改 ArchiveTaskLog 实体 -- logType 改为 String**

V1 表中 `log_type` 是 `VARCHAR(32)`，值为 `'START'/'PROGRESS'/'FINISH'/'ERROR'/'CANCEL'`。需将实体字段从 `Integer` 改为 `String`。

修改 `ArchiveTaskLog.java` 第 24-26 行：

```java
    /**
     * 日志类型: START/PROGRESS/FINISH/ERROR/CANCEL
     */
    private String logType;
```

- [ ] **Step 3: 扩展 ArchiveConfig**

在 `ArchiveConfig.java` 的 `archivePauseMs` 字段之后添加：

```java
    /**
     * 是否启用执行日志
     */
    @Value("${sync.log.enabled:true}")
    private boolean logEnabled;

    /**
     * 日志保留天数
     */
    @Value("${sync.log.retention-days:30}")
    private int logRetentionDays;
```

- [ ] **Step 4: 编译验证**

Run: `mvn compile -pl easyarchive-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/repository/ \
       easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveTaskLog.java \
       easyarchive-core/src/main/java/com/openquartz/easyarchive/core/property/ArchiveConfig.java
git commit -m "feat: add log repository interface, fix ArchiveTaskLog entity, add config properties"
```

---

## Task 5: MyBatis Mapper 与 JDBC 仓储实现

**Files:**
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupExecuteTaskMapper.java`
- Create: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupExecuteTaskMapper.xml`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveTaskLogMapper.java`
- Create: `easyarchive-starter/src/main/resources/mapper/ArchiveTaskLogMapper.xml`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/repository/JdbcArchiveLogRepository.java`

- [ ] **Step 1: 创建 ArchiveGroupExecuteTaskMapper 接口**

```java
package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ArchiveGroupExecuteTaskMapper {

    int insert(ArchiveGroupExecuteTask task);

    int update(ArchiveGroupExecuteTask task);

    ArchiveGroupExecuteTask selectById(@Param("id") Long id);

    List<ArchiveGroupExecuteTask> selectPage(@Param("offset") int offset,
                                              @Param("size") int size,
                                              @Param("status") String status);

    int count(@Param("status") String status);

    int deleteByRetentionDays(@Param("retentionDays") int retentionDays);
}
```

- [ ] **Step 2: 创建 ArchiveGroupExecuteTaskMapper XML**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper">

    <resultMap id="BaseResultMap" type="com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask">
        <id column="id" property="id"/>
        <result column="group_id" property="groupId"/>
        <result column="start_time" property="startTime"/>
        <result column="end_time" property="endTime"/>
        <result column="execute_status" property="executeStatus"/>
        <result column="error_msg" property="errorMsg"/>
        <result column="processed_records" property="processedRecords"/>
        <result column="processed_speed" property="processedSpeed"/>
        <result column="heartbeat_time" property="heartbeatTime"/>
        <result column="finished_flag" property="finishedFlag"/>
        <result column="created_time" property="createdTime"/>
        <result column="updated_time" property="updatedTime"/>
        <result column="creator_id" property="creatorId"/>
        <result column="updater_id" property="updaterId"/>
        <result column="deleted" property="deleted"/>
    </resultMap>

    <sql id="Base_Column_List">
        id, group_id, start_time, end_time, execute_status, error_msg,
        processed_records, processed_speed, heartbeat_time, finished_flag,
        created_time, updated_time, creator_id, updater_id, deleted
    </sql>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO ea_archive_group_execute_task (
            group_id, start_time, execute_status, processed_records
        ) VALUES (
            #{groupId}, #{startTime}, #{executeStatus}, #{processedRecords}
        )
    </insert>

    <update id="update">
        UPDATE ea_archive_group_execute_task
        <set>
            <if test="endTime != null">end_time = #{endTime},</if>
            <if test="executeStatus != null">execute_status = #{executeStatus},</if>
            <if test="errorMsg != null">error_msg = #{errorMsg},</if>
            <if test="processedRecords != null">processed_records = #{processedRecords},</if>
            <if test="processedSpeed != null">processed_speed = #{processedSpeed},</if>
            <if test="heartbeatTime != null">heartbeat_time = #{heartbeatTime},</if>
            <if test="finishedFlag != null">finished_flag = #{finishedFlag},</if>
            updated_time = NOW()
        </set>
        WHERE id = #{id} AND deleted = 0
    </update>

    <select id="selectById" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM ea_archive_group_execute_task
        WHERE id = #{id} AND deleted = 0
    </select>

    <select id="selectPage" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM ea_archive_group_execute_task
        <where>
            <if test="status != null and status != ''">AND execute_status = #{status}</if>
            AND deleted = 0
        </where>
        ORDER BY id DESC
        LIMIT #{offset}, #{size}
    </select>

    <select id="count" resultType="int">
        SELECT COUNT(1)
        FROM ea_archive_group_execute_task
        <where>
            <if test="status != null and status != ''">AND execute_status = #{status}</if>
            AND deleted = 0
        </where>
    </select>

    <delete id="deleteByRetentionDays">
        UPDATE ea_archive_group_execute_task
        SET deleted = id, updated_time = NOW()
        WHERE deleted = 0
          AND start_time &lt; DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)
    </delete>

</mapper>
```

- [ ] **Step 3: 创建 ArchiveTaskLogMapper 接口**

```java
package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ArchiveTaskLogMapper {

    int insert(ArchiveTaskLog log);

    List<ArchiveTaskLog> selectByTaskId(@Param("taskId") Long taskId,
                                         @Param("offset") int offset,
                                         @Param("size") int size,
                                         @Param("executePhase") String executePhase);

    int countByTaskId(@Param("taskId") Long taskId,
                      @Param("executePhase") String executePhase);

    int deleteByRetentionDays(@Param("retentionDays") int retentionDays);
}
```

- [ ] **Step 4: 创建 ArchiveTaskLogMapper XML**

注意：`ea_archive_task_log` 表的 `log_type` 是 `VARCHAR(32)`，`process_speed` 是 `DECIMAL(18,2)`，V2 新增了 `deleted`/`updated_time`/`creator_id`/`updater_id` 列。

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.openquartz.easyarchive.starter.mapper.ArchiveTaskLogMapper">

    <resultMap id="BaseResultMap" type="com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog">
        <id column="id" property="id"/>
        <result column="task_id" property="taskId"/>
        <result column="log_type" property="logType"/>
        <result column="log_level" property="logLevel"/>
        <result column="log_content" property="logContent"/>
        <result column="log_time" property="logTime"/>
        <result column="processed_count" property="processedCount"/>
        <result column="process_speed" property="processSpeed"/>
        <result column="execute_phase" property="executePhase"/>
        <result column="created_time" property="createdTime"/>
        <result column="updated_time" property="updatedTime"/>
        <result column="creator_id" property="creatorId"/>
        <result column="updater_id" property="updaterId"/>
        <result column="deleted" property="deleted"/>
    </resultMap>

    <sql id="Base_Column_List">
        id, task_id, log_type, log_level, log_content, log_time,
        processed_count, process_speed, execute_phase,
        created_time, updated_time, creator_id, updater_id, deleted
    </sql>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO ea_archive_task_log (
            task_id, log_type, log_level, log_content, log_time,
            processed_count, process_speed, execute_phase
        ) VALUES (
            #{taskId}, #{logType}, #{logLevel}, #{logContent}, #{logTime},
            #{processedCount}, #{processSpeed}, #{executePhase}
        )
    </insert>

    <select id="selectByTaskId" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM ea_archive_task_log
        <where>
            task_id = #{taskId}
            <if test="executePhase != null and executePhase != ''">
                AND execute_phase = #{executePhase}
            </if>
            AND deleted = 0
        </where>
        ORDER BY id ASC
        LIMIT #{offset}, #{size}
    </select>

    <select id="countByTaskId" resultType="int">
        SELECT COUNT(1)
        FROM ea_archive_task_log
        <where>
            task_id = #{taskId}
            <if test="executePhase != null and executePhase != ''">
                AND execute_phase = #{executePhase}
            </if>
            AND deleted = 0
        </where>
    </select>

    <delete id="deleteByRetentionDays">
        UPDATE ea_archive_task_log
        SET deleted = id, updated_time = NOW()
        WHERE deleted = 0
          AND log_time &lt; DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)
    </delete>

</mapper>
```

- [ ] **Step 5: 创建 JdbcArchiveLogRepository**

```java
package com.openquartz.easyarchive.starter.repository;

import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveTaskLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcArchiveLogRepository implements ArchiveLogRepository {

    private final ArchiveGroupExecuteTaskMapper executeTaskMapper;
    private final ArchiveTaskLogMapper taskLogMapper;

    @Override
    public void saveTaskExecution(ArchiveGroupExecuteTask task) {
        executeTaskMapper.insert(task);
    }

    @Override
    public void updateTaskExecution(ArchiveGroupExecuteTask task) {
        executeTaskMapper.update(task);
    }

    @Override
    public void saveTaskLog(ArchiveTaskLog log) {
        taskLogMapper.insert(log);
    }

    @Override
    public ArchiveGroupExecuteTask queryTaskById(Long taskId) {
        return executeTaskMapper.selectById(taskId);
    }

    @Override
    public List<ArchiveGroupExecuteTask> queryTasks(int page, int size, String status) {
        int offset = (page - 1) * size;
        return executeTaskMapper.selectPage(offset, size, status);
    }

    @Override
    public int countTasks(String status) {
        return executeTaskMapper.count(status);
    }

    @Override
    public List<ArchiveTaskLog> queryLogsByTaskId(Long taskId, int page, int size, String executePhase) {
        int offset = (page - 1) * size;
        return taskLogMapper.selectByTaskId(taskId, offset, size, executePhase);
    }

    @Override
    public int countLogsByTaskId(Long taskId, String executePhase) {
        return taskLogMapper.countByTaskId(taskId, executePhase);
    }

    @Override
    public int deleteByRetentionDays(int retentionDays) {
        int taskLogs = taskLogMapper.deleteByRetentionDays(retentionDays);
        int tasks = executeTaskMapper.deleteByRetentionDays(retentionDays);
        return taskLogs + tasks;
    }
}
```

- [ ] **Step 6: 编译验证**

Run: `mvn compile -pl easyarchive-starter -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupExecuteTaskMapper.java \
       easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveTaskLogMapper.java \
       easyarchive-starter/src/main/resources/mapper/ArchiveGroupExecuteTaskMapper.xml \
       easyarchive-starter/src/main/resources/mapper/ArchiveTaskLogMapper.xml \
       easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/repository/
git commit -m "feat: add MyBatis mappers and JDBC log repository"
```

---

## Task 6: 数据库日志监听器与 Bean 装配

**Files:**
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/listener/DbArchiveLogListener.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/config/EasyArchiveAutoConfiguration.java`
- Modify: `easyarchive-starter/src/main/resources/application.yml`

**说明:** `ea_archive_task_log.log_type` 使用 V1 定义的 VARCHAR 值：`START`/`PROGRESS`/`FINISH`/`ERROR`。Task 级事件用 `START`/`FINISH`/`ERROR`，Rule 级事件用 `PROGRESS`/`FINISH`/`ERROR`。

- [ ] **Step 1: 创建 DbArchiveLogListener**

```java
package com.openquartz.easyarchive.starter.listener;

import com.openquartz.easyarchive.core.event.*;
import com.openquartz.easyarchive.core.listener.ArchiveEventListener;
import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@RequiredArgsConstructor
public class DbArchiveLogListener implements ArchiveEventListener {

    private final ArchiveLogRepository repository;

    @Override
    public void onEvent(ArchiveEvent event) {
        if (event instanceof TaskStartEvent) {
            handleTaskStart((TaskStartEvent) event);
        } else if (event instanceof TaskEndEvent) {
            handleTaskEnd((TaskEndEvent) event);
        } else if (event instanceof RuleStartEvent) {
            handleRuleStart((RuleStartEvent) event);
        } else if (event instanceof RuleEndEvent) {
            handleRuleEnd((RuleEndEvent) event);
        }
    }

    private void handleTaskStart(TaskStartEvent event) {
        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(event.getTaskId());
        task.setGroupId(event.getGroupId());
        task.setStartTime(new Date(event.getTimestamp()));
        task.setExecuteStatus("RUNNING");
        task.setProcessedRecords(0L);
        repository.updateTaskExecution(task);

        saveLog(event.getTaskId(), "START", "INFO",
                "任务开始，规则数:" + event.getRuleCount(),
                "TASK_START", 0L, BigDecimal.ZERO, new Date(event.getTimestamp()));
    }

    private void handleTaskEnd(TaskEndEvent event) {
        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(event.getTaskId());
        task.setEndTime(new Date(event.getTimestamp()));
        task.setExecuteStatus(event.isSuccess() ? "SUCCESS" : "FAILED");
        task.setProcessedRecords(event.getTotalRows());
        task.setFinishedFlag(event.isSuccess() ? event.getTaskId() : 0L);
        if (event.getElapsedMs() > 0) {
            task.setProcessedSpeed(BigDecimal.valueOf(event.getTotalRows() * 1000.0 / event.getElapsedMs()));
        }
        if (!event.isSuccess() && event.getErrorMsg() != null) {
            task.setErrorMsg(event.getErrorMsg());
        }
        repository.updateTaskExecution(task);

        String content = event.isSuccess()
                ? "任务完成，总行数:" + event.getTotalRows() + "，耗时:" + event.getElapsedMs() + "ms"
                : "任务失败:" + event.getErrorMsg();
        String logType = event.isSuccess() ? "FINISH" : "ERROR";
        String level = event.isSuccess() ? "INFO" : "ERROR";
        saveLog(event.getTaskId(), logType, level, content,
                "TASK_END", event.getTotalRows(), BigDecimal.ZERO, new Date(event.getTimestamp()));
    }

    private void handleRuleStart(RuleStartEvent event) {
        String content = "规则开始:" + event.getSourceTable() + " -> " + event.getTargetTable()
                + ", 类型:" + event.getRuleType();
        saveLog(event.getTaskId(), "START", "INFO", content,
                "RULE_START", 0L, BigDecimal.ZERO, new Date(event.getTimestamp()));
    }

    private void handleRuleEnd(RuleEndEvent event) {
        BigDecimal speed = event.getElapsedMs() > 0
                ? BigDecimal.valueOf(event.getProcessedRows() * 1000.0 / event.getElapsedMs())
                : BigDecimal.ZERO;
        String content = event.isSuccess()
                ? "规则完成:" + event.getSourceTable() + " -> " + event.getTargetTable()
                    + ", 处理:" + event.getProcessedRows() + "行, 耗时:" + event.getElapsedMs() + "ms"
                : "规则失败:" + event.getSourceTable() + " -> " + event.getTargetTable()
                    + ", " + event.getErrorMsg();
        String logType = event.isSuccess() ? "FINISH" : "ERROR";
        String level = event.isSuccess() ? "INFO" : "ERROR";
        saveLog(event.getTaskId(), logType, level, content,
                "RULE_END", event.getProcessedRows(), speed, new Date(event.getTimestamp()));
    }

    private void saveLog(Long taskId, String logType, String logLevel,
                         String logContent, String executePhase,
                         Long processedCount, BigDecimal processSpeed, Date logTime) {
        ArchiveTaskLog log = new ArchiveTaskLog();
        log.setTaskId(taskId);
        log.setLogType(logType);
        log.setLogLevel(logLevel);
        log.setLogContent(logContent);
        log.setExecutePhase(executePhase);
        log.setProcessedCount(processedCount);
        log.setProcessSpeed(processSpeed);
        log.setLogTime(logTime);
        repository.saveTaskLog(log);
    }
}
```

- [ ] **Step 2: 修改 ArchiveTaskLog.processSpeed 类型为 BigDecimal**

`ea_archive_task_log.process_speed` 是 `DECIMAL(18,2)`，`ArchiveTaskLog.processSpeed` 当前是 `Long`。需改为 `BigDecimal`。

修改 `ArchiveTaskLog.java` 的 `processSpeed` 字段：

```java
    /**
     * 处理速度
     */
    private java.math.BigDecimal processSpeed;
```

- [ ] **Step 3: 修改 EasyArchiveAutoConfiguration 装配 Bean**

在 `EasyArchiveAutoConfiguration.java` 中添加：

```java
import com.openquartz.easyarchive.core.event.ArchiveEventPublisher;
import com.openquartz.easyarchive.core.event.DefaultArchiveEventPublisher;
import com.openquartz.easyarchive.core.event.NoOpArchiveEventPublisher;
import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.starter.listener.DbArchiveLogListener;
import org.springframework.scheduling.annotation.EnableScheduling;
```

在类上添加 `@EnableScheduling` 注解：

```java
@Configuration
@EnableScheduling
@EnableConfigurationProperties({ArchiveConfig.class, ConnectionProperties.class})
public class EasyArchiveAutoConfiguration {
```

在类末尾添加 Bean 方法：

```java
    @Bean
    public ArchiveEventPublisher archiveEventPublisher(ArchiveConfig archiveConfig,
                                                        ArchiveLogRepository archiveLogRepository) {
        if (!archiveConfig.isLogEnabled()) {
            return NoOpArchiveEventPublisher.INSTANCE;
        }
        DefaultArchiveEventPublisher publisher = new DefaultArchiveEventPublisher();
        publisher.registerListener(new DbArchiveLogListener(archiveLogRepository));
        return publisher;
    }
```

- [ ] **Step 4: 更新 application.yml**

在 `application.yml` 的 `sync` 配置段中添加：

```yaml
  log:
    enabled: true
    retention-days: 30
```

- [ ] **Step 5: 编译验证**

Run: `mvn compile -pl easyarchive-starter -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/listener/ \
       easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/config/EasyArchiveAutoConfiguration.java \
       easyarchive-starter/src/main/resources/application.yml \
       easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveTaskLog.java
git commit -m "feat: add DbArchiveLogListener, auto-configuration wiring, and enable scheduling"
```

---

## Task 7: 执行器集成（ArchiveGroupExecutor + ArchiveExecutor）

**Files:**
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/ArchiveGroupExecutor.java`
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/executor/ArchiveExecutor.java`

- [ ] **Step 1: 改造 ArchiveGroupExecutor**

完整替换 `ArchiveGroupExecutor.java`：

```java
package com.openquartz.easyarchive.core;

import com.openquartz.easyarchive.common.entity.Pair;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.event.ArchiveEventPublisher;
import com.openquartz.easyarchive.core.event.TaskEndEvent;
import com.openquartz.easyarchive.core.event.TaskStartEvent;
import com.openquartz.easyarchive.core.executor.ArchiveExecutor;
import com.openquartz.easyarchive.core.property.ArchiveConfig;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItem;
import com.openquartz.easyarchive.core.rule.ArchiveRuleLoader;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import com.openquartz.easyarchive.common.util.ExceptionUtils;

@Slf4j
public class ArchiveGroupExecutor implements Runnable {

    private final ArchiveRuleLoader loader;
    private final ArchiveConfig archiveConfig;
    private final ArchiveGroupExecuteTask executeTask;
    private final Pair<ArchiveConnection, ArchiveConnection> connectionInfo;
    private final ArchiveEventPublisher publisher;

    public ArchiveGroupExecutor(ArchiveRuleLoader loader,
                                ArchiveConfig archiveConfig,
                                ArchiveGroupExecuteTask executeTask,
                                Pair<ArchiveConnection, ArchiveConnection> connectionInfo,
                                ArchiveEventPublisher publisher) {
        this.loader = loader;
        this.archiveConfig = archiveConfig;
        this.executeTask = executeTask;
        this.connectionInfo = connectionInfo;
        this.publisher = publisher;
    }

    @Override
    public void run() {
        Date startTime = new Date();
        long startMs = System.currentTimeMillis();

        try {
            List<ArchiveGroupItem> configs = loader.load();
            configs.sort(Comparator.comparing(ArchiveGroupItem::getPriority));

            log.info("[ArchiveGroupExecutor#run] start archive, taskId:{}, ruleCount:{}",
                executeTask.getId(), configs.size());

            publisher.publish(new TaskStartEvent(
                executeTask.getId(), executeTask.getGroupId(), configs.size()));

            doExecute(configs);

            long elapsed = System.currentTimeMillis() - startMs;

            publisher.publish(new TaskEndEvent(
                executeTask.getId(), executeTask.getGroupId(),
                true, 0L, elapsed, null));

            log.info("[ArchiveGroupExecutor#run] archive completed, taskId:{}, elapsed:{}ms",
                executeTask.getId(), elapsed);

        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - startMs;

            publisher.publish(new TaskEndEvent(
                executeTask.getId(), executeTask.getGroupId(),
                false, 0L, elapsed, ex.getMessage()));

            log.error("[ArchiveGroupExecutor#run] archive failed, taskId:{}", executeTask.getId(), ex);
            ExceptionUtils.rethrow(ex);
        }
    }

    private void doExecute(List<ArchiveGroupItem> configs) {
        new ArchiveExecutor(connectionInfo.getKey(), connectionInfo.getValue(),
            archiveConfig, configs, executeTask.getId(), publisher).run();
    }
}
```

- [ ] **Step 2: 改造 ArchiveExecutor**

完整替换 `ArchiveExecutor.java`：

```java
package com.openquartz.easyarchive.core.executor;

import com.google.common.base.Stopwatch;
import com.openquartz.easyarchive.common.api.model.TableInfo;
import com.openquartz.easyarchive.common.util.DateUtils;
import com.openquartz.easyarchive.common.util.ExceptionUtils;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.event.ArchiveEventPublisher;
import com.openquartz.easyarchive.core.event.RuleEndEvent;
import com.openquartz.easyarchive.core.event.RuleStartEvent;
import com.openquartz.easyarchive.core.expr.ExpressionService;
import com.openquartz.easyarchive.core.property.ArchiveConfig;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItem;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime;
import com.openquartz.easyarchive.core.sink.EmptySink;
import lombok.extern.slf4j.Slf4j;
import com.openquartz.easyarchive.common.api.Sink;
import com.openquartz.easyarchive.common.api.PageSource;
import com.openquartz.easyarchive.core.SyncExecutor;
import com.openquartz.easyarchive.core.sink.mysql.MysqlSink;
import com.openquartz.easyarchive.core.source.mysql.MysqlSource;

@Slf4j
public class ArchiveExecutor implements Runnable {

    private final ArchiveConnection sourceConnection;
    private final ArchiveConnection sinkConnection;
    private final ArchiveConfig archiveConfig;
    private final List<ArchiveGroupItem> ruleList;
    private final Long taskId;
    private final ArchiveEventPublisher publisher;

    public ArchiveExecutor(ArchiveConnection sourceConnection,
                           ArchiveConnection sinkConnection,
                           ArchiveConfig archiveConfig,
                           List<ArchiveGroupItem> ruleList,
                           Long taskId,
                           ArchiveEventPublisher publisher) {
        this.sourceConnection = sourceConnection;
        this.sinkConnection = sinkConnection;
        this.archiveConfig = archiveConfig;
        this.ruleList = ruleList;
        this.taskId = taskId;
        this.publisher = publisher;
    }

    @Override
    public void run() {

        long totalProcessRecords = 0;
        long startTime = System.currentTimeMillis();

        for (ArchiveGroupItem rule : ruleList) {

            checkCancellation();

            String ruleType = (rule instanceof ArchiveGroupItemByTime) ? "TIME" : "ID";

            publisher.publish(new RuleStartEvent(
                taskId, rule.getGroupId(),
                rule.getSourceTable(), rule.getTargetTable(), ruleType));

            Stopwatch watch = Stopwatch.createStarted();
            int executeRows = 0;
            String fetchSql = ExpressionService.getInstance().parse(rule.getFetchSql());
            int effectivePauseMs = resolvePauseMs(rule);

            try {
                try (PageSource reader = new MysqlSource(sourceConnection,
                        rule.getGroupId(),
                        TableInfo.of(ExpressionService.getInstance().parse(rule.getSourceTable()), rule.getIdColumn()),
                        fetchSql,
                        rule.isEnableClean(),
                        rule.getDeleteWhere());
                     Sink sink = createSink(rule, sinkConnection);
                     SyncExecutor executor = new SyncExecutor(archiveConfig, reader, sink, effectivePauseMs)) {

                    if (rule instanceof ArchiveGroupItemByTime) {
                        ArchiveGroupItemByTime byTimeRule = (ArchiveGroupItemByTime) rule;
                        Date endDate = DateUtils.floorDay(DateUtils.addDays(new Date(), -byTimeRule.getKeepDay()));
                        Date startDate = DateUtils.floorDay(byTimeRule.getStartTime());

                        for (Date curDate = startDate; Objects.requireNonNull(curDate).compareTo(endDate) < 0; ) {
                            checkCancellation();
                            Date curEndDate = DateUtils.addHours(curDate, 1);
                            int batchRows = executor.execute(curDate, curEndDate, byTimeRule.getStepCount());
                            executeRows += batchRows;
                            totalProcessRecords += batchRows;
                            updateProcess(totalProcessRecords, startTime);
                            curDate = curEndDate;
                        }
                    }

                    if (rule instanceof ArchiveGroupItemById) {
                        ArchiveGroupItemById byIdRule = (ArchiveGroupItemById) rule;
                        String startIdStr = ExpressionService.getInstance().parse(byIdRule.getStartId());
                        Long startId = Long.valueOf(startIdStr);
                        String endIdStr = ExpressionService.getInstance().parse(byIdRule.getEndId());
                        Long endId = Long.valueOf(endIdStr);

                        while (Objects.requireNonNull(startId).compareTo(endId) < 0) {
                            checkCancellation();
                            Long curEndId = startId + byIdRule.getStepRounds();
                            int batchRows = executor.execute(startId, curEndId, byIdRule.getStepCount());
                            executeRows += batchRows;
                            totalProcessRecords += batchRows;
                            updateProcess(totalProcessRecords, startTime);
                            startId = curEndId;
                        }
                    }

                    watch.stop();
                    long elapsedMilliseconds = watch.elapsed(TimeUnit.MILLISECONDS);

                    log.info("[ArchiveExecutor] {} -> {}, execute completed, archive-rows:{}, take {}ms",
                        rule.getSourceTable(), rule.getTargetTable(), executeRows, elapsedMilliseconds);

                    publisher.publish(new RuleEndEvent(
                        taskId, rule.getGroupId(),
                        rule.getSourceTable(), rule.getTargetTable(),
                        ruleType, true, executeRows, elapsedMilliseconds, null));

                } catch (Exception e) {
                    watch.stop();
                    long elapsed = watch.elapsed(TimeUnit.MILLISECONDS);

                    publisher.publish(new RuleEndEvent(
                        taskId, rule.getGroupId(),
                        rule.getSourceTable(), rule.getTargetTable(),
                        ruleType, false, executeRows, elapsed, e.getMessage()));

                    log.error("[ArchiveExecutor] {} -> {}, execute error",
                        rule.getSourceTable(), rule.getTargetTable(), e);
                    ExceptionUtils.rethrow(e);
                }
            } catch (Exception ex) {
                log.error("[ArchiveExecutor] {}, execute error!", rule.getSourceTable(), ex);
                throw ex;
            }
        }
    }

    private void updateProcess(long totalProcessRecords, long startTime) {
        // 更新进度到执行任务 @TODO
    }

    private Sink createSink(ArchiveGroupItem rule, ArchiveConnection targetConnection) {
        if (!rule.isEnableWrite()) {
            return new EmptySink();
        }
        return new MysqlSink(targetConnection, rule.getTargetTable());
    }

    private int resolvePauseMs(ArchiveGroupItem rule) {
        if (rule.getPauseMs() != null && rule.getPauseMs() != 0) {
            return rule.getPauseMs();
        }
        return archiveConfig.getArchivePauseMs();
    }

    private void checkCancellation() {
        // TODO 检查任务是否已经被取消。
    }
}
```

- [ ] **Step 3: 全量编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/ArchiveGroupExecutor.java \
       easyarchive-core/src/main/java/com/openquartz/easyarchive/core/executor/ArchiveExecutor.java
git commit -m "feat: integrate event publisher into ArchiveGroupExecutor and ArchiveExecutor"
```

---

## Task 8: REST API、查询服务与定时清理

**Files:**
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveTaskLogService.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveTaskLogServiceImpl.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveTaskLogController.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/task/ArchiveLogCleanupTask.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/config/SecurityConfig.java`

- [ ] **Step 1: 创建 ArchiveTaskLogService 接口**

```java
package com.openquartz.easyarchive.starter.service;

import java.util.Map;

public interface ArchiveTaskLogService {

    Map<String, Object> queryTasks(int page, int size, String status);

    Object queryTaskById(Long taskId);

    Map<String, Object> queryLogsByTaskId(Long taskId, int page, int size, String executePhase);

    int cleanup(int retentionDays);
}
```

- [ ] **Step 2: 创建 ArchiveTaskLogServiceImpl**

```java
package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import com.openquartz.easyarchive.starter.service.ArchiveTaskLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArchiveTaskLogServiceImpl implements ArchiveTaskLogService {

    private final ArchiveLogRepository archiveLogRepository;

    @Override
    public Map<String, Object> queryTasks(int page, int size, String status) {
        List<ArchiveGroupExecuteTask> list = archiveLogRepository.queryTasks(page, size, status);
        int total = archiveLogRepository.countTasks(status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    public Object queryTaskById(Long taskId) {
        return archiveLogRepository.queryTaskById(taskId);
    }

    @Override
    public Map<String, Object> queryLogsByTaskId(Long taskId, int page, int size, String executePhase) {
        List<ArchiveTaskLog> list = archiveLogRepository.queryLogsByTaskId(taskId, page, size, executePhase);
        int total = archiveLogRepository.countLogsByTaskId(taskId, executePhase);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    public int cleanup(int retentionDays) {
        return archiveLogRepository.deleteByRetentionDays(retentionDays);
    }
}
```

- [ ] **Step 3: 创建 ArchiveTaskLogController**

```java
package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.starter.model.dto.ApiResponse;
import com.openquartz.easyarchive.starter.service.ArchiveTaskLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/task-log")
@RequiredArgsConstructor
public class ArchiveTaskLogController {

    private final ArchiveTaskLogService taskLogService;

    @GetMapping("/tasks")
    public ApiResponse<Map<String, Object>> getTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(taskLogService.queryTasks(page, size, status));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<Object> getTask(@PathVariable Long taskId) {
        Object task = taskLogService.queryTaskById(taskId);
        if (task == null) {
            return ApiResponse.error("NOT_FOUND", "任务不存在");
        }
        return ApiResponse.success(task);
    }

    @GetMapping("/tasks/{taskId}/logs")
    public ApiResponse<Map<String, Object>> getTaskLogs(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String executePhase) {
        return ApiResponse.success(taskLogService.queryLogsByTaskId(taskId, page, size, executePhase));
    }

    @PostMapping("/cleanup")
    public ApiResponse<Integer> cleanup(@RequestParam(defaultValue = "30") int retentionDays) {
        int deleted = taskLogService.cleanup(retentionDays);
        log.info("[ArchiveTaskLogController] cleanup deleted {} records, retentionDays:{}", deleted, retentionDays);
        return ApiResponse.success(deleted);
    }
}
```

- [ ] **Step 4: 创建 ArchiveLogCleanupTask**

```java
package com.openquartz.easyarchive.starter.task;

import com.openquartz.easyarchive.core.property.ArchiveConfig;
import com.openquartz.easyarchive.starter.service.ArchiveTaskLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveLogCleanupTask {

    private final ArchiveTaskLogService taskLogService;
    private final ArchiveConfig archiveConfig;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanup() {
        int retentionDays = archiveConfig.getLogRetentionDays();
        int deleted = taskLogService.cleanup(retentionDays);
        log.info("[ArchiveLogCleanup] cleaned {} log records older than {} days",
                deleted, retentionDays);
    }
}
```

- [ ] **Step 5: 更新 SecurityConfig 白名单**

在 `SecurityConfig.java` 的 `filterChain` 方法中，在 `.antMatchers("/actuator/health", "/actuator/info").permitAll()` 之后添加：

```java
                .antMatchers("/api/v1/task-log/**").permitAll()
```

- [ ] **Step 6: 全量构建**

Run: `mvn clean install -DskipTests -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveTaskLogService.java \
       easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveTaskLogServiceImpl.java \
       easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveTaskLogController.java \
       easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/task/ArchiveLogCleanupTask.java \
       easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/config/SecurityConfig.java
git commit -m "feat: add task log REST API, query service, and cleanup scheduler"
```

---

## 完成检查清单

- [ ] V2 迁移脚本创建完成（Task 1）
- [ ] 事件模型编译通过（Task 2）
- [ ] Publisher/Listener 接口及实现编译通过（Task 3）
- [ ] 仓储接口、ArchiveTaskLog 实体修复、配置扩展完成（Task 4）
- [ ] MyBatis Mapper + XML + JDBC Repository 编译通过（Task 5）
- [ ] DbArchiveLogListener + AutoConfiguration + @EnableScheduling 装配正确（Task 6）
- [ ] ArchiveGroupExecutor 和 ArchiveExecutor 集成事件发布（Task 7）
- [ ] REST API + Service + Cleanup + SecurityConfig 全部编译通过（Task 8）
- [ ] 全量构建成功：`mvn clean install -DskipTests`
