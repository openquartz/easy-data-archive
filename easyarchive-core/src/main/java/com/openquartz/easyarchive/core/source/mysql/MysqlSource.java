package com.openquartz.easyarchive.core.source.mysql;

import com.google.common.base.Joiner;
import com.openquartz.easyarchive.common.api.model.*;
import com.openquartz.easyarchive.common.constant.Constants;
import com.openquartz.easyarchive.common.util.ExceptionUtils;
import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.openquartz.easyarchive.core.connection.ConnectionFactory;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import com.openquartz.easyarchive.common.api.PageSource;
import com.openquartz.easyarchive.common.util.CollectionUtils;
import com.openquartz.easyarchive.common.util.StringUtils;
import org.apache.ibatis.jdbc.SqlRunner;

/**
 * 源表数据读取
 *
 * @author svnee
 */
@Slf4j
public class MysqlSource implements PageSource, Closeable {

    private static final String SELECT_ALL_FROM_WHERE_IN_TEMPLATE = "SELECT * FROM %s WHERE %s IN (%s)";
    private static final String DELETE_FROM_WHERE_IN_TEMPLATE = "DELETE FROM %s WHERE %s IN (%s)";

    private final ArchiveConnection archiveConnection;
    @Getter
    private final Long groupId;
    @Getter
    private final TableInfo tableInfo;
    private final String fetchSql;
    private final boolean enableDelete;
    // 删除条件
    private final String deleteWhere;

    private SqlRunner runner;
    private Connection connection;

    private final FetchDataRecordByIdsFunction function;

    public MysqlSource(ArchiveConnection archiveConnection,
                       Long groupId,
                       TableInfo tableInfo,
                       String fetchSql,
                       boolean enableDelete,
                       String deleteWhere) {

        this.archiveConnection = archiveConnection;
        this.groupId = groupId;
        this.tableInfo = tableInfo;
        this.fetchSql = fetchSql;
        this.enableDelete = enableDelete;
        this.deleteWhere = deleteWhere;

        this.function = ids -> {
            try {
                String fetDataSql = String.format(SELECT_ALL_FROM_WHERE_IN_TEMPLATE,
                    tableInfo.getTableName(),
                    tableInfo.getIdColum(),
                    Joiner.on(Constants.COMMA).join(ids));

                List<Map<String, Object>> list = runner.selectAll(fetDataSql);
                return list.stream().map(DataRecord::new).collect(Collectors.toList());
            } catch (Exception ex) {
                return ExceptionUtils.rethrow(ex);
            }
        };
    }


    /**
     * 开始读取
     *
     * @param start 开始时间
     * @param end 结束时间
     * @param exePage 执行页码
     * @param maxLoadRows 最大一次加载行数
     * @param interval 间隔
     */
    @Override
    @SneakyThrows
    public DataIterator read(Object start, Object end, Integer exePage, int maxLoadRows, int interval) {

        if (this.runner == null) {
            Connection connection = ConnectionFactory.create(archiveConnection);
            this.runner = new SqlRunner(connection);
        }

        String splitterFetchSql;
        if (this.enableDelete) {
            splitterFetchSql = this.fetchSql + " limit " + maxLoadRows;
        } else {
            splitterFetchSql = this.fetchSql + " order by " + tableInfo.getIdColum()
                + " limit " + (maxLoadRows * exePage) + Constants.COMMA + maxLoadRows;
        }
        List<Long> ids = resolveId(splitterFetchSql, start, end);
        if (CollectionUtils.isEmpty(ids)) {
            return new EmptyDataIterator();
        } else {
            return new StepIdDataIterator(ids, interval, function);
        }
    }

    private List<Long> resolveId(String fetchSql, Object start, Object end) throws SQLException {
        List<Map<String, Object>> fetchIds = runner.selectAll(fetchSql);
        return fetchIds.stream()
            .map(this::resolveRecordId)
            .map(IdResolver::resolve)
            .collect(Collectors.toList());
    }

    /**
     * 删除数据
     */
    @Override
    @SneakyThrows
    public void clean(List<DataRecord> data) {

        if (CollectionUtils.isEmpty(data) || !this.enableDelete) {
            return;
        }

        List<String> ids = data.stream()
            .map(DataRecord::getData)
            .map(this::resolveRecordId)
            .map(String::valueOf)
            .collect(Collectors.toList());
        String sql = String.format(DELETE_FROM_WHERE_IN_TEMPLATE,
            this.getTableInfo().getTableName(),
            tableInfo.getIdColum(),
            String.join(Constants.COMMA, ids));
        if (StringUtils.isNotBlank(deleteWhere)) {
            sql = sql + "\n AND ";
            sql = sql + "(";
            sql = sql + deleteWhere;
            sql = sql + ")";
        }

        int r = runner.delete(sql);
        log.info("[MysqlSource#clean] delete {} rows from {}", r, tableInfo.getTableName());
    }

    @Override
    public void close() {
        if (runner != null) {
            runner.closeConnection();
        }
    }

    private Object resolveRecordId(Map<String, Object> row) {
        String idColumn = tableInfo.getIdColum();
        Object value = row.get(idColumn);
        if (value == null) {
            value = row.get(idColumn.toUpperCase());
        }
        if (value == null) {
            value = row.get(idColumn.toLowerCase());
        }
        if (value == null) {
            throw new NumberFormatException("Id 无法转换为Long，idColumn=" + idColumn + ", row=" + row);
        }
        return value;
    }
}
