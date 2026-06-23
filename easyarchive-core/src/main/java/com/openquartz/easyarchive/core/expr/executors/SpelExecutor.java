package com.openquartz.easyarchive.core.expr.executors;

import com.openquartz.easyarchive.common.api.model.DataRecord;
import com.openquartz.easyarchive.common.exception.Asserts;
import com.openquartz.easyarchive.common.exception.CommonErrorCode;
import com.openquartz.easyarchive.core.expr.cmd.Command;
import com.openquartz.easyarchive.core.expr.cmd.Environment;
import com.openquartz.easyarchive.core.expr.cmd.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 使用SPEL 提取对象的值
 * 使用格式为：{spel 表达式}
 * 表达式中不许带{ }
 *
 * <p>支持特性：
 * <ul>
 *   <li>大小写不敏感的 Map key 查找（#root.data['id'] 可匹配 ID/Id/id）</li>
 *   <li>Expression 编译缓存，避免重复解析相同表达式</li>
 * </ul>
 */
@Slf4j
public class SpelExecutor implements CommandExecutor {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    /**
     * Expression 编译缓存：表达式字符串 → 编译后的 Expression 对象
     */
    private static final ConcurrentHashMap<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>();

    private Environment environment;

    @Override
    public Result exec(Command command) {

        // 解析表达式
        String expressionStr = String.join(" ", command.getParams());
        try {

            Expression expression = getOrParseExpression(expressionStr);

            // 创建评估上下文
            EvaluationContext evaluationContext = new StandardEvaluationContext();
            if (environment != null && environment.getLocalContextParam() != null) {
                Map<String, Object> localContextParam = environment.getLocalContextParam();
                localContextParam.forEach((key, value) -> {
                    if (value instanceof DataRecord) {
                        // DataRecord 特殊处理：将其 data map 包装为大小写不敏感的 TreeMap
                        DataRecord record = (DataRecord) value;
                        TreeMap<String, Object> caseInsensitiveData = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                        caseInsensitiveData.putAll(record.getData());
                        evaluationContext.setVariable(key, caseInsensitiveData);
                    } else {
                        evaluationContext.setVariable(key, value);
                    }
                });
            }
            // 执行表达式获取计算结果
            Object value = expression.getValue(evaluationContext);
            return Result.success(String.valueOf(value));
        } catch (Exception ex) {
            log.error("Spel expression exe error! expression:{}", expressionStr, ex);
            return Result.systemError("Exception evaluate error:" + ex.getMessage());
        }
    }

    @Override
    public void validate(Command command) {
        Asserts.notEmpty(command.getParams(), CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.notEmpty(command.getFirstParam(), CommonErrorCode.PARAM_ILLEGAL_ERROR);
    }

    @Override
    public void init(Environment environment) {
        this.environment = environment;
    }

    /**
     * 获取或解析 Expression（带缓存）
     */
    private static Expression getOrParseExpression(String expressionStr) {
        return EXPRESSION_CACHE.computeIfAbsent(expressionStr, PARSER::parseExpression);
    }
}
