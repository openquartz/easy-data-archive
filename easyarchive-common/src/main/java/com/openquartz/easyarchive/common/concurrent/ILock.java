package com.openquartz.easyarchive.common.concurrent;

/**
 * ILock
 *
 * @author svnee
 */
public interface ILock {

    /**
     * 加锁
     *
     * @param lockKey 锁定key
     */
    boolean lock(Long lockKey);

    /**
     * 解锁
     *
     * @param lockKey 锁定的key
     */
    boolean unlock(Long lockKey);
}
