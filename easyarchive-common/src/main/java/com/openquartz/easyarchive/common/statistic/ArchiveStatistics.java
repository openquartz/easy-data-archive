package com.openquartz.easyarchive.common.statistic;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 归档统计
 *
 * @author svnee
 */
public class ArchiveStatistics {

    private ArchiveStatistics() {
    }

    /**
     * 统计信息
     */
    public static final Table<String, String, ArchiveStatisticInfo> STATISTIC_INFO_TABLE = HashBasedTable.create();

    /**
     * 如果缺席则创建
     *
     * @return 统计信息
     */
    public static synchronized ArchiveStatisticInfo createIfAbsent(String group, String fromTable) {
        ArchiveStatisticInfo statisticInfo = STATISTIC_INFO_TABLE.get(group.trim(), fromTable.trim());
        if (Objects.isNull(statisticInfo)) {
            statisticInfo = new ArchiveStatisticInfo(group, fromTable);
            STATISTIC_INFO_TABLE.put(group.trim(), fromTable.trim(), statisticInfo);
        }
        return statisticInfo;
    }

    /**
     * 返回所有的统计信息
     *
     * @return 统计信息
     */
    public static synchronized List<ArchiveStatisticInfo> getArchiveStatisticInfo() {
        return STATISTIC_INFO_TABLE.values().stream()
            .sorted((Comparator.comparing(ArchiveStatisticInfo::getGroup)))
            .collect(Collectors.toList());
    }

    /**
     * 清空
     */
    public static synchronized void clear() {
        try {
            STATISTIC_INFO_TABLE.clear();
        } catch (Exception ignored) {
        }
    }

}
