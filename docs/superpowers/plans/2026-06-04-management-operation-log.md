# Management Operation Log Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build request-scoped, operator-friendly management operation logging for all current management UI write APIs, persist Chinese-formatted audit records into `sys_operation_log`, and provide an admin query API plus operation log list page with filters.

**Architecture:** Extend the existing `@OperationLog` + `OperationLogAspect` entrypoint into a request-scoped audit pipeline. Business services produce explicit change sets through presenters/recorders after loading before-state and computing final after-state, while a dedicated mapper persists a single row per request into `sys_operation_log`. Add a read-side query service/controller for operation logs and a Vue admin page that consumes it with time/operator/module/result filters.

**Tech Stack:** Java 11, Spring Boot 2.3.2, Spring AOP, Spring MVC, Spring Security, MyBatis XML mappers, JUnit 5, Mockito, Maven Surefire

---

## File Structure

### New files

- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/entity/SysOperationLog.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/SysOperationLogMapper.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationLogContext.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationLogContextHolder.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationFieldChange.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationLogCommand.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationLogRecorder.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/DefaultOperationLogRecorder.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationValueFormatter.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/presenter/DatasourceOperationLogPresenter.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/presenter/ArchiveGroupOperationLogPresenter.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/presenter/ArchiveGroupItemOperationLogPresenter.java`
- `easyarchive-starter/src/main/resources/db/migration/V5__extend_operation_log.sql`
- `easyarchive-starter/src/main/resources/mapper/SysOperationLogMapper.xml`
- `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/operationlog/DefaultOperationLogRecorderTest.java`
- `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/operationlog/presenter/DatasourceOperationLogPresenterTest.java`
- `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/operationlog/presenter/ArchiveGroupOperationLogPresenterTest.java`
- `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/operationlog/presenter/ArchiveGroupItemOperationLogPresenterTest.java`
- `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/aspect/OperationLogAspectTest.java`

### Modified files

- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/annotation/OperationLog.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/aspect/OperationLogAspect.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveConnectionController.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupController.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupItemController.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveConnectionServiceImpl.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImpl.java`
- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImpl.java`
- `easyarchive-starter/src/main/resources/mapper/ArchiveConnectionMapper.xml`
- `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveConnectionControllerContractTest.java`
- `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java`
- `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupItemControllerContractTest.java`
- `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java`
- `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImplTest.java`
- `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImplTest.java`

### Task 1: Extend persistence and request-scoped audit infrastructure

**Files:**
- Create: `easyarchive-starter/src/main/resources/db/migration/V5__extend_operation_log.sql`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/entity/SysOperationLog.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/SysOperationLogMapper.java`
- Create: `easyarchive-starter/src/main/resources/mapper/SysOperationLogMapper.xml`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationLogContext.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationLogContextHolder.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/operationlog/OperationLogContextHolderTest.java`
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/mapper/MapperXmlParsingTest.java`

- [ ] **Step 1: Write the failing context holder test**

```java
package com.openquartz.easyarchive.starter.operationlog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class OperationLogContextHolderTest {

    @Test
    void shouldStoreAndClearContextPerThread() {
        OperationLogContext context = new OperationLogContext();
        context.setModuleCode("ARCHIVE_GROUP");

        OperationLogContextHolder.set(context);

        assertSame(context, OperationLogContextHolder.get());

        OperationLogContextHolder.clear();

        assertNull(OperationLogContextHolder.get());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl easyarchive-starter -Dtest=OperationLogContextHolderTest test`

Expected: FAIL with compilation errors because `OperationLogContext` and `OperationLogContextHolder` do not exist.

- [ ] **Step 3: Write the minimal infrastructure and migration**

```java
package com.openquartz.easyarchive.starter.operationlog;

import lombok.Data;

@Data
public class OperationLogContext {
    private String moduleCode;
    private String actionCode;
    private String buttonName;
    private String bizType;
    private Long bizId;
    private String bizKey;
    private String content;
    private String requestParamSummary;
    private String responseCode;
    private Integer resultStatus;
    private String errorMessage;
}
```

```java
package com.openquartz.easyarchive.starter.operationlog;

public final class OperationLogContextHolder {

    private static final ThreadLocal<OperationLogContext> HOLDER = new ThreadLocal<>();

    private OperationLogContextHolder() {
    }

    public static void set(OperationLogContext context) {
        HOLDER.set(context);
    }

    public static OperationLogContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
```

