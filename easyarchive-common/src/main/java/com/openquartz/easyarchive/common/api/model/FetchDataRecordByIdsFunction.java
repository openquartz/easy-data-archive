package com.openquartz.easyarchive.common.api.model;

import java.util.List;

/**
 * fetchDataByIds Function
 *
 * @author svnee
 */
@FunctionalInterface
public interface FetchDataRecordByIdsFunction {

    /**
     * stepIds
     *
     * @param fetchIdList id list
     * @return data record
     */
    List<DataRecord> fetch(List<Long> fetchIdList);

}
