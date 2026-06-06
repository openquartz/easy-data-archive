# Exception And Magic Value Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish a repository-wide exception-code and magic-value governance standard, then migrate the current high-value violations in `easyarchive-common`, `easyarchive-core`, and `easyarchive-starter` to that standard.

**Architecture:** Keep the existing module boundaries intact while tightening the shared exception foundation in `easyarchive-common`, adding module-level error-code enums and one `easyarchive-starter` business exception subtype, and migrating service-layer business failures away from raw JDK runtime exceptions. Treat domain state vocabularies as enums, technical thresholds as constants, and lock the behavior with focused unit tests plus repository scans.

**Tech Stack:** Java 11, Maven 3.9, Spring Boot 2.3.2, JUnit 5, MyBatis, Lombok, ripgrep

---

### Task 1: Baseline the current violations with failing tests and scans

**Files:**
- Create: `easyarchive-common/src/test/java/com/openquartz/easyarchive/common/exception/EasyArchiveExceptionTest.java`
- Create: `easyarchive-core/src/test/java/com/openquartz/easyarchive/core/expr/executors/RandomAlphaNumExecutorTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImplTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImplTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImplTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/UserDatasourcePermissionServiceImplTest.java`

- [ ] **Step 1: Write the failing shared exception test**

```java
package com.openquartz.easyarchive.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EasyArchiveExceptionTest {

    @Test
    void shouldCreatePlaceholderMessageWithoutThrowingFromFactoryMethod() {
        EasyArchiveException exception = EasyArchiveException.withPlaceholders(
                CommonErrorCode.CLASS_NOT_FOUND_ERROR, "demo.Type");

        assertEquals(CommonErrorCode.CLASS_NOT_FOUND_ERROR, exception.getErrorCode());
        assertEquals("Class demo.Type not exist!", exception.getMessage());
    }

    @Test
    void shouldThrowCommonErrorCodeThroughAsserts() {
        EasyArchiveException exception = assertThrows(EasyArchiveException.class,
                () -> Asserts.notNull(null, CommonErrorCode.PARAM_ILLEGAL_ERROR));

        assertEquals(CommonErrorCode.PARAM_ILLEGAL_ERROR, exception.getErrorCode());
    }
}
```

- [ ] **Step 2: Write the failing enum-driven executor test**

```java
package com.openquartz.easyarchive.core.expr.executors;

import com.openquartz.easyarchive.core.expr.cmd.CommandNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomAlphaNumExecutorTest {

    private final RandomAlphaNumExecutor executor = new RandomAlphaNumExecutor();

    @Test
    void shouldGenerateUppercaseWithoutIForTypeOneAlias() {
        CommandNode node = CommandNode.command("randomAlphaNum", "8", "UPPERCASE_NO_I");

        String value = (String) executor.execute(null, node);

        assertEquals(8, value.length());
        assertTrue(value.chars().allMatch(ch -> ch >= 'A' && ch <= 'Z'));
        assertFalse(value.contains("I"));
    }

    @Test
    void shouldRejectUnsupportedClassifierWithErrorCode() {
        CommandNode node = CommandNode.command("randomAlphaNum", "8", "BAD_TYPE");

        EasyArchiveException exception = assertThrows(EasyArchiveException.class,
                () -> executor.execute(null, node));

        assertEquals(CoreErrorCode.RANDOM_ALPHA_NUM_TYPE_UNSUPPORTED, exception.getErrorCode());
    }
}
```

- [ ] **Step 3: Update one service test to expect project exceptions instead of JDK exceptions**

```java
@Test
void shouldRejectBlankGroupCodeWithStarterErrorCode() {
    EasyArchiveException exception = assertThrows(EasyArchiveException.class, () -> service.trigger("  "));

    assertEquals(StarterErrorCode.ARCHIVE_GROUP_CODE_REQUIRED, exception.getErrorCode());
}
```

Apply the same assertion style to the existing invalid-input tests in:

- `ArchiveGroupItemByIdServiceImplTest`
- `ArchiveGroupItemByTimeServiceImplTest`
- `UserDatasourcePermissionServiceImplTest`

- [ ] **Step 4: Run the focused failing tests and baseline scan**

Run:

