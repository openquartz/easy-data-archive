package com.openquartz.easyarchive.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ArchiveRuleTypeEnum {
    ID("ID","ID"),
    TIME("TIME","时间"),
    ;
    private final String code;
    private final String desc;
}
