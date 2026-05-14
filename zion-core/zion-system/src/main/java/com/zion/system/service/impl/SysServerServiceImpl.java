package com.zion.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zion.system.entity.SysServer;
import com.zion.system.mapper.SysServerMapper;
import com.zion.system.service.SysServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 服务器管理 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysServerServiceImpl extends ServiceImpl<SysServerMapper, SysServer> implements SysServerService {
    
    @Override
    public Page<SysServer> pageList(Page<SysServer> page, String name, Integer status) {
        LambdaQueryWrapper<SysServer> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), SysServer::getName, name)
               .eq(status != null, SysServer::getStatus, status)
               .orderByAsc(SysServer::getSort)
               .orderByDesc(SysServer::getCreateTime);
        
        Page<SysServer> result = this.page(page, wrapper);
        // 清除敏感信息
        result.getRecords().forEach(server -> {
            server.setPassword(null);
            server.setPrivateKey(null);
            server.setPassphrase(null);
        });
        return result;
    }
    
    @Override
    public boolean testConnection(Long id) {
        SysServer server = this.getById(id);
        if (server == null) {
            return false;
        }
        return testConnection(server.getHost(), server.getPort(), server.getUsername(),
                server.getAuthType(), server.getPassword(), server.getPrivateKey(), server.getPassphrase());
    }
    
    @Override
    public boolean testConnection(String host, Integer port, String username, Integer authType,
                                  String password, String privateKey, String passphrase) {
        SshClient client = SshClient.setUpDefaultClient();
        client.start();
        
        try {
            ClientSession session = client.connect(username, host, port)
                    .verify(Duration.ofSeconds(10))
                    .getSession();
            
            // 密码认证
            if (authType == 1 && StringUtils.hasText(password)) {
                session.addPasswordIdentity(password);
            }
            // 密钥认证（简化处理，实际需要解析私钥）
            // else if (authType == 2 && StringUtils.hasText(privateKey)) {
            //     session.addPublicKeyIdentity(keyPair);
            // }
            
            session.auth().verify(Duration.ofSeconds(10));
            
            boolean authenticated = session.isAuthenticated();
            session.close();
            
            return authenticated;
        } catch (Exception e) {
            log.error("SSH连接测试失败: {}", e.getMessage());
            return false;
        } finally {
            try {
                client.stop();
            } catch (Exception ignored) {}
        }
    }
    
    @Override
    public boolean save(SysServer entity) {
        // TODO: 可以在这里对密码进行加密存储
        return super.save(entity);
    }
    
    @Override
    public boolean updateById(SysServer entity) {
        // 如果密码为空，不更新密码字段
        if (!StringUtils.hasText(entity.getPassword())) {
            entity.setPassword(null);
        }
        if (!StringUtils.hasText(entity.getPrivateKey())) {
            entity.setPrivateKey(null);
        }
        return super.updateById(entity);
    }
}