```bash
mvn -pl easyarchive-common,easyarchive-core,easyarchive-starter \
  -Dtest=EasyArchiveExceptionTest,RandomAlphaNumExecutorTest,ArchiveGroupExecutionServiceImplTest,ArchiveGroupItemByIdServiceImplTest,ArchiveGroupItemByTimeServiceImplTest,UserDatasourcePermissionServiceImplTest \
  test
```

Expected: FAIL because the shared exception factory does not exist yet, executor classifier strings are still magic values, and service tests still see raw `IllegalArgumentException` / `IllegalStateException`.

Run:

```bash
rg -n "throw new (IllegalArgumentException|IllegalStateException)" easyarchive-common easyarchive-core easyarchive-starter -g'*.java'
```

Expected: multiple hits in `easyarchive-starter` services and a small number in project-owned common/core classes.

### Task 2: Normalize the shared exception foundation in easyarchive-common

**Files:**
- Modify: `easyarchive-common/src/main/java/com/openquartz/easyarchive/common/exception/EasyArchiveException.java`
- Modify: `easyarchive-common/src/main/java/com/openquartz/easyarchive/common/exception/Asserts.java`
- Modify: `easyarchive-common/src/main/java/com/openquartz/easyarchive/common/exception/CommonErrorCode.java`
- Modify: `easyarchive-common/src/main/java/com/openquartz/easyarchive/common/exception/DataExecuteErrorCode.java`
- Create: `easyarchive-common/src/main/java/com/openquartz/easyarchive/common/exception/CommonErrorCodeSupport.java`

- [ ] **Step 1: Add the failing error-code enum improvements**

Replace `CommonErrorCode` with a version aligned to the `easy-event` pattern and with the extra placeholder code needed by the shared tests:

```java
@Getter
public enum CommonErrorCode implements EasyArchiveErrorCode {

    PARAM_ILLEGAL_ERROR("01", "参数不合法异常"),
    CLASS_NOT_FOUND_ERROR("02", "Class {0} not exist!", true),
    METHOD_NOT_EXIST_ERROR("03", "Method not exist"),
    SEQUENCE_EXECUTOR_REGISTERED_ERROR("04", "sequence:{0} not registered!", true);

    private static final String BASE_CODE = "CommonError-";

    private final String errorCode;
    private final String errorMsg;
    private final boolean replacePlaceHold;

    CommonErrorCode(String errorCode, String errorMsg) {
        this(errorCode, errorMsg, false);
    }

    CommonErrorCode(String errorCode, String errorMsg, boolean replacePlaceHold) {
        this.errorCode = BASE_CODE + errorCode;
        this.errorMsg = errorMsg;
        this.replacePlaceHold = replacePlaceHold;
    }
}
```

- [ ] **Step 2: Replace the throwing static helper with a normal factory**

Update `EasyArchiveException` to this shape:

```java
public class EasyArchiveException extends RuntimeException {

    private final transient EasyArchiveErrorCode errorCode;

    public EasyArchiveException(EasyArchiveErrorCode errorCode) {
        super(errorCode.getErrorMsg());
        this.errorCode = errorCode;
    }

    public EasyArchiveException(EasyArchiveErrorCode errorCode, String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
    }

    public static EasyArchiveException withPlaceholders(EasyArchiveErrorCode errorCode, Object... placeHold) {
        return new EasyArchiveException(errorCode, MessageFormat.format(errorCode.getErrorMsg(), placeHold));
    }

    public EasyArchiveErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
```

- [ ] **Step 3: Update `Asserts` to use the new factory**

Use this replacement pattern in every placeholder branch:

```java
if (!expression) {
    throw EasyArchiveException.withPlaceholders(code, placeHold);
}
```

Keep the non-placeholder branches unchanged.

- [ ] **Step 4: Fix the shared data error-code prefix bug**

Update `DataExecuteErrorCode` so it actually applies its base prefix:

```java
private static final String BASE_CODE = "DataExecute-";

DataExecuteErrorCode(String errorCode, String errorMsg) {
    this.errorCode = BASE_CODE + errorCode;
    this.errorMsg = errorMsg;
}
```

- [ ] **Step 5: Run the shared exception tests**

Run:

```bash
mvn -pl easyarchive-common -Dtest=EasyArchiveExceptionTest test
```

Expected: PASS

### Task 3: Introduce module-level error codes and the starter business exception

**Files:**
- Create: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/exception/CoreErrorCode.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/exception/StarterErrorCode.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/exception/StarterManageException.java`

- [ ] **Step 1: Add `CoreErrorCode` for the current core hotspots**

Create:

```java
package com.openquartz.easyarchive.core.exception;