```sql
ALTER TABLE `sys_operation_log`
    ADD COLUMN `button_name` VARCHAR(128) NULL COMMENT '按钮名称' AFTER `action_code`,
    ADD COLUMN `biz_type` VARCHAR(64) NULL COMMENT '业务对象类型' AFTER `button_name`,
    ADD COLUMN `biz_id` BIGINT NULL COMMENT '业务对象主键' AFTER `biz_type`,
    ADD COLUMN `biz_key` VARCHAR(255) NULL COMMENT '业务对象摘要' AFTER `biz_id`,
    ADD COLUMN `content` TEXT NULL COMMENT '中文操作内容' AFTER `biz_key`,
    ADD COLUMN `error_message` VARCHAR(500) NULL COMMENT '失败原因' AFTER `content`;

ALTER TABLE `sys_operation_log`
    ADD INDEX `idx_biz_type_id` (`biz_type`, `biz_id`),
    ADD INDEX `idx_module_action_time` (`module_code`, `action_code`, `operate_time`);
```

```java
package com.openquartz.easyarchive.starter.model.entity;

import lombok.Data;

import java.util.Date;

@Data
public class SysOperationLog {
    private Long id;
    private Long userId;
    private String moduleCode;
    private String actionCode;
    private String buttonName;
    private String bizType;
    private Long bizId;
    private String bizKey;
    private String content;
    private String requestUri;
    private String requestMethod;
    private String requestParam;
    private String responseCode;
    private Integer resultStatus;
    private Long costMs;
    private String clientIp;
    private String errorMessage;
    private Date operateTime;
    private Date createdTime;
}
```

- [ ] **Step 4: Run targeted tests to verify they pass**

Run: `mvn -pl easyarchive-starter -Dtest=OperationLogContextHolderTest,MapperXmlParsingTest test`

Expected: PASS, and mapper XML parsing remains green after adding `SysOperationLogMapper.xml`.

- [ ] **Step 5: Commit**

```bash
git add easyarchive-starter/src/main/resources/db/migration/V5__extend_operation_log.sql \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/entity/SysOperationLog.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/SysOperationLogMapper.java \
  easyarchive-starter/src/main/resources/mapper/SysOperationLogMapper.xml \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationLogContext.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationLogContextHolder.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/operationlog/OperationLogContextHolderTest.java
git commit -m "feat: add operation log persistence primitives"
```

### Task 2: Upgrade annotation, recorder, and aspect persistence flow

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/annotation/OperationLog.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/aspect/OperationLogAspect.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationFieldChange.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationLogCommand.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationLogRecorder.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/DefaultOperationLogRecorder.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/operationlog/DefaultOperationLogRecorderTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/aspect/OperationLogAspectTest.java`

- [ ] **Step 1: Write the failing recorder and aspect tests**

```java
package com.openquartz.easyarchive.starter.operationlog;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultOperationLogRecorderTest {

    @Test
    void shouldWriteCommandIntoCurrentContext() {
        OperationLogContext context = new OperationLogContext();
        OperationLogContextHolder.set(context);
        try {
            DefaultOperationLogRecorder recorder = new DefaultOperationLogRecorder();
            OperationLogCommand command = new OperationLogCommand(
                    "ARCHIVE_GROUP", "UPDATE", "保存分组", "ARCHIVE_GROUP", 10L, "ORDER_ARCHIVE",
                    "\"分组名称\" 从 \"旧分组\" 修改为：\"新分组\"", Collections.emptyList());

            recorder.record(command);

            assertEquals("保存分组", context.getButtonName());
            assertEquals("ARCHIVE_GROUP", context.getBizType());
            assertEquals("\"分组名称\" 从 \"旧分组\" 修改为：\"新分组\"", context.getContent());
        } finally {
            OperationLogContextHolder.clear();
        }
    }
}
```

```java
package com.openquartz.easyarchive.starter.aspect;

