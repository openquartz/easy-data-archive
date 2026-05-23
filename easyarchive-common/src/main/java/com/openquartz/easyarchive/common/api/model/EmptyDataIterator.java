package com.openquartz.easyarchive.common.api.model;

import java.util.Collections;
import java.util.List;

/**
 * empty data iterator
 *
 * @author svnee
 */
public class EmptyDataIterator implements DataIterator {

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public List<DataRecord> next() {
        return Collections.emptyList();
    }
}