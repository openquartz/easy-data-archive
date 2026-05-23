package com.openquartz.easyarchive.core.rule;

import java.util.List;

/**
 * TableRuleLoader
 *
 * @author svnee
 */
public interface TableRuleLoader {

    /**
     * load table rule
     *
     * @return table rule
     */
    List<ArchiveGroupItem> load();

}
