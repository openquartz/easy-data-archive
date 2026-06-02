package com.openquartz.easyarchive.starter.model.enums;

import com.openquartz.easyarchive.starter.model.dto.DatasourceTypeOption;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据源类型枚举
 */
public enum DatasourceTypeEnum {

    MYSQL("MYSQL", "MySQL");

    private final String code;
    private final String name;

    DatasourceTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static List<DatasourceTypeOption> toOptions() {
        return Arrays.stream(values())
                .map(item -> new DatasourceTypeOption(item.code, item.name))
                .collect(Collectors.toList());
    }
}
