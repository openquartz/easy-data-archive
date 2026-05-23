package com.openquartz.easyarchive.core.expr.executors;

import com.openquartz.easyarchive.common.exception.Asserts;
import com.openquartz.easyarchive.common.exception.CommonErrorCode;
import com.openquartz.easyarchive.common.util.ApplicationContextUtils;
import com.openquartz.easyarchive.common.util.ExceptionUtils;
import com.openquartz.easyarchive.common.util.MapUtils;
import com.openquartz.easyarchive.core.connection.ArchiveConnectionService;
import com.openquartz.easyarchive.core.connection.ConnectionFactory;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.expr.cmd.Command;
import com.openquartz.easyarchive.core.expr.cmd.Environment;
import com.openquartz.easyarchive.core.expr.cmd.Result;
import org.apache.ibatis.jdbc.SqlRunner;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 执行sql
 * 运行sql表达式为：{sql 数据连接源code sql}
 * 输出结果为所有值使用逗号分隔的连接串，要求只能查出一条，最好是有"limit 1" 条件
 */
public class SqlExecutor implements CommandExecutor {

    @Override
    public Result exec(Command command) {

        // 连接码
        String connectCode = command.getFirstParam();
        List<String> sqlParamsList = command.getParams();
        String sql = String.join(",", sqlParamsList.subList(1, sqlParamsList.size()));

        ArchiveConnectionService connectionService = ApplicationContextUtils.getContext().getBean(ArchiveConnectionService.class);
        ArchiveConnection archiveConnection = connectionService.getByConnectionCode(connectCode);
        try (Connection connection = ConnectionFactory.create(archiveConnection)) {

            SqlRunner sqlRunner = new SqlRunner(connection);
            try {
                Map<String, Object> resMap = sqlRunner.selectOne(sql);
                if (resMap == null || MapUtils.isEmpty(resMap)) {
                    return Result.success();
                }
                return Result.success(resMap.values().stream().map(Object::toString).collect(Collectors.joining(",")));
            } catch (Exception ex) {
                return ExceptionUtils.rethrow(ex);
            }
        } catch (Exception ex) {
            return ExceptionUtils.rethrow(ex);
        }
    }

    @Override
    public void validate(Command command) {
        Asserts.notNull(command, CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.notEmpty(command.getFirstParam(), CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.notEmpty(command.getSecondParam(), CommonErrorCode.PARAM_ILLEGAL_ERROR);
    }

    @Override
    public void init(Environment environment) {

    }
}
