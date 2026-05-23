package com.openquartz.easyarchive.common.api;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import com.openquartz.easyarchive.common.api.model.DataRecord;

/**
 * Write data to target database
 *
 * @author svnee
 */
public interface Writer extends Closeable {

    /**
     * write data list
     *
     * @param dataList dataList
     */
    void write(List<DataRecord> dataList);

    /**
     * write data
     *
     * @param data data
     */
    default void write(DataRecord data) {
        write(Collections.singletonList(data));
    }
}
