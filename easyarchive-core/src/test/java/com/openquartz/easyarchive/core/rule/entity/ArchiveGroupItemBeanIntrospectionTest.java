package com.openquartz.easyarchive.core.rule.entity;

import org.apache.ibatis.reflection.Reflector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchiveGroupItemBeanIntrospectionTest {

    @Test
    void archiveGroupItemById_shouldExposeIntegerFlagsWithoutGetterConflicts() {
        Reflector reflector = assertDoesNotThrow(() -> new Reflector(ArchiveGroupItemById.class));

        assertEquals(Integer.class, reflector.getGetterType("enableClean"));
        assertEquals(Integer.class, reflector.getGetterType("enableWrite"));
    }

    @Test
    void archiveGroupItemByTime_shouldExposeIntegerFlagsWithoutGetterConflicts() {
        Reflector reflector = assertDoesNotThrow(() -> new Reflector(ArchiveGroupItemByTime.class));

        assertEquals(Integer.class, reflector.getGetterType("enableClean"));
        assertEquals(Integer.class, reflector.getGetterType("enableWrite"));
    }
}
