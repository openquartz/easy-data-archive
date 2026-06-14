package com.openquartz.easyarchive.core;

import com.openquartz.easyarchive.core.property.ArchiveConfig;

import java.io.Closeable;
import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import com.openquartz.easyarchive.common.api.PageSource;
import com.openquartz.easyarchive.common.api.Sink;
import com.openquartz.easyarchive.common.api.model.DataIterator;
import com.openquartz.easyarchive.common.api.model.DataRecord;
import com.openquartz.easyarchive.common.api.model.EmptyDataIterator;
import com.openquartz.easyarchive.common.util.ExceptionUtils;

/**
 * SyncExecutor
 *
 * @author svnee
 */
@Slf4j
public class SyncExecutor implements Closeable {

    private static final String DB_PRESSURE_MSG = "归档太快会对DB有压力, sleep %dms";

    private final ArchiveConfig archiveConfig;
    private final PageSource reader;
    private final Sink sink;
    private final int stepIntervalMs;

    public SyncExecutor(ArchiveConfig archiveConfig, PageSource source, Sink sink,int stepIntervalMs) {
        this.archiveConfig = archiveConfig;
        this.reader = source;
        this.sink = sink;
        this.stepIntervalMs = stepIntervalMs;
    }

    public int execute(Long start, Long end, int step) {
        return doExecute(start, end, step);
    }

    public int execute(Date start, Date end, int step) {
        return doExecute(start, end, step);
    }

    private int doExecute(Object start, Object end, int step) {

        int archiveRows = 0;

        // 执行循环归档
        for (Integer exeLoadFreq = 0; exeLoadFreq < archiveConfig.getMaxTryLoadFrequencyUnitTime(); exeLoadFreq++) {
            DataIterator itr = reader.read(start, end, exeLoadFreq, archiveConfig.getMaxLoadRows(), step);
            if (itr instanceof EmptyDataIterator) {
                log.info("[SyncExecutor#execute]execute end!start:{},end:{},exeFreq:{}", start, end, exeLoadFreq);
                break;
            }
            while (itr.hasNext()) {

                List<DataRecord> readData = itr.next();
                sink.write(readData);
                reader.clean(readData);

                // 归档迁移数
                archiveRows += readData.size();

                try {
                    Thread.sleep(stepIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    ExceptionUtils.rethrow(e);
                }
            }
        }

        log.info("[SyncExecutor#doExecute] execute archive,start:{},end:{},rows:{}", start,end,archiveRows);
        return archiveRows;
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (Exception ignoredException) {
            // ignore
        }
        try {
            sink.close();
        } catch (Exception ignoredException) {
            // ignore
        }
    }
}
