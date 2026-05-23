package com.openquartz.easyarchive.common.api.model;

import java.util.List;

/**
 * @author svnee
 */
public class StepIdDataIterator implements DataIterator {

    private final List<Long> ids;
    private final int countPage;
    private final int pageSize;
    private int currentPage;

    private final FetchDataRecordByIdsFunction fetchDataRecordByIdsFunction;

    public StepIdDataIterator(List<Long> ids, int internal, FetchDataRecordByIdsFunction function) {
        this.ids = ids;
        this.pageSize = internal;
        this.currentPage = 0;
        this.countPage = (this.ids.size() + internal - 1) / internal;
        this.fetchDataRecordByIdsFunction = function;
    }

    @Override
    public boolean hasNext() {
        return this.currentPage < this.countPage;
    }

    @Override
    public List<DataRecord> next() {
        int pageStart = (this.currentPage) * this.pageSize;
        int pageEnd = Math.min(pageStart + this.pageSize, this.ids.size());

        List<Long> stepIds = ids.subList(pageStart, pageEnd);

        ++this.currentPage;
        return fetchDataRecordByIdsFunction.fetch(stepIds);
    }
}