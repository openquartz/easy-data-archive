package com.openquartz.easyarchive.common.concurrent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的简单锁实现
 * 适用于单机部署环境
 *
 * @author svnee
 */
public class SimpleMemoryLock implements ILock {

    private final Map<Long, Boolean> lockMap = new ConcurrentHashMap<>();

    @Override
    public boolean lock(Long lockKey) {
        if (lockKey == null) {
            return false;
        }

        // 使用putIfAbsent实现原子性操作
        Boolean existingValue = lockMap.putIfAbsent(lockKey, true);

        // 如果existingValue为null，说明之前没有锁，加锁成功
        // 如果existingValue为true，说明锁已存在，加锁失败
        return existingValue == null;
    }

    @Override
    public boolean unlock(Long lockKey) {
        if (lockKey == null) {
            return false;
        }

        // 移除锁
        Boolean removedValue = lockMap.remove(lockKey);

        // 如果removedValue不为null，说明成功移除了锁
        return removedValue != null;
    }

}