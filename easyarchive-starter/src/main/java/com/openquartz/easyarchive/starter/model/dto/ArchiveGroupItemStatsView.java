package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

@Data
public class ArchiveGroupItemStatsView {

    private Long totalCount;
    private Long enabledCount;
    private Long disabledCount;
    private Long idTypeCount;
    private Long timeTypeCount;
}
