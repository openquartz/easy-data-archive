# Starter Management Transactions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add consistent `@Transactional(rollbackFor = Exception.class)` coverage to `easyarchive-starter` management write service methods and lock that coverage with tests.

**Architecture:** Keep transaction boundaries at public service entrypoints in `service/impl`, using method-level annotations instead of class-level annotations. Verify the contract with a focused reflection-based unit test so the refactor stays lightweight and does not require new Spring integration wiring.

**Tech Stack:** Java 11, Spring Boot 2.3.2, Spring Transaction, JUnit 5, Maven

---

### Task 1: Add failing transaction coverage test

**Files:**
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ManagementTransactionAnnotationTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.openquartz.easyarchive.starter.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ManagementTransactionAnnotationTest {

    @Test
    void shouldAnnotateAllManagementWriteMethodsWithTransactionalRollbackForException() throws Exception {
        assertTransactional(ArchiveConnectionServiceImpl.class, "create", com.openquartz.easyarchive.core.connection.entity.ArchiveConnection.class);
        assertTransactional(ArchiveConnectionServiceImpl.class, "update", com.openquartz.easyarchive.core.connection.entity.ArchiveConnection.class);
        assertTransactional(ArchiveConnectionServiceImpl.class, "updateStatus", Long.class, Integer.class);
        assertTransactional(ArchiveConnectionServiceImpl.class, "testConnection", com.openquartz.easyarchive.core.connection.entity.ArchiveConnection.class);

        assertTransactional(ArchiveGroupServiceImpl.class, "create", com.openquartz.easyarchive.core.rule.entity.ArchiveGroup.class);
        assertTransactional(ArchiveGroupServiceImpl.class, "update", com.openquartz.easyarchive.core.rule.entity.ArchiveGroup.class);
        assertTransactional(ArchiveGroupServiceImpl.class, "updateStatus", Long.class, Integer.class);
        assertTransactional(ArchiveGroupServiceImpl.class, "delete", Long.class);

        assertTransactional(ArchiveGroupItemByIdServiceImpl.class, "create", Long.class, com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById.class);
        assertTransactional(ArchiveGroupItemByIdServiceImpl.class, "update", Long.class, Long.class, com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById.class);
        assertTransactional(ArchiveGroupItemByIdServiceImpl.class, "updateStatus", Long.class, Long.class, Integer.class);
        assertTransactional(ArchiveGroupItemByIdServiceImpl.class, "delete", Long.class, Long.class);

        assertTransactional(ArchiveGroupItemByTimeServiceImpl.class, "create", Long.class, com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime.class);
        assertTransactional(ArchiveGroupItemByTimeServiceImpl.class, "update", Long.class, Long.class, com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime.class);
        assertTransactional(ArchiveGroupItemByTimeServiceImpl.class, "updateStatus", Long.class, Long.class, Integer.class);
        assertTransactional(ArchiveGroupItemByTimeServiceImpl.class, "delete", Long.class, Long.class);

        assertTransactional(UserServiceImpl.class, "create", com.openquartz.easyarchive.core.common.SysUser.class);
        assertTransactional(UserServiceImpl.class, "update", com.openquartz.easyarchive.core.common.SysUser.class);
        assertTransactional(UserServiceImpl.class, "updateStatus", Long.class, Integer.class);

        assertTransactional(UserDatasourcePermissionServiceImpl.class, "grant", Long.class, Long.class);
        assertTransactional(UserDatasourcePermissionServiceImpl.class, "revoke", Long.class, Long.class);
        assertTransactional(UserDatasourcePermissionServiceImpl.class, "replacePermissions", Long.class, java.util.List.class);

        assertTransactional(ArchiveTaskLogServiceImpl.class, "cancelTask", Long.class, String.class);
        assertTransactional(ArchiveTaskLogServiceImpl.class, "cleanup", Integer.class);

        assertTransactional(ArchiveGroupExecutionServiceImpl.class, "trigger", Long.class);
        assertTransactional(ArchiveGroupExecutionServiceImpl.class, "cancelActiveTask", Long.class, String.class);
    }

    @Test
    void shouldKeepReadOnlyManagementMethodsWithoutTransactionalAnnotation() throws Exception {
        assertNotTransactional(ArchiveConnectionServiceImpl.class, "findAll");
        assertNotTransactional(ArchiveConnectionServiceImpl.class, "findById", Long.class);
        assertNotTransactional(ArchiveGroupServiceImpl.class, "findAll", Integer.class);
        assertNotTransactional(ArchiveGroupServiceImpl.class, "findById", Long.class);
        assertNotTransactional(ArchiveGroupServiceImpl.class, "findOverview", Long.class);
        assertNotTransactional(ArchiveGroupExecutionServiceImpl.class, "requireExistingGroup", Long.class);
        assertNotTransactional(UserDatasourcePermissionServiceImpl.class, "listUserPermissions", Long.class);
        assertNotTransactional(ArchiveTaskLogServiceImpl.class, "queryTasks", Integer.class, Integer.class, String.class);
    }

    private static void assertTransactional(Class<?> type, String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = type.getMethod(methodName, parameterTypes);
        Transactional annotation = method.getAnnotation(Transactional.class);
        assertNotNull(annotation, () -> type.getSimpleName() + "#" + methodName + " must be transactional");
        assertArrayEquals(new Class<?>[]{Exception.class}, annotation.rollbackFor(),
                () -> type.getSimpleName() + "#" + methodName + " must roll back for Exception");
    }

    private static void assertNotTransactional(Class<?> type, String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = type.getDeclaredMethod(methodName, parameterTypes);
        Transactional annotation = method.getAnnotation(Transactional.class);
        assertNull(annotation, () -> type.getSimpleName() + "#" + methodName + " must stay non-transactional");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl easyarchive-starter -Dtest=ManagementTransactionAnnotationTest test`
Expected: FAIL because multiple write methods do not yet have `@Transactional`

### Task 2: Add method-level transaction annotations

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveConnectionServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/UserServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/UserDatasourcePermissionServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImpl.java`

- [ ] **Step 1: Write minimal implementation**

Add `import org.springframework.transaction.annotation.Transactional;` to each file above and annotate only the management write entrypoints with:

```java
@Transactional(rollbackFor = Exception.class)
```

Apply to these exact methods:

```java
// ArchiveConnectionServiceImpl
create(ArchiveConnection datasource)
update(ArchiveConnection datasource)
updateStatus(Long id, Integer status)
testConnection(ArchiveConnection datasource)

// ArchiveGroupServiceImpl
create(ArchiveGroup group)
update(ArchiveGroup group)
updateStatus(Long id, Integer enableStatus)
delete(Long id)

// ArchiveGroupItemByIdServiceImpl
create(Long groupId, ArchiveGroupItemById item)
update(Long groupId, Long itemId, ArchiveGroupItemById item)
updateStatus(Long groupId, Long itemId, Integer enableStatus)
delete(Long groupId, Long itemId)

// ArchiveGroupItemByTimeServiceImpl
create(Long groupId, ArchiveGroupItemByTime item)
update(Long groupId, Long itemId, ArchiveGroupItemByTime item)
updateStatus(Long groupId, Long itemId, Integer enableStatus)
delete(Long groupId, Long itemId)

// UserServiceImpl
create(SysUser user)
update(SysUser user)
updateStatus(Long id, Integer status)

// UserDatasourcePermissionServiceImpl
grant(Long userId, Long datasourceId)
revoke(Long userId, Long datasourceId)

// ArchiveGroupExecutionServiceImpl
trigger(Long groupId)
cancelActiveTask(Long groupId, String cancelReason)
```

Do not add class-level `@Transactional`. Do not annotate query methods.

- [ ] **Step 2: Run test to verify it passes**

Run: `mvn -pl easyarchive-starter -Dtest=ManagementTransactionAnnotationTest test`
Expected: PASS

### Task 3: Run targeted regression tests

**Files:**
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveConnectionServiceImplTest.java`
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java`
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImplTest.java`
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImplTest.java`
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/UserServiceImplTest.java`
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/UserDatasourcePermissionServiceImplTest.java`
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveTaskLogServiceImplTest.java`
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImplTest.java`

- [ ] **Step 1: Run focused service tests**

Run:

```bash
mvn -pl easyarchive-starter \
  -Dtest=ManagementTransactionAnnotationTest,ArchiveConnectionServiceImplTest,ArchiveGroupServiceImplTest,ArchiveGroupItemByIdServiceImplTest,ArchiveGroupItemByTimeServiceImplTest,UserServiceImplTest,UserDatasourcePermissionServiceImplTest,ArchiveTaskLogServiceImplTest,ArchiveGroupExecutionServiceImplTest \
  test
```

Expected: PASS

- [ ] **Step 2: Commit**

```bash
git add \
  docs/superpowers/plans/2026-06-04-starter-management-transactions.md \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ManagementTransactionAnnotationTest.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveConnectionServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/UserServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/UserDatasourcePermissionServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImpl.java
git commit -m "refactor: add starter management transactions"
```
