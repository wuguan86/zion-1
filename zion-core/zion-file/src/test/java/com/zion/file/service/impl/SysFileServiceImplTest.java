package com.zion.file.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SysFileServiceImplTest {

    @Test
    void shouldUsePreviewUrlWhenStorageReturnsObjectPath() {
        assertThat(SysFileServiceImpl.resolvePublicUrl("/images/2026/05/16/avatar.png", 123L))
                .isEqualTo("/api/sys/file/preview/123");
    }

    @Test
    void shouldKeepAccessibleUrlUnchanged() {
        assertThat(SysFileServiceImpl.resolvePublicUrl("https://cdn.example.com/avatar.png", 123L))
                .isEqualTo("https://cdn.example.com/avatar.png");
        assertThat(SysFileServiceImpl.resolvePublicUrl("/api/files/images/avatar.png", 123L))
                .isEqualTo("/api/files/images/avatar.png");
        assertThat(SysFileServiceImpl.resolvePublicUrl("/api/sys/file/preview/123", 123L))
                .isEqualTo("/api/sys/file/preview/123");
    }
}
