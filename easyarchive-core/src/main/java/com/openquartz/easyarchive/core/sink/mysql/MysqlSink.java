package com.openquartz.easyarchive.core.sink.mysql;

import com.google.common.base.Stopwatch;
import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.openquartz.easyarchive.core.connection.ConnectionFactory;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import com.openquartz.easyarchive.common.api.Sink;
import com.openquartz.easyarchive.common.api.model.DataRecord;
import com.openquartz.easyarchive.common.util.CollectionUtils;

/**
 * MysqlSink
 *
 * @author svnee
 */
@Slf4j
public class MysqlSink implements Sink, Closeable {

    private static final String COMMA = ",";
    private static final String QUESTION_MASK = "?";

    private final ArchiveConnection archiveConnection;
    private Connection connection;
    private String[] columnCache;

    @Getter
    private final String tableName;

    public MysqlSink(ArchiveConnection archiveConnection, String tableName) {
        this.archiveConnection = archiveConnection;
        this.tableName = tableName;
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
        String insertSql = buildInsertSql(columns);

        try (PreparedStatement preSmt = connection.prepareStatement(insertSql)) {
            int j = 0;
            for (DataRecord dateItem : dataList) {
                for (int i = 0; i < columns.length; i++) {
                    preSmt.setObject(1 + i, dateItem.getData().get(columns[i]));
                }
                preSmt.addBatch();
                ++j;
                if (j % 500 == 0) {
                    int[] r = preSmt.executeBatch();
                }
            }
            if (j % 500 != 0) {
                int[] r = preSmt.executeBatch();
            }
        }
        stopwatch.stop();
        log.info("[MysqlSink#write] write {} rows into {},take {}ms", dataList.size(), tableName,
            stopwatch.elapsed(TimeUnit.MILLISECONDS));
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

    private String buildInsertSql(String[] columns) {
        String[] commaList = new String[columns.length];
        Arrays.fill(commaList, QUESTION_MASK);

        return "REPLACE INTO "
            + "\n"
            + tableName
            + "("
            + String.join(COMMA, columns)
            + ")"
            + "VALUES"
            + "("
            + String.join(COMMA, commaList)
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
