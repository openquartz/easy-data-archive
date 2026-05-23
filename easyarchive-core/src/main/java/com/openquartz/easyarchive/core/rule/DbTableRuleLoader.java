package com.openquartz.easyarchive.core.rule;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.jdbc.SqlRunner;

/**
 * Db table rule loader
 *
 * @author svnee
 */
@Slf4j
public class DbTableRuleLoader implements TableRuleLoader {

    private final String connectionStr;
    private final String tableName;

    public DbTableRuleLoader(String connectionStr, String tableName) {
        this.tableName = tableName;
        this.connectionStr = connectionStr;
    }

    @Override
    @SneakyThrows
    public List<ArchiveGroupItem> load() {
        String sql = "SELECT * FROM " + this.tableName;
//
//        try (Connection connection = DriverManager.getConnection(this.connectionStr)) {
//            SqlRunner runner = new SqlRunner(connection);
//            List<Map<String, Object>> data = runner.selectAll(sql);
//            Class<TableRule> clazz = TableRule.class;
//            List<TableRule> configs = new ArrayList<>();
//            for (Map<String, Object> row : data) {
//                TableRule entity = new TableRule();
//                Field[] fields = clazz.getDeclaredFields();
//                for (Field field : fields) {
//                    field.setAccessible(true);
//                    field.set(entity, row.get(field.getName().toUpperCase(Locale.ENGLISH)));
//                }
//                configs.add(entity);
//            }
//
//            return configs.stream().filter(TableRule::valid).collect(Collectors.toList());
//        }
        return Collections.emptyList();
    }
}
