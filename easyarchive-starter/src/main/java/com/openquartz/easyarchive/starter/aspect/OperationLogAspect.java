package com.openquartz.easyarchive.starter.aspect;

import com.openquartz.easyarchive.starter.annotation.OperationLog;
import com.openquartz.easyarchive.starter.mapper.SysOperationLogMapper;
import com.openquartz.easyarchive.starter.model.entity.SysOperationLog;
import com.openquartz.easyarchive.starter.operationlog.OperationLogContext;
import com.openquartz.easyarchive.starter.operationlog.OperationLogContextHolder;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;

/**
 * 操作日志AOP
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    private final SysOperationLogMapper sysOperationLogMapper;
    private final DataPermissionService dataPermissionService;

    @Pointcut("@annotation(com.openquartz.easyarchive.starter.annotation.OperationLog)")
    public void operationLogPointcut() {
    }

    @Around(value = "operationLogPointcut() && @annotation(operationLog)", argNames = "joinPoint,operationLog")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        OperationLogContext context = new OperationLogContext();
        context.setModuleCode(operationLog.module());
        context.setActionCode(operationLog.action());
        context.setButtonName(resolveButtonName(operationLog));
        context.setRequestParamSummary(operationLog.logParams() ? Arrays.toString(joinPoint.getArgs()) : null);
        OperationLogContextHolder.set(context);

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            context.setResultStatus(0);
            context.setResponseCode("SUCCESS");
            return result;
        } catch (Throwable ex) {
            context.setResultStatus(1);
            context.setErrorMessage(truncateErrorMessage(ex.getMessage()));
            throw ex;
        } finally {
            persistLog(operationLog, context, System.currentTimeMillis() - startTime);
            OperationLogContextHolder.clear();
        }
    }

    private void persistLog(OperationLog operationLog, OperationLogContext context, long costMs) {
        HttpServletRequest request = currentRequest();
        SysOperationLog logRow = new SysOperationLog();
        CurrentUserInfo currentUser = currentUser();
        if (currentUser != null) {
            logRow.setUserId(currentUser.getUserId());
        }
        logRow.setModuleCode(defaultIfBlank(context.getModuleCode(), operationLog.module()));
        logRow.setActionCode(defaultIfBlank(context.getActionCode(), operationLog.action()));
        logRow.setButtonName(defaultIfBlank(context.getButtonName(), resolveButtonName(operationLog)));
        logRow.setBizType(context.getBizType());
        logRow.setBizId(context.getBizId());
        logRow.setBizKey(context.getBizKey());
        logRow.setContent(defaultIfBlank(context.getContent(), operationLog.value()));
        if (request != null) {
            logRow.setRequestUri(request.getRequestURI());
            logRow.setRequestMethod(request.getMethod());
            logRow.setClientIp(request.getRemoteAddr());
        }
        logRow.setRequestParam(context.getRequestParamSummary());
        logRow.setResponseCode(defaultIfBlank(context.getResponseCode(), "SUCCESS"));
        logRow.setResultStatus(context.getResultStatus() == null ? 0 : context.getResultStatus());
        logRow.setCostMs(costMs);
        logRow.setErrorMessage(context.getErrorMessage());
        logRow.setOperateTime(new Date());
        sysOperationLogMapper.insert(logRow);
        log.info("操作结束: {}，耗时: {}ms", logRow.getButtonName(), costMs);
    }

    private CurrentUserInfo currentUser() {
        try {
            return dataPermissionService.getCurrentUser();
        } catch (RuntimeException ex) {
            log.debug("skip current user for operation log: {}", ex.getMessage());
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes == null ? null : attributes.getRequest();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String resolveButtonName(OperationLog operationLog) {
        return defaultIfBlank(operationLog.button(), operationLog.value());
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String truncateErrorMessage(String errorMessage) {
        if (!StringUtils.hasText(errorMessage) || errorMessage.length() <= ERROR_MESSAGE_MAX_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }
}
