package com.openquartz.easyarchive.common.statistic;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 分组
 *
 * @author svnee
 */
@Data
@Accessors(chain = true)
public class ArchiveStaticGroupInfo {

    /**
     * 分组
     */
    private String group;

    /**
     * 统计信息
     */
    private List<ArchiveStatisticInfo> infoList;

}
