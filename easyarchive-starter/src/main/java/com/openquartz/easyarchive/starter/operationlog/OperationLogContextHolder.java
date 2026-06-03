package com.openquartz.easyarchive.starter.operationlog;

public final class OperationLogContextHolder {

    private static final ThreadLocal<OperationLogContext> HOLDER = new ThreadLocal<>();

    private OperationLogContextHolder() {
    }

    public static void set(OperationLogContext context) {
        HOLDER.set(context);
    }

    public static OperationLogContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
