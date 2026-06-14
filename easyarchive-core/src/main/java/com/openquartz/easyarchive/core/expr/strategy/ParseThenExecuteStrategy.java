package com.openquartz.easyarchive.core.expr.strategy;

import com.openquartz.easyarchive.common.exception.CommonErrorCode;
import com.openquartz.easyarchive.core.expr.cmd.Environment;
import com.openquartz.easyarchive.core.expr.cmd.Command;
import com.openquartz.easyarchive.core.expr.cmd.CommandNode;
import com.openquartz.easyarchive.core.expr.cmd.CommandTreeParser;
import com.openquartz.easyarchive.core.expr.cmd.Result;
import com.openquartz.easyarchive.core.expr.exception.CommandExceptionCode;
import com.openquartz.easyarchive.core.expr.executors.CommandExecutor;
import com.openquartz.easyarchive.common.constant.Constants;
import com.openquartz.easyarchive.common.exception.Asserts;
import com.openquartz.easyarchive.common.util.CollectionUtils;
import com.openquartz.easyarchive.common.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * @author svnee
 */
@Slf4j
public class ParseThenExecuteStrategy implements ExprExecuteStrategy {

    @Override
    public String exec(String expr, Environment environment) {
        CommandNode rootNode = CommandTreeParser.parseExpr(expr);
        return recursiveExec(rootNode, environment);
    }

    /**
     * 递归执行CommandNode命令
     *
     * @param node node
     * @param environment environment
     * @return execute result
     */
    private String recursiveExec(CommandNode node, Environment environment) {
        if (!node.isCommand()) {
            return Constants.EMPTY;
        }

        String cmdName = node.getValue();
        CommandExecutor executor = environment.getExecutor(cmdName);
        Asserts.notNull(executor, CommonErrorCode.SEQUENCE_EXECUTOR_REGISTERED_ERROR, cmdName);

        List<String> params = new ArrayList<>();
        List<CommandNode> children = node.getChildren();

        if (CollectionUtils.isNotEmpty(children)) {
            StringBuilder tmp = new StringBuilder();
            boolean joinFlag = false;
            for (CommandNode child : children) {
                if (child.isCommand()) {
                    if (!joinFlag) {
                        String str = tmp.toString();
                        if (StringUtils.isNotBlank(str)) {
                            params.add(tmp.toString());
                        }
                        tmp = new StringBuilder();
                    }
                    tmp.append(recursiveExec(child, environment));
                } else if (child.isParam()) {
                    if (!joinFlag) {
                        String str = tmp.toString();
                        if (StringUtils.isNotBlank(str)) {
                            params.add(tmp.toString());
                        }
                        tmp = new StringBuilder();
                    }
                    tmp.append(child.getValue());
                } else if (child.isJoin()) {
                    tmp.append(child.getValue());
                    joinFlag = true;
                }
            }
            String str = tmp.toString();
            if (StringUtils.isNotBlank(str)) {
                params.add(str);
            }
        }

        Command command = makeCommand(cmdName, params);
        executor.validate(command);
        Result result = executor.exec(command);
        Asserts.isTrue(result.isSuccess(), CommandExceptionCode.COMMAND_RUN_ERROR, command.getCmd(), command.getParams(), result.getErrorMsg());
        return result.getOutput();
    }

    public Command makeCommand(String cmd, List<String> params) {
        Command command = new Command();
        command.setCmd(cmd);
        command.setParams(params);
        return command;
    }

}
