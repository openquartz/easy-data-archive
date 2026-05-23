package com.openquartz.easyarchive.source.mysql;

import com.google.common.base.Joiner;
import com.openquartz.easyarchive.common.util.ExceptionUtils;
import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.jdbc.SqlRunner;
import com.openquartz.easyarchive.common.api.connection.ConnectionFactory;
import com.openquartz.easyarchive.common.api.model.IdResolver;
import com.openquartz.easyarchive.common.api.PageSource;
import com.openquartz.easyarchive.common.api.model.DataIterator;
import com.openquartz.easyarchive.common.api.model.DataRecord;
import com.openquartz.easyarchive.common.api.model.EmptyDataIterator;
import com.openquartz.easyarchive.common.api.model.FetchDataRecordByIdsFunction;
import com.openquartz.easyarchive.common.api.model.StepIdDataIterator;
import com.openquartz.easyarchive.common.util.CollectionUtils;
import com.openquartz.easyarchive.common.util.StringUtils;

/**
 * 源表数据读取
 *
 * @author svnee
 */
@Slf4j
public class MysqlSource implements PageSource, Closeable {

    private static final String COMMA = ",";
    private static final String ID_COLUMN = "ID";

    private final String connectionUrl;
    @Getter
    private final Long group;
    @Getter
    private final String tableName;
    private final String fetchSql;
    private final boolean enableDelete;
    // 删除条件
    private final String deleteWhere;

    private SqlRunner runner;

    private final FetchDataRecordByIdsFunction function;

    public MysqlSource(String connectionUrl, Long group, String tableName, String fetchSql, boolean enableDelete,
        String deleteWhere) {
        this.connectionUrl = connectionUrl;
        this.group = group;
        this.tableName = tableName;
        this.fetchSql = fetchSql;
        this.enableDelete = enableDelete;
        this.deleteWhere = deleteWhere;

        this.function = ids -> {
            try {
                String fetDataSql = "SELECT * FROM "
                    + tableName
                    + " WHERE id IN ("
                    + Joiner.on(COMMA).join(ids)
                    + ")";

                List<Map<String, Object>> list = runner.selectAll(fetDataSql);
                return list.stream().map(DataRecord::new).collect(Collectors.toList());
            } catch (Exception ex) {
                return ExceptionUtils.rethrow(ex);
            }
        };
    }

    public MysqlSource(String connectionUrl, Long group, String tableName, String fetchSql, boolean enableDelete) {
        this(connectionUrl, group, tableName, fetchSql, enableDelete, StringUtils.EMPTY);
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
            Connection connection = ConnectionFactory.create(this.connectionUrl);
            this.runner = new SqlRunner(connection);
        }

        String splitterFetchSql;
        if (this.enableDelete) {
            splitterFetchSql = this.fetchSql + " limit " + maxLoadRows;
        } else {
            splitterFetchSql = this.fetchSql + " order by id limit " + maxLoadRows * exePage + COMMA + maxLoadRows;
        }
        List<Long> ids = resolveId(splitterFetchSql, start, end);
        if (CollectionUtils.isEmpty(ids)) {
            return new EmptyDataIterator();
        } else {
            return new StepIdDataIterator(ids, interval, function);
        }
    }

    private List<Long> resolveId(String fetchSql, Object start, Object end) throws SQLException {
        List<Map<String, Object>> fetchIds = runner.selectAll(fetchSql, start, end);
        return fetchIds.stream().map(t -> IdResolver.resolve(t.get(ID_COLUMN))).collect(Collectors.toList());
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

        List<String> ids = data.stream().map(t -> t.getData().get(ID_COLUMN).toString()).collect(Collectors.toList());
        String sql = "DELETE FROM "
            + this.tableName
            + " WHERE ID IN ("
            + String.join(COMMA, ids)
            + ")";
        if (StringUtils.isNotBlank(deleteWhere)) {
            sql = sql + "\n AND ";
            sql = sql + "(";
            sql = sql + deleteWhere;
            sql = sql + ")";
        }

        int r = runner.delete(sql);
        log.info("[MysqlSource#clean] delete {} rows from {}", r, tableName);
    }

    @Override
    public void close() {
        runner.closeConnection();
    }
}