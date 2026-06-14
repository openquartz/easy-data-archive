# EasyArchive 表达式引擎指南

## 1. 概述

表达式引擎是 EasyArchive 的核心功能之一，支持在归档规则中使用动态表达式进行字段映射、表名生成、数据转换等操作。

### 1.1 表达式语法

基本格式：

```
$<command> <param1> <param2> ... {<nested_expression>}$
```

- 以 `$` 开头，以 `$` 结尾
- 第一个单词是命令名，后续是参数
- 支持嵌套表达式，用 `{}` 包裹子表达式

### 1.2 使用场景

| 场景 | 说明 |
|------|------|
| 表名生成 | 根据时间自动生成归档表名，如 `order_20240101` |
| 字段映射 | 将源字段值转换为目标字段值 |
| 数据转换 | 对数据进行加密、哈希、取模等操作 |
| 默认值填充 | 为新增字段填充固定值或动态计算值 |

---

## 2. 内置命令

### 2.1 const — 常量值

返回固定的字符串值。

**语法：** `$const <value>$`

**示例：**

```
# 固定字符串
$table_name: $const order_archive$

# 在表名中使用
targetTableName: $const log_2024$
```

### 2.2 time — 当前时间

返回当前时间，支持格式化。

**语法：** `$time <format>$`

**示例：**

```
# 默认格式
$time$ → 2024-01-15 10:30:00

# 自定义格式
$time YYYY-MM-DD$ → 2024-01-15
$time YYYYMMDD$ → 20240115
$time HH:mm:ss$ → 10:30:00
$time YYYY年MM月DD日$ → 2024年01月15日
```

### 2.3 time_add — 时间加减

在指定时间基础上增加/减少指定的时间单位。

**语法：** `$time_add <time> <amount> <unit>$`

**时间单位：** `day`、`hour`、`minute`、`second`

**示例：**

```
# 在当前时间基础上加 1 天
$time_add now 1 day$

# 在当前时间基础上减 30 分钟
$time_add now -30 minute$

# 在指定时间基础上加 7 天
$time_add 2024-01-15 7 day$

# 格式化输出
$time_add now 1 day YYYY-MM-DD$
```

### 2.4 time_format — 时间格式化

将时间字符串格式化为指定格式。

**语法：** `$time_format <time> <format>$`

**示例：**

```
$time_format now YYYYMMDD$ → 20240115
$time_format 2024-01-15 YYYY/MM/DD$ → 2024/01/15
```

### 2.5 fix — 定长补零

将数字格式为指定位数的定长字符串，不足部分左侧补零。

**语法：** `$fix <number> <length>$`

**示例：**

```
$fix 123 5$ → 00123
$fix 7 3$ → 007
$fix 1 8$ → 00000001
```

### 2.6 env — 环境变量

获取系统环境变量值。

**语法：** `$env <ENV_VAR_NAME>$`

**示例：**

```
$env HOSTNAME$ → 服务器主机名
$env NODE_ENV$ → production
```

### 2.7 rand_n — 随机数字

生成指定位数的随机数字。

**语法：** `$rand_n <digits>$`

**示例：**

```
$rand_n 5$ → 38472
$rand_n 6$ → 910234
$rand_n 4$ → 5671
```

### 2.8 rand_c — 随机字母数字

生成指定长度的随机字母数字混合字符串。

**语法：** `$rand_c <length>$`

**示例：**

```
$rand_c 8$ → aB3xK9mQ
$rand_c 12$ → xY7nP2wR5tL8
```

### 2.9 func — 函数调用

调用内置函数或 Java 方法。

**语法：** `$func <function_name> <params>$`

**示例：**

```
# MD5 哈希
$func md5 hello$ → 5d41402abc4b2a76b9719d911017c592

# 其他内置函数
$func upper hello$ → HELLO
$func lower HELLO$ → hello
$func abs -42$ → 42
```

### 2.10 mod — 取模运算

对两个数值执行取模运算。

**语法：** `$mod <a> <b>$`

**示例：**

```
$mod 100 7$ → 2
$mod 10 3$ → 1
$mod 1000 100$ → 0
```

### 2.11 sql — SQL 查询

执行 SQL 查询并将结果作为表达式值。

**语法：** `$sql <SQL_QUERY>$`

**示例：**

```
# 查询最大 ID
$maxId: $sql SELECT MAX(id) FROM source_table$

# 查询配置值
$config: $sql SELECT config_value FROM sys_config WHERE config_key = 'archive_prefix'$
```

