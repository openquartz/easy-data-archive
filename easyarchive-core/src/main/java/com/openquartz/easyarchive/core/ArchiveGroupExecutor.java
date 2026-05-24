package com.openquartz.easyarchive.core;

import com.openquartz.easyarchive.common.entity.Pair;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.executor.ArchiveExecutor;
import com.openquartz.easyarchive.core.property.ArchiveConfig;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItem;
import com.openquartz.easyarchive.core.rule.ArchiveRuleLoader;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import com.openquartz.easyarchive.common.concurrent.ILock;
import com.openquartz.easyarchive.common.util.ExceptionUtils;

/**
 * ArchiveService
 *
 * @author svnee
 */
@Slf4j
public class ArchiveGroupExecutor implements Runnable {

    private final ArchiveRuleLoader loader;
    private final ArchiveConfig archiveConfig;
    private final ArchiveGroupExecuteTask executeTask;
    private final Pair<ArchiveConnection,ArchiveConnection> connectionInfo;

    public ArchiveGroupExecutor(ArchiveRuleLoader loader,
                                ArchiveConfig archiveConfig,
                                ArchiveGroupExecuteTask executeTask,
                                Pair<ArchiveConnection, ArchiveConnection> connectionInfo) {
        this.loader = loader;
        this.archiveConfig = archiveConfig;
        this.executeTask = executeTask;
        this.connectionInfo = connectionInfo;
    }

    /**
     * 开始执行
     */
    @Override
    public void run() {

        Date startTime = new Date();
        try{

            List<ArchiveGroupItem> configs = loader.load();
            configs.sort(Comparator.comparing(ArchiveGroupItem::getPriority));

            // 开始执行归档
            doExecute(configs);

            // 上报进度
            doReportExecuteProcess(startTime);

        } catch (Exception ex){

            // 记录异常日志

            // 执行状态

            ExceptionUtils.rethrow(ex);
        }
    }

    private void doReportExecuteProcess(Date startTime) {

        // 计算执行时间

        // 计算最终处理进度

        // 上报进度

    }

    private void doExecute(List<ArchiveGroupItem> configs) {
        new ArchiveExecutor(connectionInfo.getKey(),connectionInfo.getValue(),archiveConfig,configs,executeTask.getId()).run();
    }
}
