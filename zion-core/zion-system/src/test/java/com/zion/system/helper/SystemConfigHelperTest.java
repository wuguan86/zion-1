package com.zion.system.helper;

import com.zion.system.entity.SysConfigGroup;
import com.zion.system.service.SysConfigGroupService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemConfigHelperTest {

    private final SysConfigGroupService configGroupService = mock(SysConfigGroupService.class);
    private final SystemConfigHelper helper = new SystemConfigHelper(configGroupService, new ObjectMapper());

    @Test
    void getSmsRegionDefaultsToCnHangzhouWhenNotConfigured() {
        SysConfigGroup group = new SysConfigGroup();
        group.setConfigValue("{\"provider\":\"aliyun\"}");
        when(configGroupService.getByGroupCode(SystemConfigHelper.GROUP_SMS)).thenReturn(group);

        assertThat(helper.getSmsRegion()).isEqualTo("cn-hangzhou");
    }

    @Test
    void getSmsRegionReturnsConfiguredRegion() {
        SysConfigGroup group = new SysConfigGroup();
        group.setConfigValue("{\"region\":\"ap-guangzhou\"}");
        when(configGroupService.getByGroupCode(SystemConfigHelper.GROUP_SMS)).thenReturn(group);

        assertThat(helper.getSmsRegion()).isEqualTo("ap-guangzhou");
    }
}