> **安全提醒**：SQL 表达式仅支持 SELECT 查询，不支持写操作。

### 2.12 spel — Spring 表达式语言

使用 Spring Expression Language (SpEL) 执行复杂表达式。

**语法：** `$spel <expression>$`

**示例：**

```
# 生成 UUID
$spel #{T(java.util.UUID).randomUUID()}$

# 数学计算
$spel #{100 + 200}$ → 300

# 字符串操作
$spel #'hello'.toUpperCase()` → HELLO
```

### 2.13 hash_mod — Hash 取模

对指定字段值计算哈希值后取模，常用于分库分表场景。

**语法：** `$hash_mod <field_name> <modulus>$`

**示例：**

```
# 对用户 ID 取模，得到 10 个分片之一
$hash_mod userId 10$ → 0~9 之间的整数

# 配合表名使用
targetTable: $hash_mod orderId 4$ → table_0 ~ table_3
```

---

## 3. 嵌套表达式

表达式支持嵌套，可将子表达式的结果作为外层表达式的参数。

### 3.1 基本嵌套

```
# 拼接前缀和时间戳
$table_name: $prefix_${time YYYYMMDD}$

# 多层嵌套
$result: $upper_${const hello world}$
```

### 3.2 在归档规则中使用

**按时间归档 — 动态目标表名：**

```
sourceTable: order_log
targetTable: $const order_log_archive_${time YYYYMMDD}$
timeField: created_at
keepDays: 30
```

**按 ID 归档 — 动态字段映射：**

```
sourceTable: user_order
targetTable: user_order_history
targetFields:
  - id
  - user_id
  - order_no: $const ORD-${fix ${rand_n 6} 6}$
  - archive_time: $time YYYY-MM-DD HH:mm:ss$
```

---

## 4. 实战示例

### 4.1 按月归档订单表

**需求：** 将 3 个月前的订单数据归档到按月命名的表中。

```yaml
sourceTable: order
targetTable: $const order_archive_${time YYYYMM}$
timeField: created_at
keepDays: 90
```

### 4.2 按用户分片归档

**需求：** 根据用户 ID 的哈希值将数据分发到不同的分片表。

```yaml
sourceTable: user_transaction
targetTable: $hash_mod userId 4$
timeField: transaction_time
keepDays: 180
```

### 4.3 带数据转换的归档

**需求：** 归档时生成唯一的归档编号，并记录归档时间。

```yaml
sourceTable: invoice
targetTable: invoice_history
targetFields:
  - id
  - invoice_no: $const INV-${time YYYYMMDD}-${fix ${rand_n 4} 4}$
  - archived_at: $time YYYY-MM-DD HH:mm:ss$
  - status: $const ARCHIVED$
timeField: invoice_date
keepDays: 365
```

### 4.4 基于查询结果的动态归档

**需求：** 根据配置表中的值动态决定归档策略。

```yaml
sourceTable: log
targetTable: $sql SELECT archive_table FROM sys_archive_config WHERE table_name = 'log'$
timeField: log_time
keepDays: 30
```

---

## 5. 扩展自定义命令

### 5.1 实现 CommandExecutor

```java
@Component
public class UpperCaseExecutor implements CommandExecutor {
    @Override
    public Result execute(CommandNode node, Environment env) {
        String input = node.getParams().get(0);
        return Result.success(input.toUpperCase());
    }
}
```

### 5.2 注册命令

```java
@Configuration
public class ExpressionConfig {
    @Bean
    public ExecutorRegistry executorRegistry(
            UpperCaseExecutor upperCaseExecutor) {
        ExecutorRegistry registry = new ExecutorRegistry();
        registry.register("upper", UpperCaseExecutor.class);
        return registry;
    }
}
```

### 5.3 使用自定义命令

```
# 将文本转为大写
$upper hello$ → HELLO
```

---

## 6. 注意事项

| 事项 | 说明 |
|------|------|
| 特殊字符 | 表达式中的 `$` 需要使用 `\` 转义 |
| 空值处理 | 表达式结果为 null 时，目标字段保持原值 |
| 性能影响 | 避免在表达式中执行耗时操作（如复杂 SQL 查询） |
| 嵌套深度 | 建议嵌套不超过 3 层，过深会影响可读性 |
| 调试技巧 | 可通过归档日志查看表达式解析结果 |
