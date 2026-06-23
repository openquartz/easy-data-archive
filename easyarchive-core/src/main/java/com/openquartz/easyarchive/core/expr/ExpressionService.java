package com.openquartz.easyarchive.core.expr;

import com.openquartz.easyarchive.core.expr.cmd.AssignExtParam;
import com.openquartz.easyarchive.core.expr.cmd.ExpressionEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表达式服务
 * 表达式格式: ${expression}
 */
@Slf4j
public class ExpressionService {

    private static final ExpressionService INSTANCE = new ExpressionService();

    public static ExpressionService getInstance() {
        return INSTANCE;
    }

    private final ExpressionEngine expressionEngine = ExpressionEngine.builder().build();

    // 匹配 $表达式$ 格式的正则表达式
    public static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$([^$]+)\\$");

    public String parse(String expr) {
        return parseExpression(expr, null);
    }

    public String parse(String expr, AssignExtParam param) {
        return parseExpression(expr, param);
    }

    /**
     * 解析表达式
     *
     * @param expr  表达式
     * @param param param
     * @return 解析结果
     */
    private String parseExpression(String expr, AssignExtParam param) {
        if (expr == null || expr.isEmpty()) {
            return expr;
        }

        Matcher matcher = EXPRESSION_PATTERN.matcher(expr);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {

            // 提取 $表达式$ 中的表达式
            String expression = matcher.group(1);
            String executionResult;

            try {
                // 表达式执行结果
                if (param != null && param.getParams() != null) {
                    executionResult = expressionEngine.execute(expression, param.getParams());
                } else {
                    executionResult = expressionEngine.execute(expression);
                }
            } catch (Exception ex) {
                log.error("parse expression error", ex);
                throw ex;
            }

            // 转义特殊字符以避免替换时出现问题
            matcher.appendReplacement(result, executionResult != null ?
                    Matcher.quoteReplacement(executionResult) : "");
        }

        matcher.appendTail(result);
        return result.toString();
    }
}