import com.openquartz.easyarchive.common.exception.EasyArchiveErrorCode;
import lombok.Getter;

@Getter
public enum CoreErrorCode implements EasyArchiveErrorCode {

    RANDOM_ALPHA_NUM_TYPE_UNSUPPORTED("01", "Unsupported random alpha num type: {0}", true),
    TIME_UNIT_UNSUPPORTED("02", "Unsupported time unit: {0}", true);

    private static final String BASE_CODE = "CoreError-";

    private final String errorCode;
    private final String errorMsg;
    private final boolean replacePlaceHold;

    CoreErrorCode(String errorCode, String errorMsg, boolean replacePlaceHold) {
        this.errorCode = BASE_CODE + errorCode;
        this.errorMsg = errorMsg;
        this.replacePlaceHold = replacePlaceHold;
    }
}
```

- [ ] **Step 2: Add `StarterErrorCode` covering the service-layer violations in this pass**

Create:

```java
@Getter
public enum StarterErrorCode implements EasyArchiveErrorCode {

    ARCHIVE_GROUP_CODE_REQUIRED("01", "分组编码不能为空"),
    ARCHIVE_GROUP_NOT_FOUND("02", "归档分组不存在"),
    ARCHIVE_GROUP_DISABLED("03", "归档分组已停用"),
    ARCHIVE_GROUP_HAS_ACTIVE_TASK("04", "归档分组存在运行中的任务"),
    ARCHIVE_GROUP_HAS_NO_ENABLED_ITEM("05", "归档分组没有已启用的归档项"),
    ARCHIVE_GROUP_ITEM_REQUIRED("06", "归档明细不能为空"),
    ARCHIVE_GROUP_ITEM_NOT_FOUND("07", "归档明细不存在"),
    DATASOURCE_NOT_FOUND("08", "归档连接不存在"),
    USER_NOT_FOUND("09", "用户不存在"),
    PERMISSION_DENIED("10", "无权限访问该资源");

    private static final String BASE_CODE = "StarterError-";

    private final String errorCode;
    private final String errorMsg;
    private final boolean replacePlaceHold;

    StarterErrorCode(String errorCode, String errorMsg) {
        this(errorCode, errorMsg, false);
    }

    StarterErrorCode(String errorCode, String errorMsg, boolean replacePlaceHold) {
        this.errorCode = BASE_CODE + errorCode;
        this.errorMsg = errorMsg;
        this.replacePlaceHold = replacePlaceHold;
    }
}
```

- [ ] **Step 3: Add the starter-specific exception subtype**

Create:

```java
package com.openquartz.easyarchive.starter.exception;

import com.openquartz.easyarchive.common.exception.EasyArchiveErrorCode;
import com.openquartz.easyarchive.common.exception.EasyArchiveException;

public class StarterManageException extends EasyArchiveException {

    public StarterManageException(EasyArchiveErrorCode errorCode) {
        super(errorCode);
    }

    public StarterManageException(EasyArchiveErrorCode errorCode, String errorMsg) {
        super(errorCode, errorMsg);
    }

    public static StarterManageException withPlaceholders(EasyArchiveErrorCode errorCode, Object... placeHold) {
        EasyArchiveException exception = EasyArchiveException.withPlaceholders(errorCode, placeHold);
        return new StarterManageException(errorCode, exception.getMessage());
    }
}
```

- [ ] **Step 4: Compile the new exception units**

Run:

```bash
mvn -pl easyarchive-common,easyarchive-core,easyarchive-starter -DskipTests compile
```

Expected: PASS

### Task 4: Migrate starter service-layer business exceptions to error codes

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveConnectionServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveTaskLogServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/DataPermissionServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/UserDatasourcePermissionServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/AuthServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/CustomUserDetailsService.java`

- [ ] **Step 1: Replace direct throws with `StarterManageException`**

Use this replacement pattern:

```java
if (normalizedGroupCode == null || normalizedGroupCode.isEmpty()) {
    throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_CODE_REQUIRED);
}
```

For placeholder-based messages, use:

```java
throw StarterManageException.withPlaceholders(
        StarterErrorCode.UNSUPPORTED_DATASOURCE_TYPE, source.getClass().getName());
```

- [ ] **Step 2: Centralize repeated service validation helpers**

In services with repeated validation blocks, extract small private helpers such as:

