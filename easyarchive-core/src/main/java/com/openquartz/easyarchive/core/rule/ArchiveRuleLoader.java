package com.openquartz.easyarchive.core.rule;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItem;

import java.util.List;

/**
 * TableRuleLoader
 *
 * @author svnee
 */
public interface ArchiveRuleLoader {

    /**
     * load table rule
     *
     * @return table rule
     */
    List<ArchiveGroupItem> load();

}
