package com.openquartz.easyarchive.core.expr.executors;

import com.openquartz.easyarchive.common.exception.Asserts;
import com.openquartz.easyarchive.common.exception.CommonErrorCode;
import com.openquartz.easyarchive.core.expr.cmd.Command;
import com.openquartz.easyarchive.core.expr.cmd.Environment;
import com.openquartz.easyarchive.core.expr.cmd.Result;

/**
 * 直接取模
 * 表达式：{mod 取模数 源数}
 * 说明：取模数、源数都要求为正整数
 */
public class ModExecutor implements CommandExecutor {

    private static final int MOD_PARAM_INDEX = 0;
    private static final int SOURCE_PARAM_INDEX = 1;
    private static final int REQUIRED_PARAM_SIZE = 2;

    @Override
    public Result exec(Command command) {
        long mod = Long.parseLong(command.indexOfParam(MOD_PARAM_INDEX));
        long source = Long.parseLong(command.indexOfParam(SOURCE_PARAM_INDEX));
        return Result.success(String.valueOf(source % mod));
    }

    @Override
    public void validate(Command command) {
        Asserts.notNull(command, CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.notNull(command.getParams(), CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.isTrue(command.getParams().size() >= REQUIRED_PARAM_SIZE, CommonErrorCode.PARAM_ILLEGAL_ERROR);

        String modParam = command.indexOfParam(MOD_PARAM_INDEX);
        String sourceParam = command.indexOfParam(SOURCE_PARAM_INDEX);
        Asserts.notEmpty(modParam, CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.notEmpty(sourceParam, CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.isTrue(isPositiveInteger(modParam), CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.isTrue(isPositiveInteger(sourceParam), CommonErrorCode.PARAM_ILLEGAL_ERROR);
    }

    @Override
    public void init(Environment environment) {

    }

    private boolean isPositiveInteger(String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return Long.parseLong(value) > 0;
    }
}
