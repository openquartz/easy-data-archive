## 规则表达式使用说明

表达式使用 `$` 作为开始和结束标识，格式为：`$表达式$`。

### 1. 配置表达式

表达式支持串接、嵌套；多个表达式使用 `{}` 分隔。

示例：

```text
$ {const DEMO}{fix {env w} 4}{time yyyyMMdd} $
```

### 2. 指令清单

#### 2.1 常量

语法：`{const 固定常量}`

#### 2.2 当前时间

语法：`{time 输出时间格式}`

支持的时间格式：

- `yyyyMMdd`
- `yyyyMMddHHmmss`
- `yyyy-MM-dd`
- `yyyyMMddHHmmssSSS`
- `yyMMddHHmmss`
- `yyMMdd`
- `Timestamp`：毫秒时间戳
- `Timestamp-s`：秒时间戳

#### 2.3 当前时间累加

语法：`{time_add 增加数值 时间单位 输出时间格式}`

要求：

- `增加数值` 必须为整数
- `时间单位` 支持 `Y`、`M`、`D`、`H`、`m`、`s`

示例：

```text
{time_add 1 D yyyyMMdd}
```

#### 2.4 长度补齐

语法：`{fix 字符串 补齐长度 [方向] [填充字符]}`

说明：

- 默认左侧补齐 `0`
- 第三个参数传 `l` 表示右侧补齐
- 第四个参数可指定填充字符

示例：

```text
{fix 12 4}
{fix 12 4 l X}
```

#### 2.5 自定义环境参数

语法：`{env 参数名 [local|global]}`

说明：

- 默认读取 `local`
- 也可显式指定 `global`

示例：

```text
{env w}
{env tenant global}
```

#### 2.6 随机字母数字字符串

语法：`{rand_c 长度 [随机范围类型]}`

支持的随机范围类型：

- `UPPERCASE_NO_I`：大写字母，排除 `I`
- `UPPERCASE`：大写字母
- `LOWERCASE`：小写字母
- `NUMBER`：数字

未传类型时，默认使用大小写字母和数字全集。

示例：

```text
{rand_c 8 UPPERCASE_NO_I}
{rand_c 12}
```

#### 2.7 随机数字字符串

语法：`{rand_n 长度}`

示例：

```text
{rand_n 6}
```

#### 2.8 外部函数调用

语法：`{func 函数名 参数1 参数2 ...}`

要求：

- 函数入参为字符串
- 返回值为字符串或对象
- 函数必须为 `public static`

示例：

```text
{func com.openquartz.sequence.generator.example.controller.SelfFunc#rand}
```

#### 2.9 SQL 查询

语法：`{sql 数据连接标识 执行sql}`

说明：

- 数据连接标识必须存在于归档连接配置中
- 查询建议加 `limit 1`，确保只返回一条结果

示例：

```text
{sql test1 select max(id) from table1 limit 1}
```

#### 2.10 SpEL 计算

语法：`{spel 表达式}`

说明：

- 表达式中不允许出现 `{`、`}`

示例：

```text
{spel #root.id}
```

#### 2.11 Hash 取模

语法：`{hash_mod 取模数 数据1 数据2 ...}`

说明：

- 将 `数据1...数据n` 按顺序直接拼接
- 按 Java 默认 `String.hashCode()` 算法计算哈希值
- 使用 `Math.floorMod(hash, 取模数)` 归一化，结果范围固定为 `0` 到 `取模数 - 1`
- `取模数` 必须为正整数

示例：

```text
{hash_mod 16 orderNo}
{hash_mod 32 tenantId userId}
```

#### 2.12 直接取模

语法：`{mod 取模数 源数}`

说明：

- `取模数` 和 `源数` 都必须为正整数
- 返回值为 `源数 % 取模数`

示例：

```text
{mod 16 33}
```

### 3. 组合示例

```text
{const DEMO}{fix {env w} 4}{time yyyyMMdd}{hash_mod 16 {env tenantId} {env orderId}}{mod 8 33}
```

### 4. 扩展说明

新增表达式指令的实现规范见：

- [新增表达式指令规则](./new-command-rule.md)
