package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.starter.security.CurrentUserInfo;

public interface CurrentUserService {
    CurrentUserInfo getCurrentUser();
    boolean isAdmin();
    void assertAdmin();
}
