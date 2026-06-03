package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime;
import com.openquartz.easyarchive.starter.annotation.OperationLog;
import com.openquartz.easyarchive.starter.model.dto.ApiResponse;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupItemSummary;
import com.openquartz.easyarchive.starter.service.ArchiveGroupItemByIdService;
import com.openquartz.easyarchive.starter.service.ArchiveGroupItemByTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/archive/groups/{groupId}/items")
@RequiredArgsConstructor
public class ArchiveGroupItemController {

    private static final String TYPE_ID = "ID";
    private static final String TYPE_TIME = "TIME";

    private final ArchiveGroupItemByIdService idService;
    private final ArchiveGroupItemByTimeService timeService;

    @GetMapping
    public ApiResponse<List<ArchiveGroupItemSummary>> list(@PathVariable Long groupId,
                                                           @RequestParam(required = false) Integer enableStatus) {
        List<ArchiveGroupItemSummary> summaries = new ArrayList<>();
        for (ArchiveGroupItemById item : idService.findByGroupId(groupId, enableStatus)) {
            summaries.add(summary(TYPE_ID, item.getId(), item.getGroupId(), item.getSourceTable(),
                    item.getTargetTable(), item.getPriority(), item.getStepCount(), item.getEnableWrite(),
                    item.getEnableClean(), item.getEnableStatus()));
        }
        for (ArchiveGroupItemByTime item : timeService.findByGroupId(groupId, enableStatus)) {
            summaries.add(summary(TYPE_TIME, item.getId(), item.getGroupId(), item.getSourceTable(),
                    item.getTargetTable(), item.getPriority(), item.getStepCount(), item.getEnableWrite(),
                    item.getEnableClean(), item.getEnableStatus()));
        }
        summaries.sort(Comparator.comparing(ArchiveGroupItemSummary::getPriority,
                Comparator.nullsLast(Integer::compareTo)));
        return ApiResponse.success(summaries);
    }

    @GetMapping("/id/{itemId}")
    public ApiResponse<ArchiveGroupItemById> getIdItem(@PathVariable Long groupId, @PathVariable Long itemId) {
        return ApiResponse.success(idService.findById(groupId, itemId));
    }

    @PostMapping("/id")
    @OperationLog(value = "新增分组项", module = "ARCHIVE_GROUP_ITEM_ID", action = "CREATE", button = "新增分组项")
    public ApiResponse<ArchiveGroupItemById> createIdItem(@PathVariable Long groupId,
                                                          @RequestBody ArchiveGroupItemById item) {
        return ApiResponse.success(idService.create(groupId, item));
    }

    @PutMapping("/id/{itemId}")
    @OperationLog(value = "编辑分组项", module = "ARCHIVE_GROUP_ITEM_ID", action = "UPDATE", button = "编辑分组项")
    public ApiResponse<ArchiveGroupItemById> updateIdItem(@PathVariable Long groupId,
                                                          @PathVariable Long itemId,
                                                          @RequestBody ArchiveGroupItemById item) {
        return ApiResponse.success(idService.update(groupId, itemId, item));
    }

    @PatchMapping("/id/{itemId}/status")
    @OperationLog(value = "修改分组项状态", module = "ARCHIVE_GROUP_ITEM_ID", action = "STATUS", button = "修改分组项状态")
    public ApiResponse<?> updateIdStatus(@PathVariable Long groupId,
                                         @PathVariable Long itemId,
                                         @RequestParam Integer enableStatus) {
        idService.updateStatus(groupId, itemId, enableStatus);
        return ApiResponse.success();
    }

    @DeleteMapping("/id/{itemId}")
    @OperationLog(value = "删除分组项", module = "ARCHIVE_GROUP_ITEM_ID", action = "DELETE", button = "删除分组项")
    public ApiResponse<?> deleteIdItem(@PathVariable Long groupId, @PathVariable Long itemId) {
        idService.delete(groupId, itemId);
        return ApiResponse.success();
    }

    @GetMapping("/time/{itemId}")
    public ApiResponse<ArchiveGroupItemByTime> getTimeItem(@PathVariable Long groupId, @PathVariable Long itemId) {
        return ApiResponse.success(timeService.findById(groupId, itemId));
    }

    @PostMapping("/time")
    @OperationLog(value = "新增分组项", module = "ARCHIVE_GROUP_ITEM_TIME", action = "CREATE", button = "新增分组项")
    public ApiResponse<ArchiveGroupItemByTime> createTimeItem(@PathVariable Long groupId,
                                                              @RequestBody ArchiveGroupItemByTime item) {
        return ApiResponse.success(timeService.create(groupId, item));
    }

    @PutMapping("/time/{itemId}")
    @OperationLog(value = "编辑分组项", module = "ARCHIVE_GROUP_ITEM_TIME", action = "UPDATE", button = "编辑分组项")
    public ApiResponse<ArchiveGroupItemByTime> updateTimeItem(@PathVariable Long groupId,
                                                              @PathVariable Long itemId,
                                                              @RequestBody ArchiveGroupItemByTime item) {
        return ApiResponse.success(timeService.update(groupId, itemId, item));
    }

    @PatchMapping("/time/{itemId}/status")
    @OperationLog(value = "修改分组项状态", module = "ARCHIVE_GROUP_ITEM_TIME", action = "STATUS", button = "修改分组项状态")
    public ApiResponse<?> updateTimeStatus(@PathVariable Long groupId,
                                           @PathVariable Long itemId,
                                           @RequestParam Integer enableStatus) {
        timeService.updateStatus(groupId, itemId, enableStatus);
        return ApiResponse.success();
    }

    @DeleteMapping("/time/{itemId}")
    @OperationLog(value = "删除分组项", module = "ARCHIVE_GROUP_ITEM_TIME", action = "DELETE", button = "删除分组项")
    public ApiResponse<?> deleteTimeItem(@PathVariable Long groupId, @PathVariable Long itemId) {
        timeService.delete(groupId, itemId);
        return ApiResponse.success();
    }

    private ArchiveGroupItemSummary summary(String itemType, Long id, Long groupId, String sourceTable,
                                            String targetTable, Integer priority, Integer stepCount,
                                            Integer enableWrite, Integer enableClean, Integer enableStatus) {
        ArchiveGroupItemSummary summary = new ArchiveGroupItemSummary();
        summary.setItemType(itemType);
        summary.setId(id);
        summary.setGroupId(groupId);
        summary.setSourceTable(sourceTable);
        summary.setTargetTable(targetTable);
        summary.setPriority(priority);
        summary.setStepCount(stepCount);
        summary.setEnableWrite(enableWrite);
        summary.setEnableClean(enableClean);
        summary.setEnableStatus(enableStatus);
        return summary;
    }
}
