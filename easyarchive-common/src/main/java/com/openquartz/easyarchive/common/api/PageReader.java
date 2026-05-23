package com.openquartz.easyarchive.common.api;

import com.openquartz.easyarchive.common.api.model.DataIterator;

/**
 * page read
 *
 * @author svnee
 */
public interface PageReader extends Cloneable {

    /**
     * Read Record
     *
     * @param start start-time
     * @param end end-time
     * @param exePage exe-page
     * @param maxLoadRows offset
     * @param interval interval
     * @return data
     */
    DataIterator read(Object start, Object end, Integer exePage, int maxLoadRows, int interval);

}
