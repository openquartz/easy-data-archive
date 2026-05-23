package com.openquartz.easyarchive.common.api.model;

import java.util.Map;

/**
 * Data Record
 *
 * @author svnee
 **/
public class DataRecord {

    /**
     * key: column, value: columnValue
     */
    private Map<String, Object> data;

    public DataRecord(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
