package com.openquartz.easyarchive.core.sink;

import com.openquartz.easyarchive.common.api.Sink;
import com.openquartz.easyarchive.common.api.model.DataRecord;

import java.io.IOException;
import java.util.List;

/**
 * EmptySink
 */
public class EmptySink implements Sink, Cloneable {

    @Override
    public void write(List<DataRecord> dataList) {

    }

    @Override
    public void close() {

    }
}