```java
private void validateEnableStatus(Integer enableStatus) {
    if (EnableStatusEnum.fromCode(enableStatus) == null) {
        throw new StarterManageException(StarterErrorCode.ENABLE_STATUS_INVALID);
    }
}
```

and:

```java
private ArchiveGroup requireExistingGroup(Long groupId) {
    if (groupId == null) {
        throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_ID_REQUIRED);
    }
    ArchiveGroup group = groupMapper.selectById(groupId);
    if (group == null) {
        throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_NOT_FOUND);
    }
    return group;
}
```

- [ ] **Step 3: Update the affected tests to assert exception codes**

Use this assertion style in all modified service tests:

```java
StarterManageException exception = assertThrows(StarterManageException.class,
        () -> service.updateStatus(groupId, itemId, 99));

assertEquals(StarterErrorCode.ENABLE_STATUS_INVALID, exception.getErrorCode());
```

- [ ] **Step 4: Run the starter service tests**

Run:

```bash
mvn -pl easyarchive-starter \
  -Dtest=ArchiveConnectionServiceImplTest,ArchiveGroupExecutionServiceImplTest,ArchiveGroupItemByIdServiceImplTest,ArchiveGroupItemByTimeServiceImplTest,ArchiveGroupServiceImplTest,ArchiveTaskLogServiceImplTest,DataPermissionServiceImplTest,UserDatasourcePermissionServiceImplTest,AuthServiceImplTest,UserServiceImplTest \
  test
```

Expected: PASS

### Task 5: Replace core magic values with enums and constants

**Files:**
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/expr/executors/RandomAlphaNumExecutor.java`
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/expr/executors/TimeAddExecutor.java`
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/sink/mysql/MysqlSink.java`
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/executor/ArchiveExecutor.java`

- [ ] **Step 1: Replace random classifier strings with an enum**

Refactor `RandomAlphaNumExecutor` around a nested enum:

```java
private enum CharacterClass {
    UPPERCASE_NO_I("UPPERCASE_NO_I", UP_CASE_IGNORE_I_SET),
    UPPERCASE("UPPERCASE", UP_CASE_SET),
    LOWERCASE("LOWERCASE", LOWER_CASE_SET),
    NUMBER("NUMBER", NUMBER_SET);

    private final String code;
    private final char[] characters;

    static CharacterClass fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> EasyArchiveException.withPlaceholders(
                        CoreErrorCode.RANDOM_ALPHA_NUM_TYPE_UNSUPPORTED, code));
    }
}
```

- [ ] **Step 2: Replace time-unit string validation with an enum-backed lookup**

Refactor `TimeAddExecutor` around a nested enum:

```java
private enum SupportedTimeUnit {
    DAY("day"),
    HOUR("hour"),
    MINUTE("minute"),
    SECOND("second");

    static SupportedTimeUnit fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> EasyArchiveException.withPlaceholders(
                        CoreErrorCode.TIME_UNIT_UNSUPPORTED, code));
    }
}
```

- [ ] **Step 3: Name the core technical thresholds**

Apply class-local constants such as:

```java
private static final int BATCH_FLUSH_SIZE = 500;
private static final long DEFAULT_PROCESSED_RECORDS = 0L;
private static final long DEFAULT_FINISHED_FLAG = 0L;
```

Use them in `MysqlSink` and `ArchiveExecutor` instead of inline literals.

- [ ] **Step 4: Run the core tests**

Run:

```bash
mvn -pl easyarchive-core -Dtest=RandomAlphaNumExecutorTest test
```

Expected: PASS

### Task 6: Add repository rule documentation and agent guidance

**Files:**
- Create: `docs/conventions/exception-and-magic-value-rules.md`
- Modify: `AGENTS.md`

- [ ] **Step 1: Add the human-readable rule document**

Create `docs/conventions/exception-and-magic-value-rules.md` with these sections:

```md
# Exception And Magic Value Rules

## Business Exceptions
- Service-layer business failures must use `EasyArchiveErrorCode`.
- Do not throw `IllegalArgumentException` or `IllegalStateException` for business validation or state transitions.

## Error-Code Design
- Use module-prefixed enums such as `CommonErrorCode`, `CoreErrorCode`, and `StarterErrorCode`.
- Use placeholder-based messages for dynamic values.

## Magic Values
- Use enums for closed business vocabularies.
- Use `private static final` constants for technical thresholds and defaults.
- Do not create global constants for trivial local literals.
```