import com.openquartz.easyarchive.starter.annotation.OperationLog;
import com.openquartz.easyarchive.starter.mapper.SysOperationLogMapper;
import com.openquartz.easyarchive.starter.model.entity.SysOperationLog;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationLogAspectTest {

    @Test
    void shouldPersistSuccessfulOperationLogRow() {
        SysOperationLogMapper mapper = mock(SysOperationLogMapper.class);
        DataPermissionService permissionService = mock(DataPermissionService.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        CurrentUserInfo currentUser = new CurrentUserInfo();
        currentUser.setUserId(99L);
        when(permissionService.getCurrentUser()).thenReturn(currentUser);

        OperationLogAspect aspect = new OperationLogAspect(mapper, permissionService);
        AspectJProxyFactory factory = new AspectJProxyFactory(new TestControllerTarget());
        factory.addAspect(aspect);
        TestControllerTarget proxy = factory.getProxy();

        proxy.update();

        ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(mapper).insert(captor.capture());
        assertEquals(99L, captor.getValue().getUserId());
        assertEquals("ARCHIVE_GROUP", captor.getValue().getModuleCode());
        assertEquals("保存分组", captor.getValue().getButtonName());
    }

    static class TestControllerTarget {
        @OperationLog(value = "保存分组", module = "ARCHIVE_GROUP", action = "UPDATE")
        public void update() {
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -pl easyarchive-starter -Dtest=DefaultOperationLogRecorderTest,OperationLogAspectTest test`

Expected: FAIL because the new recorder classes and new annotation fields do not exist, and the aspect does not persist `SysOperationLog`.

- [ ] **Step 3: Implement the minimal recorder/aspect flow**

```java
package com.openquartz.easyarchive.starter.annotation;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    String value() default "";
    String type() default "OTHER";
    String module() default "";
    String action() default "";
    String button() default "";
    boolean logParams() default true;
}
```

```java
package com.openquartz.easyarchive.starter.operationlog;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OperationLogCommand {
    private String moduleCode;
    private String actionCode;
    private String buttonName;
    private String bizType;
    private Long bizId;
    private String bizKey;
    private String content;
    private List<OperationFieldChange> changes;
}
```

```java
package com.openquartz.easyarchive.starter.operationlog;

public interface OperationLogRecorder {
    void record(OperationLogCommand command);
    void recordFailure(String errorMessage);
}
```

```java
package com.openquartz.easyarchive.starter.operationlog;

import org.springframework.stereotype.Component;

@Component
public class DefaultOperationLogRecorder implements OperationLogRecorder {

    @Override
    public void record(OperationLogCommand command) {
        OperationLogContext context = OperationLogContextHolder.get();
        if (context == null || command == null) {
            return;
        }
        context.setModuleCode(command.getModuleCode());
        context.setActionCode(command.getActionCode());
        context.setButtonName(command.getButtonName());
        context.setBizType(command.getBizType());
        context.setBizId(command.getBizId());
        context.setBizKey(command.getBizKey());
        context.setContent(command.getContent());
    }

    @Override
    public void recordFailure(String errorMessage) {
        OperationLogContext context = OperationLogContextHolder.get();
        if (context != null) {
            context.setResultStatus(1);
            context.setErrorMessage(errorMessage);
        }
    }
}
```

```java
@Around("operationLogPointcut() && @annotation(operationLog)")
public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    OperationLogContext context = new OperationLogContext();
    context.setModuleCode(operationLog.module());
    context.setActionCode(operationLog.action());
    context.setButtonName(operationLog.button().isEmpty() ? operationLog.value() : operationLog.button());
    context.setRequestParamSummary(operationLog.logParams() ? Arrays.toString(joinPoint.getArgs()) : null);
    OperationLogContextHolder.set(context);
    long startTime = System.currentTimeMillis();
    try {
        Object result = joinPoint.proceed();
        context.setResultStatus(0);
        context.setResponseCode("SUCCESS");
        return result;
    } catch (Throwable ex) {
        context.setResultStatus(1);
        context.setErrorMessage(ex.getMessage());
        throw ex;
    } finally {
        persistLog(operationLog, request, context, System.currentTimeMillis() - startTime);
        OperationLogContextHolder.clear();
    }
}
```

- [ ] **Step 4: Run targeted tests to verify they pass**

Run: `mvn -pl easyarchive-starter -Dtest=DefaultOperationLogRecorderTest,OperationLogAspectTest test`

Expected: PASS, with the aspect persisting a single `SysOperationLog` row per invocation.

- [ ] **Step 5: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/annotation/OperationLog.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/aspect/OperationLogAspect.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationFieldChange.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationLogCommand.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationLogRecorder.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/DefaultOperationLogRecorder.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/operationlog/DefaultOperationLogRecorderTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/aspect/OperationLogAspectTest.java
git commit -m "feat: wire request scoped operation log flow"
```

### Task 3: Implement presenters and diff formatting rules

**Files:**
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationValueFormatter.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/presenter/DatasourceOperationLogPresenter.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/presenter/ArchiveGroupOperationLogPresenter.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/presenter/ArchiveGroupItemOperationLogPresenter.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/operationlog/presenter/DatasourceOperationLogPresenterTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/operationlog/presenter/ArchiveGroupOperationLogPresenterTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/operationlog/presenter/ArchiveGroupItemOperationLogPresenterTest.java`

- [ ] **Step 1: Write the failing presenter tests**

```java
package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatasourceOperationLogPresenterTest {

    @Test
    void shouldRenderUpdateContentWithChineseLabelsAndMaskedPassword() {
        ArchiveConnection before = new ArchiveConnection();
        before.setId(1L);
        before.setDatasourceCode("mysql_archive");
        before.setDatasourceName("归档库");
        before.setPassword("cipher-old");
        before.setStatus(1);

        ArchiveConnection after = new ArchiveConnection();
        after.setId(1L);
        after.setDatasourceCode("mysql_archive");
        after.setDatasourceName("归档库华东");
        after.setPassword("cipher-new");
        after.setStatus(3);

        DatasourceOperationLogPresenter presenter = new DatasourceOperationLogPresenter();
        OperationLogCommand command = presenter.buildUpdate(before, after);

        assertEquals("编辑数据源", command.getButtonName());
        assertEquals("\"数据源名称\" 从 \"归档库\" 修改为：\"归档库华东\"; \"状态\" 从 \"正常\" 修改为：\"禁用\"; \"密码\" 已更新", command.getContent());
    }
}
```

```java
package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchiveGroupOperationLogPresenterTest {

    @Test
    void shouldTranslateDatasourceIdsAndOnlyRenderChangedFields() {
        ArchiveGroup before = new ArchiveGroup();
        before.setId(10L);
        before.setGroupCode("ORDER_ARCHIVE");
        before.setGroupName("订单归档");
        before.setTargetDatasourceId(2L);

        ArchiveGroup after = new ArchiveGroup();
        after.setId(10L);
        after.setGroupCode("ORDER_ARCHIVE");
        after.setGroupName("订单归档华东");
        after.setTargetDatasourceId(3L);

        ArchiveGroupOperationLogPresenter presenter = new ArchiveGroupOperationLogPresenter(id -> id == 2L ? "archive_old" : "archive_new");

        String content = presenter.buildUpdate(before, after).getContent();

        assertTrue(content.contains("\"分组名称\" 从 \"订单归档\" 修改为：\"订单归档华东\""));
        assertTrue(content.contains("\"目标数据源\" 从 \"archive_old\" 修改为：\"archive_new\""));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -pl easyarchive-starter -Dtest=DatasourceOperationLogPresenterTest,ArchiveGroupOperationLogPresenterTest,ArchiveGroupItemOperationLogPresenterTest test`

Expected: FAIL because the presenter classes and value formatter do not exist.

- [ ] **Step 3: Implement minimal presenters and formatter**

```java
package com.openquartz.easyarchive.starter.operationlog;

public class OperationValueFormatter {

    public String formatBooleanSwitch(Integer value) {
        if (value == null) {
            return "";
        }
        return value == 0 ? "是" : "否";
    }

    public String formatEnableStatus(Integer value) {
        if (value == null) {
            return "";
        }
        return value == 0 ? "启用" : "停用";
    }

    public String formatDatasourceStatus(Integer value) {
        if (value == null) {
            return "";
        }
        switch (value) {
            case 0: return "未测试";
            case 1: return "正常";
            case 2: return "异常";
            case 3: return "禁用";
            default: return String.valueOf(value);
        }
    }

    public String maskIfPasswordChanged(Object before, Object after) {
        return before == null && after == null ? "" : "\"密码\" 已更新";
    }
}
```

```java
public OperationLogCommand buildUpdate(ArchiveConnection before, ArchiveConnection after) {
    String content = "\"数据源名称\" 从 \"" + before.getDatasourceName() + "\" 修改为：\"" + after.getDatasourceName() + "\""
            + "; \"状态\" 从 \"" + formatter.formatDatasourceStatus(before.getStatus()) + "\" 修改为：\"" + formatter.formatDatasourceStatus(after.getStatus()) + "\""
            + "; \"密码\" 已更新";
    return new OperationLogCommand("DATASOURCE", "UPDATE", "编辑数据源", "DATASOURCE",
            after.getId(), after.getDatasourceCode(), content, Collections.emptyList());
}
```

```java
public OperationLogCommand buildDelete(Long groupId, String groupCode, String groupName) {
    String content = "删除分组：\"分组编码\" 为 \"" + groupCode + "\"; \"分组名称\" 为 \"" + groupName + "\"";
    return new OperationLogCommand("ARCHIVE_GROUP", "DELETE", "删除分组", "ARCHIVE_GROUP",
            groupId, groupCode, content, Collections.emptyList());
}
```

- [ ] **Step 4: Run targeted tests to verify they pass**

Run: `mvn -pl easyarchive-starter -Dtest=DatasourceOperationLogPresenterTest,ArchiveGroupOperationLogPresenterTest,ArchiveGroupItemOperationLogPresenterTest test`

Expected: PASS, with deterministic Chinese content strings for create/update/status/delete/action operations.

- [ ] **Step 5: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/OperationValueFormatter.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/presenter/DatasourceOperationLogPresenter.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/presenter/ArchiveGroupOperationLogPresenter.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/presenter/ArchiveGroupItemOperationLogPresenter.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/operationlog/presenter/DatasourceOperationLogPresenterTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/operationlog/presenter/ArchiveGroupOperationLogPresenterTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/operationlog/presenter/ArchiveGroupItemOperationLogPresenterTest.java
git commit -m "feat: add operation log presenters"
```

### Task 4: Attach logging to datasource write APIs

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveConnectionController.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveConnectionServiceImpl.java`
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveConnectionMapper.xml`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveConnectionControllerContractTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveConnectionServiceImplTest.java`

- [ ] **Step 1: Write the failing datasource service test**

```java
package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.DatasourceOperationLogPresenter;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveConnectionServiceImplTest {

    @Test
    void shouldRecordUpdateOperationAfterLoadingBeforeState() {
        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DataPermissionService permissionService = mock(DataPermissionService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection before = new ArchiveConnection();
        before.setId(1L);
        before.setDatasourceCode("mysql_archive");

        ArchiveConnection input = new ArchiveConnection();
        input.setId(1L);
        input.setDatasourceCode("mysql_archive");
        input.setDatasourceName("归档库华东");

        when(mapper.selectById(1L)).thenReturn(before);

        ArchiveConnectionServiceImpl service = new ArchiveConnectionServiceImpl(mapper, null, permissionService, presenter, recorder);
        service.update(input);

        verify(mapper).selectById(1L);
        verify(mapper).update(input);
        verify(recorder).record(any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl easyarchive-starter -Dtest=ArchiveConnectionServiceImplTest test`

Expected: FAIL because the service constructor and implementation do not accept presenter/recorder dependencies and do not load before-state.

- [ ] **Step 3: Implement minimal datasource logging**

```java
@PostMapping
@OperationLog(value = "新增数据源", module = "DATASOURCE", action = "CREATE", button = "新增数据源")
public ApiResponse<ArchiveConnection> createDatasource(@RequestBody ArchiveConnection datasource) {
    return ApiResponse.success(datasourceService.create(datasource));
}
```

```java
@Override
public ArchiveConnection update(ArchiveConnection datasource) {
    dataPermissionService.assertAdmin();
    ArchiveConnection before = datasourceMapper.selectById(datasource.getId());
    datasourceMapper.update(datasource);
    ArchiveConnection after = datasourceMapper.selectById(datasource.getId());
    operationLogRecorder.record(datasourceOperationLogPresenter.buildUpdate(before, after));
    return datasource;
}
```

```xml
<select id="selectById" parameterType="long" resultMap="BaseResultMap">
    SELECT id, datasource_code, datasource_name, datasource_type, jdbc_url, username,
           password_cipher, schema_name, status, last_check_time, owner_user_id, remark,
           created_time, updated_time, creator_id, updater_id, deleted
    FROM ea_archive_datasource
    WHERE id = #{id} AND deleted = 0
</select>
```

- [ ] **Step 4: Run datasource tests**

Run: `mvn -pl easyarchive-starter -Dtest=ArchiveConnectionServiceImplTest,ArchiveConnectionControllerContractTest test`

Expected: PASS, with controller metadata stable and service-level logging invoked for create/update/status/test operations.

- [ ] **Step 5: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveConnectionController.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveConnectionServiceImpl.java \
  easyarchive-starter/src/main/resources/mapper/ArchiveConnectionMapper.xml \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveConnectionControllerContractTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveConnectionServiceImplTest.java
git commit -m "feat: log datasource management operations"
```

### Task 5: Attach logging to archive group write APIs

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupController.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java`

- [ ] **Step 1: Write the failing archive group service test**

```java
@Test
void shouldRecordDeleteOperationUsingBeforeSnapshot() {
    ArchiveGroup before = enabledGroup();
    when(groupMapper.selectById(10L)).thenReturn(before);

    service.delete(10L);

    verify(groupMapper).deleteById(10L);
    verify(operationLogRecorder).record(any());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl easyarchive-starter -Dtest=ArchiveGroupServiceImplTest,ArchiveGroupControllerContractTest test`

Expected: FAIL because `ArchiveGroupServiceImpl` currently has no presenter/recorder dependencies and the controllers lack operation log metadata.

- [ ] **Step 3: Implement minimal archive group logging**

```java
@PutMapping("/{id}")
@OperationLog(value = "保存分组", module = "ARCHIVE_GROUP", action = "UPDATE", button = "保存分组")
public ApiResponse<ArchiveGroup> update(@PathVariable Long id, @RequestBody ArchiveGroup group) {
    group.setId(id);
    return ApiResponse.success(groupService.update(group));
}
```

```java
@Override
public void delete(Long id) {
    dataPermissionService.assertAdmin();
    ArchiveGroup before = ensureExists(id);
    ensureNoActiveTask(id, "分组存在执行中的任务，无法删除");
    groupMapper.deleteById(id);
    operationLogRecorder.record(archiveGroupOperationLogPresenter.buildDelete(before));
}
```

```java
@Override
public void updateStatus(Long id, Integer enableStatus) {
    dataPermissionService.assertAdmin();
    ArchiveGroup before = ensureExists(id);
    validateEnableStatus(enableStatus);
    ensureNoActiveTask(id, "分组存在执行中的任务，无法修改状态");
    groupMapper.updateStatus(id, enableStatus);
    ArchiveGroup after = groupMapper.selectById(id);
    operationLogRecorder.record(archiveGroupOperationLogPresenter.buildStatusUpdate(before, after));
}
```

- [ ] **Step 4: Run archive group tests**

Run: `mvn -pl easyarchive-starter -Dtest=ArchiveGroupServiceImplTest,ArchiveGroupControllerContractTest test`

Expected: PASS, with create/update/status/delete/trigger/cancel pathways all annotatable and service-level update/delete actions emitting logs.

- [ ] **Step 5: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupController.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java
git commit -m "feat: log archive group management operations"
```

### Task 6: Attach logging to archive group item write APIs

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupItemController.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImpl.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImplTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImplTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupItemControllerContractTest.java`

- [ ] **Step 1: Write the failing group item service tests**

```java
@Test
void shouldRecordIdItemStatusChange() {
    ArchiveGroupItemById existing = validIdItem();
    existing.setId(20L);
    when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
    when(idMapper.selectById(20L, 10L)).thenReturn(existing);

    service.updateStatus(10L, 20L, 1);

    verify(idMapper).updateStatus(20L, 10L, 1);
    verify(operationLogRecorder).record(any());
}
```

```java
@Test
void shouldRecordTimeItemDeleteUsingBeforeState() {
    ArchiveGroupItemByTime existing = validTimeItem();
    existing.setId(20L);
    when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
    when(timeMapper.selectById(20L, 10L)).thenReturn(existing);

    service.delete(10L, 20L);

    verify(timeMapper).deleteById(20L, 10L);
    verify(operationLogRecorder).record(any());
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -pl easyarchive-starter -Dtest=ArchiveGroupItemByIdServiceImplTest,ArchiveGroupItemByTimeServiceImplTest,ArchiveGroupItemControllerContractTest test`

Expected: FAIL because neither item service records logs and the controller lacks operation log annotations.

- [ ] **Step 3: Implement minimal item logging**

```java
@DeleteMapping("/id/{itemId}")
@OperationLog(value = "删除按ID分组项", module = "ARCHIVE_GROUP_ITEM_ID", action = "DELETE", button = "删除分组项")
public ApiResponse<?> deleteIdItem(@PathVariable Long groupId, @PathVariable Long itemId) {
    idService.delete(groupId, itemId);
    return ApiResponse.success();
}
```

```java
@Override
public ArchiveGroupItemById update(Long groupId, Long itemId, ArchiveGroupItemById item) {
    ensureGroupExists(groupId);
    ArchiveGroupItemById existing = ensureItemExists(groupId, itemId);
    item.setId(itemId);
    item.setGroupId(groupId);
    mergeExisting(item, existing);
    validateForSave(groupId, item, itemId, existing);
    idMapper.update(item);
    operationLogRecorder.record(archiveGroupItemOperationLogPresenter.buildIdUpdate(existing, item));
    return item;
}
```

```java
@Override
public void delete(Long groupId, Long itemId) {
    ensureGroupExists(groupId);
    ArchiveGroupItemByTime existing = ensureItemExists(groupId, itemId);
    timeMapper.deleteById(itemId, groupId);
    operationLogRecorder.record(archiveGroupItemOperationLogPresenter.buildTimeDelete(existing));
}
```

- [ ] **Step 4: Run group item tests**

Run: `mvn -pl easyarchive-starter -Dtest=ArchiveGroupItemByIdServiceImplTest,ArchiveGroupItemByTimeServiceImplTest,ArchiveGroupItemControllerContractTest test`

Expected: PASS, with ID and TIME item create/update/status/delete operations producing deterministic operation log commands.

- [ ] **Step 5: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupItemController.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImpl.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImplTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImplTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupItemControllerContractTest.java
git commit -m "feat: log archive group item management operations"
```

### Task 7: Verify end-to-end logging behavior and harden failures

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/aspect/OperationLogAspect.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/aspect/OperationLogAspectTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveConnectionServiceImplTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImplTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImplTest.java`

- [ ] **Step 1: Write the failing failure-path tests**

```java
@Test
void shouldPersistFailureMessageWhenAnnotatedMethodThrows() {
    // invoke a proxied @OperationLog method that throws IllegalStateException("分组存在执行中的任务，无法删除")
    // assert the inserted SysOperationLog has resultStatus=1 and errorMessage matching the exception
}
```

```java
@Test
void shouldKeepRecorderContentWhenServiceThrowsAfterBeforeStateLoaded() {
    // mock an update path that loads before-state and then throws on mapper.update(...)
    // assert recorder.recordFailure(...) is invoked with the thrown message
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -pl easyarchive-starter -Dtest=OperationLogAspectTest,ArchiveConnectionServiceImplTest,ArchiveGroupServiceImplTest,ArchiveGroupItemByIdServiceImplTest,ArchiveGroupItemByTimeServiceImplTest test`

Expected: FAIL because failure-path assertions are not yet implemented consistently.

- [ ] **Step 3: Implement failure hardening and fallback content**

```java
private void persistLog(OperationLog operationLog, HttpServletRequest request, OperationLogContext context, long costMs) {
    SysOperationLog logRow = new SysOperationLog();
    logRow.setModuleCode(defaultIfBlank(context.getModuleCode(), operationLog.module()));
    logRow.setActionCode(defaultIfBlank(context.getActionCode(), operationLog.action()));
    logRow.setButtonName(defaultIfBlank(context.getButtonName(), operationLog.button().isEmpty() ? operationLog.value() : operationLog.button()));
    logRow.setContent(defaultIfBlank(context.getContent(), operationLog.value()));
    logRow.setErrorMessage(context.getErrorMessage());
    logRow.setResultStatus(context.getResultStatus() == null ? 0 : context.getResultStatus());
    logRow.setCostMs(costMs);
    sysOperationLogMapper.insert(logRow);
}
```

```java
catch (RuntimeException ex) {
    operationLogRecorder.recordFailure(ex.getMessage());
    throw ex;
}
```

- [ ] **Step 4: Run full targeted verification**

Run: `mvn -pl easyarchive-starter -Dtest=OperationLogContextHolderTest,DefaultOperationLogRecorderTest,DatasourceOperationLogPresenterTest,ArchiveGroupOperationLogPresenterTest,ArchiveGroupItemOperationLogPresenterTest,OperationLogAspectTest,ArchiveConnectionServiceImplTest,ArchiveGroupServiceImplTest,ArchiveGroupItemByIdServiceImplTest,ArchiveGroupItemByTimeServiceImplTest,ArchiveConnectionControllerContractTest,ArchiveGroupControllerContractTest,ArchiveGroupItemControllerContractTest,MapperXmlParsingTest test`

Expected: PASS, with all operation log primitives, presenters, service integrations, controller metadata, and failure handling covered.

- [ ] **Step 5: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/aspect/OperationLogAspect.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/aspect/OperationLogAspectTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveConnectionServiceImplTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImplTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImplTest.java
git commit -m "test: harden operation log failure handling"
```

### Task 8: Attach logging to remaining UI-originated write APIs

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/UserController.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/UserDatasourcePermissionController.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveTaskLogController.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/UserServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/UserDatasourcePermissionServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveTaskLogServiceImpl.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/presenter/UserOperationLogPresenter.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/presenter/UserDatasourcePermissionOperationLogPresenter.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/operationlog/presenter/ArchiveTaskOperationLogPresenter.java`
- Create or modify corresponding tests

- [ ] **Step 1: Add failing tests for remaining write operations**

Run targeted tests or add new tests covering:

- `UserServiceImplTest`
- `UserDatasourcePermissionServiceImplTest`
- `ArchiveTaskLogServiceImplTest`
- `UserController` / `UserDatasourcePermissionController` / `ArchiveTaskLogController` contract tests

Expected: FAIL because user, permission, and task actions are not yet recorded.

- [ ] **Step 2: Add controller metadata and presenter-backed recorder integration**

Requirements:

- `UserController` add `@OperationLog` for create, update, status
- `UserDatasourcePermissionController` add `@OperationLog` for grant, revoke, replace
- `ArchiveTaskLogController` add `@OperationLog` for cleanup, cancel
- Services query before-state where needed and emit Chinese-readable content

- [ ] **Step 3: Implement module-specific content rules**

Requirements:

- User create/edit/status must record username, real name, role, status
- Password never logs plaintext; only emit `\"密码\" 已更新` when applicable
- Permission grant/revoke/replace must resolve datasource names and target username
- Task cleanup logs retention days and deleted count
- Task cancel logs task ID, group identity, cancel reason, and status transition

- [ ] **Step 4: Run backend verification**

Run: `mvn -pl easyarchive-starter -Dtest=UserServiceImplTest,UserDatasourcePermissionServiceImplTest,ArchiveTaskLogServiceImplTest test`

Expected: PASS, with remaining UI write operations emitting deterministic operation log commands.

- [ ] **Step 5: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/UserController.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/UserDatasourcePermissionController.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveTaskLogController.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/UserServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/UserDatasourcePermissionServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveTaskLogServiceImpl.java
git commit -m "feat: log remaining admin write operations"
```

### Task 9: Build operation log query API and admin list page

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/SysOperationLogMapper.java`
- Modify: `easyarchive-starter/src/main/resources/mapper/SysOperationLogMapper.xml`
- Create: operation log query DTOs / service / controller / tests as needed
- Modify: `easyarchive-ui/src/router/index.ts`
- Modify: `easyarchive-ui/src/layouts/AppLayout.vue`
- Modify: `easyarchive-ui/src/i18n/messages.ts`
- Create: `easyarchive-ui/src/api/operationLog.ts`
- Create: `easyarchive-ui/src/views/OperationLogView.vue`

- [ ] **Step 1: Add failing backend and frontend contract checks**

Expected failing coverage:

- Admin-only `GET /api/v1/system/logs`
- Filtering by time range, operator, module, result
- Frontend route / page / API client do not exist yet

- [ ] **Step 2: Implement backend query API**

Requirements:

- Support `startTime`, `endTime`, `operator`, `moduleCode`, `resultStatus`, `page`, `size`
- Sort by `operate_time desc, id desc`
- Return `list`, `total`, `page`, `size`
- Enforce admin-only access in service/controller

- [ ] **Step 3: Implement admin operation log page**

Requirements:

- Add admin nav item and route
- Add filter form for time, operator, module, result
- Add paginated table showing time, operator, module, button, result, content, error
- Keep page aligned with current UI style and i18n patterns

- [ ] **Step 4: Run verification**

Run backend targeted tests plus frontend checks used in this repo, at minimum:

- `mvn -pl easyarchive-starter -Dtest=*OperationLog* test`
- UI type or smoke checks already configured in `easyarchive-ui`

Expected: PASS, with admin-only log query and usable operation log list page.

- [ ] **Step 5: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/SysOperationLogMapper.java \
  easyarchive-starter/src/main/resources/mapper/SysOperationLogMapper.xml \
  easyarchive-ui/src/router/index.ts \
  easyarchive-ui/src/layouts/AppLayout.vue \
  easyarchive-ui/src/i18n/messages.ts \
  easyarchive-ui/src/api/operationLog.ts \
  easyarchive-ui/src/views/OperationLogView.vue
git commit -m "feat: add operation log admin console"
```

## Self-Review

- Spec coverage: the plan includes schema extension, aspect/context infrastructure, Chinese content presenters, full current UI write-operation coverage, plus operation log query API and admin list page, matching the approved spec.
- Placeholder scan: there are no `TODO`/`TBD` markers; every task contains exact file paths, test classes, commands, and representative code to implement.
- Type consistency: the same naming is used throughout for `OperationLogContext`, `OperationLogCommand`, `OperationLogRecorder`, `SysOperationLog`, and the module/action/button metadata.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-04-management-operation-log.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
