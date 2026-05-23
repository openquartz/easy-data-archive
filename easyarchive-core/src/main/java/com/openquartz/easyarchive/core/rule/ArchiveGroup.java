package com.openquartz.easyarchive.core.rule;

import lombok.Data;

@Data
public class ArchiveGroup {

    /**
     * ID
     */
    private Integer id;

    /**
     * 分组名
     */
    private String group;

    /**
     * 源库ID
     */
    private Integer sourceConnectionId;

    /**
     * 目标库ID
     */
    private Integer targetConnectionId;
}
