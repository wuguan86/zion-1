package com.zion.admin.controller.wechat;

import com.zion.common.result.Result;
import com.zion.wechat.WechatMpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信公众号后台管理接口
 */
@RestController
@RequestMapping("/wechat/mp")
@RequiredArgsConstructor
@Slf4j
public class WechatMpAdminController {

    private final WechatMpService wechatMpService;

    /**
     * 同步自定义菜单到微信公众号
     */
    @PostMapping("/menu/sync")
    public Result<Void> syncMenu(@RequestBody MenuSyncRequest request) {
        if (request == null || request.menuConfig() == null || request.menuConfig().isBlank()) {
            return Result.fail(400, "公众号菜单配置不能为空");
        }

        wechatMpService.createMenu(request.menuConfig());
        log.info("微信公众号菜单同步成功");
        return Result.ok();
    }

    /**
     * 获取微信公众号当前菜单配置
     */
    @GetMapping("/menu")
    public Result<String> getMenu() {
        return Result.ok(wechatMpService.getMenu());
    }

    /**
     * 删除微信公众号当前菜单
     */
    @DeleteMapping("/menu")
    public Result<Void> deleteMenu() {
        wechatMpService.deleteMenu();
        log.info("微信公众号菜单删除成功");
        return Result.ok();
    }

    public record MenuSyncRequest(String menuConfig) {
    }
}
