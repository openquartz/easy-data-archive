package com.openquartz.easyarchive.common.api.model;

import java.math.BigInteger;

/**
 * IdResolver
 *
 * @author svnee
 */
public class IdResolver {

    public static Long resolve(Object id) {

        if (id instanceof Long) {
            return (Long) id;
        }

        if (id instanceof BigInteger) {
            return ((BigInteger) id).longValue();
        }
        if (id instanceof Integer) {
            return ((Integer) id).longValue();
        }

        throw new NumberFormatException("Id 无法转换为Long，id=" + id.toString());
    }
}
