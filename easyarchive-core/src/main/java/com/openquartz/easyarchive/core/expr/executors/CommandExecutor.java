package com.openquartz.easyarchive.core.expr.executors;

import com.openquartz.easyarchive.core.expr.cmd.Environment;
import com.openquartz.easyarchive.core.expr.cmd.Command;
import com.openquartz.easyarchive.core.expr.cmd.Result;

/**
 * @author svnee
 */
public interface CommandExecutor {

    /**
     * 执行
     */
    Result exec(Command command);

    /**
     * 校验
     */
    void validate(Command command);

    /**
     * 初始化
     */
    void init(Environment environment);
}
