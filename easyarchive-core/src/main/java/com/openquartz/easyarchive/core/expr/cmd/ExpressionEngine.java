package com.openquartz.easyarchive.core.expr.cmd;

import com.openquartz.easyarchive.core.expr.executors.CommandExecutor;
import com.openquartz.easyarchive.core.expr.executors.ExecutorFactory;
import com.openquartz.easyarchive.core.expr.executors.ExecutorRegistry;
import com.openquartz.easyarchive.core.expr.strategy.ExprExecuteStrategy;
import com.openquartz.easyarchive.core.expr.strategy.ExprExecuteStrategyFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 例如：
 * 表达式： {const DEMO}{fix {env w} 4}{time yyyyMMdd}{fix {seq DEMO_GENERATOR {env w}} 5}
 *
 * @author svnee
 */
@Slf4j
public class ExpressionEngine {

    private ExprExecuteStrategy strategy;
    private Environment environment;

    public String execute(String expr) {
        return execute(expr, null);
    }

    public String execute(String expr, Map<String, String> localContextParams) {
        long startTime = System.currentTimeMillis();
        try {
            expr = wrapAsConstCmd(expr);
            if (localContextParams != null) {
                registerLocalContextParams(localContextParams);
            }
            String res = strategy.exec(expr, environment);
            long costTime = System.currentTimeMillis() - startTime;
            if (costTime >= 100) {
                log.warn("[ExpressionEngine#execute] expr:{} cost-time:{}", expr, costTime);
            }
            return res;
        } finally {
            clearLocalContextParams();
        }
    }

    private void registerLocalContextParams(Map<String, String> localContextParams) {
        environment.registerLocalContextParam(localContextParams);
    }

    private void clearLocalContextParams() {
        environment.clearLocalContext();
    }

    private static String wrapAsConstCmd(String expr) {
        return String.format("{const %s}", expr);
    }

    private ExpressionEngine() {

    }

    public static ExpressionEngineBuilder builder() {
        return new ExpressionEngineBuilder();
    }

    public static class ExpressionEngineBuilder {

        private ExprExecuteStrategy strategy = ExprExecuteStrategyFactory.getDefaultStrategy();
        private final Map<String, Class<? extends CommandExecutor>> registerMap = new HashMap<>();
        /**
         * 全局上下文
         */
        private final Map<String, String> globalContextInfo = new HashMap<>();

        public ExpressionEngineBuilder setStrategyAsDefault() {
            this.strategy = ExprExecuteStrategyFactory.getDefaultStrategy();
            return this;
        }

        public ExpressionEngineBuilder setStrategy(ExprExecuteStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public ExpressionEngineBuilder registerExecutor(String cmdName, Class<? extends CommandExecutor> clazz) {
            this.registerMap.put(cmdName, clazz);
            return this;
        }

        public ExpressionEngineBuilder registerGlobalContextInfos(Map<String, String> globalContextInfo) {
            if (globalContextInfo != null) {
                this.globalContextInfo.putAll(globalContextInfo);
            }
            return this;
        }

        public ExpressionEngineBuilder registerGlobalContextInfo(String paramName, String value) {
            if (paramName != null && value != null) {
                this.globalContextInfo.put(paramName, value);
            }
            return this;
        }

        public ExpressionEngine build() {
            ExpressionEngine expressionEngine = new ExpressionEngine();
            Environment environment = new Environment();
            environment.setExecutorFactory(new ExecutorFactory(new ExecutorRegistry()));
            if (!registerMap.isEmpty()) {
                environment.registerExecutor(registerMap);
            }
            if (!globalContextInfo.isEmpty()) {
                environment.registerGlobalContextParam(globalContextInfo);
            }
            expressionEngine.environment = environment;
            if (this.strategy == null) {
                expressionEngine.strategy = ExprExecuteStrategyFactory.getDefaultStrategy();
            } else {
                expressionEngine.strategy = this.strategy;
            }
            return expressionEngine;
        }
    }
}
