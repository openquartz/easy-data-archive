package com.openquartz.easyarchive.core.sink.mysql;

import com.google.common.base.Stopwatch;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.openquartz.easyarchive.common.util.ExceptionUtils;
import com.openquartz.easyarchive.core.connection.ConnectionFactory;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.expr.ExpressionService;
import com.openquartz.easyarchive.core.expr.cmd.AssignExtParam;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import com.openquartz.easyarchive.common.api.Sink;
import com.openquartz.easyarchive.common.api.model.DataRecord;
import com.openquartz.easyarchive.common.constant.Constants;
import com.openquartz.easyarchive.common.util.CollectionUtils;

/**
 * MysqlSink
 *
 * @author svnee
 */
@Slf4j
public class MysqlSink implements Sink, Closeable {

    private static final int BATCH_FLUSH_SIZE = 500;

    private final ArchiveConnection archiveConnection;
    private Connection connection;
    private String[] columnCache;

    /**
     * 表名是否为纯静态（不含表达式）
     */
    private final boolean staticTable;

    /**
     * 静态表名快路径缓存：实际表名 → 预编译 SQL
     */
    private final Map<String, String> sqlCache = new HashMap<>();

    @Getter
    private final String tableName;

    public MysqlSink(ArchiveConnection archiveConnection, String tableName) {
        this.archiveConnection = archiveConnection;
        this.tableName = tableName;
        this.staticTable = ExpressionService.EXPRESSION_PATTERN.matcher(tableName).find();
    }

    @SneakyThrows
    @Override
    public void write(List<DataRecord> dataList) {
        if (CollectionUtils.isEmpty(dataList)) {
            return;
        }
        if (connection == null) {
            connection = ConnectionFactory.create(archiveConnection);
        }
        Stopwatch stopwatch = Stopwatch.createStarted();
        String[] columns = resolveColumns(dataList);

        if (staticTable) {
            // 快路径：跳过 stream/grouping，直接批量写入
            String insertSql = getOrBuildSql(tableName, columns);
            executeBatch(dataList, insertSql, columns);
        } else {
            // 慢路径：按表达式表名分组写入
            writeByExpression(dataList, columns);
        }

        stopwatch.stop();
        log.info("[MysqlSink#write] write {} rows into {},take {}ms", dataList.size(), tableName,
                stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    /**
     * 快路径：单表直接批处理，无 stream/grouping 开销
     */
    private void executeBatch(List<DataRecord> dataList, String insertSql, String[] columns) {
        try (PreparedStatement preSmt = connection.prepareStatement(insertSql)) {
            int j = 0;
            for (DataRecord dateItem : dataList) {
                for (int i = 0; i < columns.length; i++) {
                    preSmt.setObject(1 + i, dateItem.getData().get(columns[i]));
                }
                preSmt.addBatch();
                ++j;
                if (j % BATCH_FLUSH_SIZE == 0) {
                    preSmt.executeBatch();
                }
            }
            if (j % BATCH_FLUSH_SIZE != 0) {
                preSmt.executeBatch();
            }
        } catch (Exception ex) {
            ExceptionUtils.rethrow(ex);
        }
    }

    /**
     * 慢路径：按表达式解析后的实际表名分组，每组一个 PreparedStatement
     */
    private void writeByExpression(List<DataRecord> dataList, String[] columns) {
        Map<String, List<DataRecord>> actualTable2DataList = new HashMap<>(4);
        for (DataRecord record : dataList) {
            String actualTable = ExpressionService.getInstance().parse(tableName, AssignExtParam.create().set("root", record));
            actualTable2DataList.computeIfAbsent(actualTable, k -> new java.util.ArrayList<>()).add(record);
        }

        for (Map.Entry<String, List<DataRecord>> entry : actualTable2DataList.entrySet()) {
            String actualTable = entry.getKey();
            List<DataRecord> records = entry.getValue();
            String insertSql = getOrBuildSql(actualTable, columns);
            executeBatch(records, insertSql, columns);
        }
    }

    private String[] resolveColumns(List<DataRecord> rst) {
        if (columnCache == null) {
            Map<String, Object> firstRow = rst.get(0).getData();
            columnCache = new String[firstRow.size()];
            int j = 0;
            for (String s : firstRow.keySet()) {
                columnCache[j] = s;
                j++;
            }
        }
        return columnCache;
    }

    private String getOrBuildSql(String actualTable, String[] columns) {
        return sqlCache.computeIfAbsent(actualTable, table -> buildInsertSql(table, columns));
    }

    private String buildInsertSql(String actualTable, String[] columns) {
        String[] commaList = new String[columns.length];
        Arrays.fill(commaList, "?");

        return "REPLACE INTO "
                + "\n"
                + actualTable
                + "("
                + String.join(Constants.COMMA, columns)
                + ")"
                + "VALUES"
                + "("
                + String.join(Constants.COMMA, commaList)
                + ")";
    }

    @Override
    public void close() {
        try {
            if (this.connection != null) {
                this.connection.close();
            }
        } catch (SQLException ignoreException) {
            //ignore
        }
    }
}
