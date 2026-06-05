## 新增表达式指令规则

本文用于说明 EasyArchive 中如何新增一个表达式指令，并保持实现、注册、测试、文档同步。

### 1. 适用范围

适用于 `easyarchive-core` 模块下的表达式执行器扩展，目录为：

`easyarchive-core/src/main/java/com/openquartz/easyarchive/core/expr/executors`

### 2. 新增步骤

#### 2.1 新增执行器

新增一个实现 `CommandExecutor` 的类：

- 实现 `exec(Command command)`：完成业务逻辑并返回 `Result`
- 实现 `validate(Command command)`：完成参数校验
- 实现 `init(Environment environment)`：仅在需要环境上下文时使用

要求：

- 业务参数校验统一使用 `Asserts`
- 服务层和业务逻辑中不要引入新的 `IllegalArgumentException`、`IllegalStateException`
- 固定索引、阈值、默认值使用具名常量，不直接写魔法值
- 如果存在封闭集合取值，优先使用枚举表达

示例骨架：

```java
public class DemoExecutor implements CommandExecutor {

    private static final int FIRST_PARAM_INDEX = 0;

    @Override
    public Result exec(Command command) {
        String value = command.indexOfParam(FIRST_PARAM_INDEX);
        return Result.success(value);
    }

    @Override
    public void validate(Command command) {
        Asserts.notNull(command, CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.notEmpty(command.getFirstParam(), CommonErrorCode.PARAM_ILLEGAL_ERROR);
    }

    @Override
    public void init(Environment environment) {
        // no-op
    }
}
```

#### 2.2 注册执行器

在 `ExecutorRegistry#registerDefault()` 中添加指令名与执行器的映射：

```java
register("demo", DemoExecutor.class);
```

规则：

- 指令名使用小写下划线风格
- 指令名必须唯一
- 注册器中保留统一入口，不要分散到其他类中做隐式注册

#### 2.3 添加测试

在 `easyarchive-core/src/test/java/com/openquartz/easyarchive/core/expr/executors` 下新增对应测试类。

至少覆盖：

- 正常路径：核心输出是否符合预期
- 参数异常：空值、非法格式、边界值
- 特殊分支：例如默认值、拼接逻辑、枚举取值、负数归一化等

测试要求：

- 优先断言明确结果
- 业务异常优先断言项目错误码
- 新功能按 TDD 执行，先写失败测试，再补实现

#### 2.4 更新文档

同步更新：

- [expr-introduction.md](./expr-introduction.md)

文档至少说明：

- 指令语法
- 参数含义
- 约束条件
- 返回结果特点
- 典型示例

### 3. 设计建议

#### 3.1 参数解析

- 优先使用 `getFirstParam()`、`getSecondParam()`、`indexOfParam(int index)`
- 多参数拼接时，明确是否保留空格、分隔符、顺序

#### 3.2 错误处理

- 参数问题统一抛 `CommonErrorCode.PARAM_ILLEGAL_ERROR`
- 如果是特定业务语义错误，复用或补充项目已有错误码

#### 3.3 环境依赖

如果指令依赖上下文参数或全局配置：

- 在 `init(Environment environment)` 中缓存环境对象
- 在 `exec` 中只读取必要上下文
- 避免在执行器中混入连接创建、复杂装配等职责

### 4. 交付检查清单

- 已新增 `CommandExecutor` 实现类
- 已在 `ExecutorRegistry` 中完成注册
- 已补充最小必要测试
- 已更新表达式说明文档
- 已验证至少一个成功用例和一个失败用例
