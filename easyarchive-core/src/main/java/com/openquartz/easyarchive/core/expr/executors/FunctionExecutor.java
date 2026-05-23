package com.openquartz.easyarchive.core.expr.executors;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openquartz.easyarchive.core.expr.cmd.Command;
import com.openquartz.easyarchive.core.expr.cmd.Environment;
import com.openquartz.easyarchive.core.expr.cmd.Result;
import com.openquartz.easyarchive.common.exception.Asserts;
import com.openquartz.easyarchive.common.exception.CommonErrorCode;
import com.openquartz.easyarchive.common.util.ExceptionUtils;
import com.openquartz.easyarchive.common.util.StringUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 函数支持器
 * 要求函数支持返回值为字符串
 * 表达式为：{func 全限定方法名 参数1,参数2,参数3}
 * 函数要求为：入参是字符串，返回值为字符串或为object。为public 的静态函数
 */
public class FunctionExecutor implements CommandExecutor {

    private final Cache<String, Method> function2MethodCache = CacheBuilder
            .newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();


    @Override
    public Result exec(Command command) {

        // 读取全限定名
        String funcName = command.getFirstParam();

        try {
            int paramCount = 0;
            Object[] invokeArgs = null;
            if (StringUtils.isNotBlank(command.getSecondParam())) {
                String[] args = command.getSecondParam().split(",");
                invokeArgs = Stream.of(args).map(String::trim).map(e -> (Object) e).toArray();
                paramCount = invokeArgs.length;
            }

            Method method = getMethod(funcName, paramCount);
            String res;
            if (invokeArgs == null) {
                res = String.valueOf(method.invoke(null));
            } else {
                res = String.valueOf(method.invoke(null, invokeArgs));
            }
            return Result.success(res);
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * 根据全限定方法名获取Method对象
     * 全限定方法名格式: className#methodName 或 packageName.className#methodName
     *
     * @param fullyQualifiedMethodName 全限定方法名
     * @param paramCount 参数个数
     * @return Method对象
     * @throws ClassNotFoundException 当找不到类时抛出
     * @throws NoSuchMethodException  当找不到方法时抛出
     */
    private Method getMethod(String fullyQualifiedMethodName, int paramCount) throws ClassNotFoundException, NoSuchMethodException {

        String cacheKey = fullyQualifiedMethodName + ":" + paramCount;
        // 先从缓存中获取
        Method cachedMethod = function2MethodCache.getIfPresent(cacheKey);
        if (cachedMethod != null) {
            return cachedMethod;
        }

        // 解析全限定方法名
        int lastHashIndex = fullyQualifiedMethodName.lastIndexOf('#');
        Asserts.isTrue(lastHashIndex > 0 && lastHashIndex < fullyQualifiedMethodName.length() - 1,
                CommonErrorCode.PARAM_ILLEGAL_ERROR, "方法名格式错误，应为: className#methodName");

        String className = fullyQualifiedMethodName.substring(0, lastHashIndex);
        String methodName = fullyQualifiedMethodName.substring(lastHashIndex + 1);

        // 加载类
        Class<?> clazz = Class.forName(className);

        // 获取方法
        Method method;
        if (paramCount == 0) {
            method = clazz.getMethod(methodName);
        } else {
            Class<?>[] paramTypes = new Class<?>[paramCount];
            for (int i = 0; i < paramCount; i++) {
                paramTypes[i] = String.class;
            }
            method = clazz.getMethod(methodName, paramTypes);
        }

        // 放入缓存
        function2MethodCache.put(cacheKey, method);

        return method;
    }

    @Override
    public void validate(Command command) {
        Asserts.notNull(command, CommonErrorCode.PARAM_ILLEGAL_ERROR);
        List<String> params = command.getParams();
        Asserts.notNull(params, CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.isTrue(!params.isEmpty(), CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.isTrue(StringUtils.isNotBlank(command.getFirstParam()), CommonErrorCode.PARAM_ILLEGAL_ERROR);
    }

    @Override
    public void init(Environment environment) {

    }
}
