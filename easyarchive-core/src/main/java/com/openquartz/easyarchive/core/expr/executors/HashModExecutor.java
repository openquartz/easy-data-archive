package com.openquartz.easyarchive.core.expr.executors;

import com.openquartz.easyarchive.common.exception.Asserts;
import com.openquartz.easyarchive.common.exception.CommonErrorCode;
import com.openquartz.easyarchive.core.expr.cmd.Command;
import com.openquartz.easyarchive.core.expr.cmd.Environment;
import com.openquartz.easyarchive.core.expr.cmd.Result;

import java.util.List;

/**
 * hash 并取模
 * 表达式：{hash_mod 取模数 数据字符串}
 * 说明：取模数 要求为 正整数，数据字符串为1..n 参数的拼接字符串
 */
public class HashModExecutor implements CommandExecutor {

    private static final int MOD_PARAM_INDEX = 0;
    private static final int FIRST_DATA_PARAM_INDEX = 1;
    private static final int MIN_PARAM_SIZE = 2;
    private static final int POSITIVE_NUMBER_MIN = 1;

    @Override
    public Result exec(Command command) {
        int mod = Integer.parseInt(command.indexOfParam(MOD_PARAM_INDEX));
        String input = String.join("", command.getParams().subList(FIRST_DATA_PARAM_INDEX, command.getParams().size()));
        int result = Math.floorMod(input.hashCode(), mod);
        return Result.success(String.valueOf(result));
    }

    @Override
    public void validate(Command command) {
        Asserts.notNull(command, CommonErrorCode.PARAM_ILLEGAL_ERROR);
        List<String> params = command.getParams();

        Asserts.notNull(params, CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.isTrue(params.size() >= MIN_PARAM_SIZE, CommonErrorCode.PARAM_ILLEGAL_ERROR);

        String modParam = command.indexOfParam(MOD_PARAM_INDEX);
        Asserts.notEmpty(modParam, CommonErrorCode.PARAM_ILLEGAL_ERROR);

        Asserts.isTrue(isPositiveInteger(modParam), CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.isTrue(Integer.parseInt(modParam) >= POSITIVE_NUMBER_MIN, CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.notEmpty(command.indexOfParam(FIRST_DATA_PARAM_INDEX), CommonErrorCode.PARAM_ILLEGAL_ERROR);
    }

    @Override
    public void init(Environment environment) {

    }

    private boolean isPositiveInteger(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }
}
