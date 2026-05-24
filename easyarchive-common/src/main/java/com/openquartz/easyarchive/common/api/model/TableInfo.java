package com.openquartz.easyarchive.common.api.model;

import lombok.Data;

@Data
public class TableInfo {

    private String tableName;

    private String idColum;

    public static TableInfo of(String tableName, String idColum) {
        TableInfo tableInfo = new TableInfo();
        tableInfo.setTableName(tableName);
        tableInfo.setIdColum(idColum);
        return tableInfo;
    }
}
