package com.openquartz.easyarchive.core.expr.executors;


import com.openquartz.easyarchive.common.exception.EasyArchiveException;
import com.google.common.collect.Sets;
import com.openquartz.easyarchive.core.expr.cmd.Command;
import com.openquartz.easyarchive.core.expr.cmd.Environment;
import com.openquartz.easyarchive.core.expr.cmd.Result;
import com.openquartz.easyarchive.common.exception.Asserts;
import com.openquartz.easyarchive.common.exception.CommonErrorCode;
import com.openquartz.easyarchive.core.exception.CoreErrorCode;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;


/**
 * 功能：时间增加
 * 格式为：time_add 数值 单位 输出格式
 * 例如：time_add 1 Y yyyy-MM-dd
 */
public class TimeAddExecutor extends TimeFormatExecutor {

    /**
     * 时间单位
     */
    private static final Set<String> TIME_UNIT = Sets.newHashSet("Y", "M", "D", "H", "m", "s");

    @Override
    public Result exec(Command command) {

        LocalDateTime localTime = LocalDateTime.now();

        // 增加时间操作
        localTime = addTimeOperate(command, localTime);
        return Result.success(formatTime(localTime, command.getParams().get(2)));
    }

    private static LocalDateTime addTimeOperate(Command command, LocalDateTime localTime) {
        long amount = Long.parseLong(command.getParams().get(0));
        return SupportedTimeUnit.fromCode(command.getParams().get(1)).add(localTime, amount);
    }

    @Override
    public void validate(Command command) {
        Asserts.notNull(command, CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.notNull(command.getParams(), CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.isTrue(!command.getParams().isEmpty(), CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.isTrue(Objects.nonNull(command.getParams().get(0)), CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.isTrue(TIME_UNIT.contains(command.getParams().get(1)), CommonErrorCode.PARAM_ILLEGAL_ERROR);
        checkTimePattern(command.getParams().get(2));
    }

    @Override
    public void init(Environment environment) {

    }

    private enum SupportedTimeUnit {
        YEAR("Y") {
            @Override
            LocalDateTime add(LocalDateTime localTime, long amount) {
                return localTime.plusYears(amount);
            }
        },
        MONTH("M") {
            @Override
            LocalDateTime add(LocalDateTime localTime, long amount) {
                return localTime.plusMonths(amount);
            }
        },
        DAY("D") {
            @Override
            LocalDateTime add(LocalDateTime localTime, long amount) {
                return localTime.plusDays(amount);
            }
        },
        HOUR("H") {
            @Override
            LocalDateTime add(LocalDateTime localTime, long amount) {
                return localTime.plusHours(amount);
            }
        },
        MINUTE("m") {
            @Override
            LocalDateTime add(LocalDateTime localTime, long amount) {
                return localTime.plusMinutes(amount);
            }
        },
        SECOND("s") {
            @Override
            LocalDateTime add(LocalDateTime localTime, long amount) {
                return localTime.plusSeconds(amount);
            }
        };

        private final String code;

        SupportedTimeUnit(String code) {
            this.code = code;
        }

        abstract LocalDateTime add(LocalDateTime localTime, long amount);

        private static SupportedTimeUnit fromCode(String code) {
            return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> EasyArchiveException.withPlaceholders(
                    CoreErrorCode.TIME_UNIT_UNSUPPORTED, code));
        }
    }

}
