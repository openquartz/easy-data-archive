package com.openquartz.easyarchive.starter.operationlog;

import org.springframework.stereotype.Component;

@Component
public class DefaultOperationLogRecorder implements OperationLogRecorder {

    @Override
    public void record(OperationLogCommand command) {
        OperationLogContext context = OperationLogContextHolder.get();
        if (context == null || command == null) {
            return;
        }
        context.setModuleCode(command.getModuleCode());
        context.setActionCode(command.getActionCode());
        context.setButtonName(command.getButtonName());
        context.setBizType(command.getBizType());
        context.setBizId(command.getBizId());
        context.setBizKey(command.getBizKey());
        context.setContent(command.getContent());
    }

    @Override
    public void recordFailure(String errorMessage) {
        OperationLogContext context = OperationLogContextHolder.get();
        if (context == null) {
            return;
        }
        context.setResultStatus(1);
        context.setErrorMessage(errorMessage);
    }
}
