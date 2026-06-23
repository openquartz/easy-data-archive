package com.openquartz.easyarchive.core.expr.executors;

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

/**
 * 使用SPEL 提取对象的值
 * 使用格式为：{spel 表达式}
 * 表达式中不许带{ }
 */
@Slf4j
public class SpelExecutor implements CommandExecutor {

    private ExpressionParser parser;
    private Environment environment;

    @Override
    public Result exec(Command command) {

        // 解析表达式
        String expressionStr = String.join(" ", command.getParams());
        try {

            Expression expression = parser.parseExpression(expressionStr);

            // 创建评估上下文
            EvaluationContext evaluationContext = new StandardEvaluationContext();
            if (environment != null && environment.getLocalContextParam()!=null){
                Map<String, Object> localContextParam = environment.getLocalContextParam();
                localContextParam.forEach(evaluationContext::setVariable);
            }
            // 执行表达式获取计算结果
            Object value = expression.getValue(evaluationContext);
            return Result.success(String.valueOf(value));
        }catch (Exception ex){
            log.error("Spel expression exe error! expression:{}",expressionStr,ex);
            return Result.systemError("Exception evaluate error:"+ex.getMessage());
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
        this.parser = new SpelExpressionParser();
    }
}
