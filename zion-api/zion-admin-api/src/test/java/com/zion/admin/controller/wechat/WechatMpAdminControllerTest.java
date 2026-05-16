package com.zion.admin.controller.wechat;

import com.zion.wechat.WechatMpService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WechatMpAdminControllerTest {

    @Test
    void syncMenuCreatesWechatMenu() {
        WechatMpService wechatMpService = mock(WechatMpService.class);
        WechatMpAdminController controller = new WechatMpAdminController(wechatMpService);

        var response = controller.syncMenu(new WechatMpAdminController.MenuSyncRequest("{\"button\":[]}"));

        assertThat(response.getCode()).isEqualTo(200);
        verify(wechatMpService).createMenu("{\"button\":[]}");
    }

    @Test
    void syncMenuRejectsBlankMenuConfig() {
        WechatMpService wechatMpService = mock(WechatMpService.class);
        WechatMpAdminController controller = new WechatMpAdminController(wechatMpService);

        var response = controller.syncMenu(new WechatMpAdminController.MenuSyncRequest(" "));

        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMessage()).isEqualTo("公众号菜单配置不能为空");
    }

    @Test
    void getMenuReturnsWechatMenuJson() {
        WechatMpService wechatMpService = mock(WechatMpService.class);
        when(wechatMpService.getMenu()).thenReturn("{\"menu\":{\"button\":[]}}");
        WechatMpAdminController controller = new WechatMpAdminController(wechatMpService);

        var response = controller.getMenu();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo("{\"menu\":{\"button\":[]}}");
    }

    @Test
    void deleteMenuDeletesWechatMenu() {
        WechatMpService wechatMpService = mock(WechatMpService.class);
        WechatMpAdminController controller = new WechatMpAdminController(wechatMpService);

        var response = controller.deleteMenu();

        assertThat(response.getCode()).isEqualTo(200);
        verify(wechatMpService).deleteMenu();
    }
}