- [ ] **Step 2: Add the same rule to project agent guidance**

Append this section to `AGENTS.md`:

```md
## Exception And Magic Value Rules

- Business/service exceptions must use project-defined error codes and custom exceptions.
- Do not introduce new `IllegalArgumentException` or `IllegalStateException` in service-layer business logic.
- Convert closed-set status/type values to enums.
- Convert technical thresholds/defaults to named constants.
```

- [ ] **Step 3: Verify the docs are present**

Run:

```bash
sed -n '1,200p' docs/conventions/exception-and-magic-value-rules.md
sed -n '1,260p' AGENTS.md
```

Expected: both files show the new rules.

### Task 7: Final verification, scans, and commit

**Files:**
- Modify: `easyarchive-common/src/test/java/com/openquartz/easyarchive/common/exception/EasyArchiveExceptionTest.java`
- Modify: `easyarchive-core/src/test/java/com/openquartz/easyarchive/core/expr/executors/RandomAlphaNumExecutorTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImplTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImplTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImplTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/UserDatasourcePermissionServiceImplTest.java`

- [ ] **Step 1: Run focused module tests**

Run:

```bash
mvn -pl easyarchive-common,easyarchive-core,easyarchive-starter \
  -Dtest=EasyArchiveExceptionTest,RandomAlphaNumExecutorTest,ArchiveConnectionServiceImplTest,ArchiveGroupExecutionServiceImplTest,ArchiveGroupItemByIdServiceImplTest,ArchiveGroupItemByTimeServiceImplTest,ArchiveGroupServiceImplTest,ArchiveTaskLogServiceImplTest,DataPermissionServiceImplTest,UserDatasourcePermissionServiceImplTest,AuthServiceImplTest,UserServiceImplTest \
  test
```

Expected: PASS

- [ ] **Step 2: Run final repository scans**

Run:

```bash
rg -n "throw new (IllegalArgumentException|IllegalStateException)" easyarchive-common easyarchive-core easyarchive-starter -g'*.java'
rg -n "\"type1\"|\"type2\"|\"type3\"|\"type4\"|time unit error|j % 500|== 0|!= 0" easyarchive-core easyarchive-starter -g'*.java'
```

Expected: no remaining business-layer raw exceptions in the targeted files; no remaining targeted classifier strings or named hotspot literals after manual review of any residual `== 0` / `!= 0` matches.

- [ ] **Step 3: Commit**

```bash
git add \
  AGENTS.md \
  docs/conventions/exception-and-magic-value-rules.md \
  docs/superpowers/plans/2026-06-04-exception-and-magic-value-governance.md \
  easyarchive-common/src/main/java/com/openquartz/easyarchive/common/exception/CommonErrorCode.java \
  easyarchive-common/src/main/java/com/openquartz/easyarchive/common/exception/DataExecuteErrorCode.java \
  easyarchive-common/src/main/java/com/openquartz/easyarchive/common/exception/EasyArchiveException.java \
  easyarchive-common/src/main/java/com/openquartz/easyarchive/common/exception/Asserts.java \
  easyarchive-common/src/test/java/com/openquartz/easyarchive/common/exception/EasyArchiveExceptionTest.java \
  easyarchive-core/src/main/java/com/openquartz/easyarchive/core/exception/CoreErrorCode.java \
  easyarchive-core/src/main/java/com/openquartz/easyarchive/core/expr/executors/RandomAlphaNumExecutor.java \
  easyarchive-core/src/main/java/com/openquartz/easyarchive/core/expr/executors/TimeAddExecutor.java \
  easyarchive-core/src/main/java/com/openquartz/easyarchive/core/sink/mysql/MysqlSink.java \
  easyarchive-core/src/main/java/com/openquartz/easyarchive/core/executor/ArchiveExecutor.java \
  easyarchive-core/src/test/java/com/openquartz/easyarchive/core/expr/executors/RandomAlphaNumExecutorTest.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/exception/StarterErrorCode.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/exception/StarterManageException.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveConnectionServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveTaskLogServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/DataPermissionServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/UserDatasourcePermissionServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/AuthServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/CustomUserDetailsService.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImplTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImplTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImplTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/UserDatasourcePermissionServiceImplTest.java
git commit -m "refactor: govern exceptions and magic values"
```
