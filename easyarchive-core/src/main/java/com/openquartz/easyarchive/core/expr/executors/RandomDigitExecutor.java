package com.openquartz.easyarchive.core.expr.executors;

import com.openquartz.easyarchive.core.expr.cmd.Environment;
import com.openquartz.easyarchive.core.expr.cmd.Command;
import com.openquartz.easyarchive.core.expr.cmd.Result;
import com.openquartz.easyarchive.common.exception.Asserts;
import com.openquartz.easyarchive.common.exception.CommonErrorCode;
import com.openquartz.easyarchive.common.util.StringUtils;
import java.util.Random;

/**
 * 随机数字
 * 语法：{rand_n 随机长度}
 *
 * @author svnee
 */
public class RandomDigitExecutor implements CommandExecutor {

    private static final Random RANDOM = new Random();

    @Override
    public Result exec(Command command) {
        // 位数
        int count = Integer.parseInt(command.getFirstParam());
        StringBuilder sb = new StringBuilder(count);
        for (int i = 1; i <= count; i++) {
            sb.append(randGetSingleDigit());
        }
        return Result.success(sb.toString());
    }

    private int randGetSingleDigit() {
        return RANDOM.nextInt(10);
    }

    @Override
    public void validate(Command command) {
        Asserts.notNull(command, CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.notNull(command.getFirstParam(), CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.isTrue(StringUtils.isNumeric(command.getFirstParam()), CommonErrorCode.PARAM_ILLEGAL_ERROR);
    }

    @Override
    public void init(Environment environment) {

    }
}
