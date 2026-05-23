package com.openquartz.easyarchive.common.api;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import com.openquartz.easyarchive.common.api.model.DataRecord;

/**
 * Cleaner
 *
 * @author svnee
 */
public interface Cleaner extends Closeable {

    /**
     * clean data
     *
     * @param data data
     */
    default void clean(DataRecord data) {
        clean(Collections.singletonList(data));
    }

    /**
     * clean record
     *
     * @param dataList data list
     */
    void clean(List<DataRecord> dataList);
}
