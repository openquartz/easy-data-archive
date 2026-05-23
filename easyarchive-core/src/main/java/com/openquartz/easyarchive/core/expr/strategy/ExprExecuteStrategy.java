package com.openquartz.easyarchive.core.expr.strategy;

import com.openquartz.easyarchive.core.expr.cmd.Environment;

/**
 * @author svnee
 */
public interface ExprExecuteStrategy {

    /**
     * exe expression
     *
     * @param expr expression
     * @param environment env
     * @return exe-result
     */
    String exec(String expr, Environment environment);
}
