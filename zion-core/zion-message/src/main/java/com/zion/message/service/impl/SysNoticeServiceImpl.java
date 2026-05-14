package com.zion.message.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zion.message.entity.SysNotice;
import com.zion.message.service.NoticeSendService;
import com.zion.system.entity.SysUser;
import com.zion.message.entity.SysUserNotice;
import com.zion.message.mapper.SysNoticeMapper;
import com.zion.system.mapper.SysUserMapper;
import com.zion.message.mapper.SysUserNoticeMapper;
import com.zion.message.service.SysNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysNoticeServiceImpl implements SysNoticeService {

    private final SysNoticeMapper noticeMapper;
    private final SysUserNoticeMapper userNoticeMapper;
    private final SysUserMapper userMapper;
    private final NoticeSendService noticeSendService;

    @Override
    public Page<SysNotice> page(Integer page, Integer pageSize, String title, Integer noticeType, Integer status) {
        Page<SysNotice> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(title)) {
            wrapper.like(SysNotice::getTitle, title);
        }
        if (noticeType != null) {
            wrapper.eq(SysNotice::getNoticeType, noticeType);
        }
        if (status != null) {
            wrapper.eq(SysNotice::getStatus, status);
        }
        
        wrapper.orderByDesc(SysNotice::getCreateTime);
        return noticeMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Page<SysNotice> getUserNotices(Long userId, Integer page, Integer pageSize, Integer isRead) {
        Page<SysNotice> pageParam = new Page<>(page, pageSize);
        
        // 查询用户可见的已发布通知
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotice::getStatus, 1); // 只查询已发布的
        wrapper.orderByDesc(SysNotice::getCreateTime);
        
        Page<SysNotice> result = noticeMapper.selectPage(pageParam, wrapper);
        
        // 设置已读状态
        result.getRecords().forEach(notice -> {
            LambdaQueryWrapper<SysUserNotice> unWrapper = new LambdaQueryWrapper<>();
            unWrapper.eq(SysUserNotice::getUserId, userId);
            unWrapper.eq(SysUserNotice::getNoticeId, notice.getId());
            SysUserNotice userNotice = userNoticeMapper.selectOne(unWrapper);
            // 如果没有记录或未读，则标记为未读状态（通过临时字段或其他方式处理）
        });
        
        return result;
    }

    @Override
    public SysNotice getById(Long id) {
        return noticeMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(SysNotice notice) {
        notice.setCreateBy(StpUtil.getLoginIdAsLong());
        SysUser user = userMapper.selectById(notice.getCreateBy());
        if (user != null) {
            notice.setCreateName(user.getNickname());
        }
        notice.setCreateTime(LocalDateTime.now());
        notice.setDeleted(0);
        noticeMapper.insert(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysNotice notice) {
        notice.setUpdateTime(LocalDateTime.now());
        noticeMapper.updateById(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        noticeMapper.deleteById(id);
        // 删除关联的用户通知记录
        LambdaQueryWrapper<SysUserNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserNotice::getNoticeId, id);
        userNoticeMapper.delete(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new RuntimeException("通知不存在");
        }
        notice.setStatus(1);
        noticeMapper.updateById(notice);
        noticeSendService.send(notice);
        log.info("通知发布成功，ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, Long noticeId) {
        LambdaUpdateWrapper<SysUserNotice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUserNotice::getUserId, userId);
        wrapper.eq(SysUserNotice::getNoticeId, noticeId);
        wrapper.set(SysUserNotice::getIsRead, 1);
        wrapper.set(SysUserNotice::getReadTime, LocalDateTime.now());
        userNoticeMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<SysUserNotice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUserNotice::getUserId, userId);
        wrapper.eq(SysUserNotice::getIsRead, 0);
        wrapper.set(SysUserNotice::getIsRead, 1);
        wrapper.set(SysUserNotice::getReadTime, LocalDateTime.now());
        userNoticeMapper.update(null, wrapper);
    }

    @Override
    public int getUnreadCount(Long userId) {
        return userNoticeMapper.selectUnreadCount(userId);
    }
}
