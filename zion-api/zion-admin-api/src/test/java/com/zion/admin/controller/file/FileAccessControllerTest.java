package com.zion.admin.controller.file;

import com.zion.oss.FileStorage;
import com.zion.oss.LocalFileStorage;
import com.zion.system.helper.SystemConfigHelper;
import com.zion.system.storage.FileStorageFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileAccessControllerTest {

    @Test
    void shouldReadCurrentStorageWhenProviderIsNotLocal() {
        SystemConfigHelper configHelper = mock(SystemConfigHelper.class);
        FileStorageFactory storageFactory = mock(FileStorageFactory.class);
        FileStorage storage = new InMemoryFileStorage();
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(configHelper.getStorageProvider()).thenReturn("aliyun");
        when(storageFactory.getStorage()).thenReturn(storage);
        when(request.getRequestURI()).thenReturn("/api/files/images/2026/05/16/avatar.png");

        FileAccessController controller = new FileAccessController(configHelper, storageFactory);

        ResponseEntity<byte[]> response = controller.getFile(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("avatar".getBytes());
    }

    private static class InMemoryFileStorage implements FileStorage {

        @Override
        public String getStorageType() {
            return "aliyun";
        }

        @Override
        public String upload(InputStream inputStream, String path, String fileName) {
            return null;
        }

        @Override
        public void delete(String path) {
        }

        @Override
        public byte[] getFile(String path) {
            return "images/2026/05/16/avatar.png".equals(path) ? "avatar".getBytes() : new byte[0];
        }

        @Override
        public String getUrl(String path) {
            return null;
        }

        @Override
        public boolean exists(String path) {
            return true;
        }
    }
}
