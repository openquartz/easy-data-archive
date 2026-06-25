package com.openquartz.easyarchive.core.rule;

import com.openquartz.easyarchive.core.connection.ConnectionFactory;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.property.ArchiveConfig;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItem;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime;
import com.openquartz.easyarchive.common.util.ExceptionUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Db table rule loader
 *
 * @author svnee
 */
@Slf4j
public class DbArchiveRuleLoader implements ArchiveRuleLoader {

    private final ArchiveConfig archiveConfig;
    private final ArchiveConnection configConnection;

    public DbArchiveRuleLoader(ArchiveConfig archiveConfig, ArchiveConnection configConnection) {
        this.archiveConfig = archiveConfig;
        this.configConnection = configConnection;
    }

    @Override
    @SneakyThrows
    public List<ArchiveGroupItem> load() {
        List<ArchiveGroupItem> rules = new ArrayList<>();

        try (Connection connection = ConnectionFactory.create(configConnection)) {
            String sql = "SELECT * FROM " + archiveConfig.getConfigTable() + " WHERE enable_status = 0";

            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    ArchiveGroupItem rule = parseRuleFromResultSet(rs);
                    if (rule != null && rule.valid()) {
                        rules.add(rule);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[DbArchiveRuleLoader#load] load rules error", e);
            ExceptionUtils.rethrow(e);
        }

        log.info("[DbArchiveRuleLoader#load] load {} rules", rules.size());
        return rules;
    }

    private ArchiveGroupItem parseRuleFromResultSet(ResultSet rs) throws Exception {
        String archiveType = rs.getString("archive_type");

        if ("TIME".equals(archiveType)) {
            return parseTimeRule(rs);
        } else if ("ID".equals(archiveType)) {
            return parseIdRule(rs);
        }

        return null;
    }

    private ArchiveGroupItemByTime parseTimeRule(ResultSet rs) throws Exception {
        ArchiveGroupItemByTime rule = new ArchiveGroupItemByTime();
        populateCommonFields(rule, rs);

        rule.setStartTime(rs.getTimestamp("start_time"));
        rule.setKeepDay(rs.getInt("keep_day"));
        rule.setStepMinutes(rs.getInt("step_minutes"));
        rule.setStepCount(rs.getInt("step_count"));

        return rule;
    }

    private ArchiveGroupItemById parseIdRule(ResultSet rs) throws Exception {
        ArchiveGroupItemById rule = new ArchiveGroupItemById();
        populateCommonFields(rule, rs);

        rule.setStartId(rs.getString("start_id"));
        rule.setEndId(rs.getString("end_id"));
        rule.setStepCount(rs.getInt("step_count"));
        rule.setStepRounds(rs.getLong("step_rounds"));

        return rule;
    }

    private void populateCommonFields(Object ruleObj, ResultSet rs) throws Exception {
        if (ruleObj instanceof ArchiveGroupItemByTime) {
            ArchiveGroupItemByTime rule = (ArchiveGroupItemByTime) ruleObj;
            rule.setId(rs.getLong("id"));
            rule.setGroupId(rs.getLong("group_id"));
            rule.setSourceTable(rs.getString("source_table"));
            rule.setTargetTable(rs.getString("target_table"));
            rule.setPriority(rs.getInt("priority"));
            rule.setFetchSql(rs.getString("fetch_sql"));
            rule.setDeleteWhere(rs.getString("delete_where"));
            rule.setEnableClean(rs.getInt("enable_clean"));
            rule.setEnableWrite(rs.getInt("enable_write"));
            rule.setPauseMs(rs.getInt("pause_ms"));
            rule.setIdColumn(rs.getString("id_column"));
        } else if (ruleObj instanceof ArchiveGroupItemById) {
            ArchiveGroupItemById rule = (ArchiveGroupItemById) ruleObj;
            rule.setId(rs.getLong("id"));
            rule.setGroupId(rs.getLong("group_id"));
            rule.setSourceTable(rs.getString("source_table"));
            rule.setTargetTable(rs.getString("target_table"));
            rule.setPriority(rs.getInt("priority"));
            rule.setFetchSql(rs.getString("fetch_sql"));
            rule.setDeleteWhere(rs.getString("delete_where"));
            rule.setEnableClean(rs.getInt("enable_clean"));
            rule.setEnableWrite(rs.getInt("enable_write"));
            rule.setPauseMs(rs.getInt("pause_ms"));
            rule.setIdColumn(rs.getString("id_column"));
        }
    }
}
