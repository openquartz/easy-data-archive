package com.openquartz.easyarchive.common.api.model;

import java.util.List;

/**
 * DataIterator
 *
 * @author svnee
 */
public interface DataIterator {

    /**
     * has next data
     *
     * @return next data
     */
    boolean hasNext();

    /**
     * next data
     *
     * @return List<DataRecord>
     */
    List<DataRecord> next();
}