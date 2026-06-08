package com.openquartz.easyarchive.starter.service;

/**
 * Archive resource access control service.
 * Enforces permission checks on archive groups and tasks based on
 * datasource-level authorization rather than admin-only restrictions.
 */
public interface ArchiveResourceAccessService {

    /**
     * Assert the current user can read (access) the given group.
     * Requires USE-level permission on both source and target datasources.
     */
    void assertGroupAccessible(Long groupId);

    /**
     * Assert the current user can manage (write) the given group.
     * Requires MANAGE-level permission on both source and target datasources.
     */
    void assertGroupManageable(Long groupId);

    /**
     * Assert the current user can access the given task.
     * Delegates to assertGroupAccessible for the task's parent group.
     */
    void assertTaskAccessible(Long taskId);
}
