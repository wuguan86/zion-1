package com.zion.system.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zion.common.entity.BaseEntity;
import com.zion.common.exception.BusinessException;
import com.zion.common.result.PageResult;
import com.zion.system.entity.SysDept;
import com.zion.system.entity.SysPost;
import com.zion.system.entity.SysRole;
import com.zion.system.entity.SysUser;
import com.zion.system.entity.SysUserPost;
import com.zion.system.entity.SysUserRole;
import com.zion.system.excel.SysUserExcel;
import com.zion.system.excel.SysUserImportListener;
import com.zion.system.mapper.SysDeptMapper;
import com.zion.system.mapper.SysPostMapper;
import com.zion.system.mapper.SysRoleMapper;
import com.zion.system.mapper.SysUserMapper;
import com.zion.system.mapper.SysUserPostMapper;
import com.zion.system.mapper.SysUserRoleMapper;
import com.zion.system.config.StpInterfaceImpl;
import com.zion.system.service.SysUserService;
import com.zion.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysUserPostMapper userPostMapper;
    private final SysDeptMapper deptMapper;
    private final SysPostMapper postMapper;
    private final SysRoleMapper roleMapper;
    private final SystemConfigHelper configHelper;

    private static final String DEFAULT_PASSWORD = "123456";

    @Override
    public PageResult<SysUser> page(Integer page, Integer pageSize, String username, Integer status, String userType, Long deptId, Long postId) {
        Page<SysUser> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), SysUser::getUsername, username)
                .eq(status != null, SysUser::getStatus, status)
                .eq(StringUtils.hasText(userType), SysUser::getUserType, userType)
                .eq(deptId != null, SysUser::getDeptId, deptId)
                .apply("u.deleted = 0");

        if (postId != null) {
            List<Long> userIds = userPostMapper.selectUserIdsByPostId(postId);
            if (userIds.isEmpty()) {
                return PageResult.empty();
            }
            wrapper.apply("u.id IN ({0})", userIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        }

        wrapper.orderByDesc(SysUser::getCreateTime);

        // 切换到自定义 @DataScope 拦截器
        IPage<SysUser> result = baseMapper.selectUserPage(pageParam, wrapper);

        // 获取岗位映射
        List<SysPost> allPosts = postMapper.selectList(null);
        Map<Long, String> postMap = allPosts.stream().collect(Collectors.toMap(SysPost::getId, SysPost::getPostName));

        // 清空密码，填充部门名称
        result.getRecords().forEach(user -> {
            user.setPassword(null);
            List<Long> userPostIds = getPostIds(user.getId());
            if (userPostIds != null && !userPostIds.isEmpty()) {
                String postNames = userPostIds.stream()
                        .map(postMap::get)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.joining(","));
                user.setPostNames(postNames);
            }
        });
        return PageResult.of(result);
    }

    @Override
    public SysUser getDetail(Long id) {
        SysUser user = this.getById(id);
        if (user != null) {
            user.setPassword(null);
            // 填充部门名称
            if (user.getDeptId() != null) {
                SysDept dept = deptMapper.selectById(user.getDeptId());
                if (dept != null) {
                    user.setDeptName(dept.getDeptName());
                }
            }
            // 填充岗位名称
            List<Long> postIds = getPostIds(user.getId());
            if (postIds != null && !postIds.isEmpty()) {
                List<SysPost> posts = postMapper.selectBatchIds(postIds);
                String postNames = posts.stream()
                        .map(SysPost::getPostName)
                        .collect(Collectors.joining(","));
                user.setPostNames(postNames);
            }
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(SysUser user, List<Long> roleIds, List<Long> postIds) {
        // 检查用户名是否存在
        if (this.getByUsername(user.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        // 加密密码
        String password = StringUtils.hasText(user.getPassword()) ? user.getPassword() : DEFAULT_PASSWORD;
        user.setPassword(BCrypt.hashpw(password));
        this.save(user);
        // 保存用户角色关联
        saveUserRoles(user.getId(), roleIds);
        // 保存用户岗位关联
        saveUserPosts(user.getId(), postIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysUser user, List<Long> roleIds, List<Long> postIds) {
        SysUser existUser = this.getById(user.getId());
        if (existUser == null) {
            throw new BusinessException("用户不存在");
        }
        // 检查用户名是否存在
        SysUser byUsername = this.getByUsername(user.getUsername());
        if (byUsername != null && !byUsername.getId().equals(user.getId())) {
            throw new BusinessException("用户名已存在");
        }
        // 不更新密码
        user.setPassword(null);
        this.updateById(user);
        // 更新用户角色关联
        userRoleMapper.deleteByUserId(user.getId());
        saveUserRoles(user.getId(), roleIds);
        // 更新用户岗位关联
        userPostMapper.deleteByUserId(user.getId());
        saveUserPosts(user.getId(), postIds);
        // 角色变更，清除该用户的权限缓存
        StpInterfaceImpl.clearPermissionCache(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        this.removeById(id);
        userRoleMapper.deleteByUserId(id);
        userPostMapper.deleteByUserId(id);
        // 用户删除，清除权限缓存
        StpInterfaceImpl.clearPermissionCache(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            this.removeById(id);
            userRoleMapper.deleteByUserId(id);
            userPostMapper.deleteByUserId(id);
            StpInterfaceImpl.clearPermissionCache(id);
        }
    }

    @Override
    public SysUser getByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    @Override
    public List<String> getRoleCodes(Long userId) {
        return baseMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    public List<String> getPermissions(Long userId) {
        return baseMapper.selectPermissionsByUserId(userId);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        // 验证新密码规则
        configHelper.validatePassword(newPassword);

        user.setPassword(BCrypt.hashpw(newPassword));
        this.updateById(user);
    }

    @Override
    public void resetPassword(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(BCrypt.hashpw(DEFAULT_PASSWORD));
        this.updateById(user);
    }

    @Override
    public void updateProfile(Long userId, SysUser profile) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 只允许更新昵称、邮箱、手机号、头像、性别、备注
        if (profile.getNickname() != null) {
            user.setNickname(profile.getNickname());
        }
        if (profile.getEmail() != null) {
            user.setEmail(profile.getEmail());
        }
        if (profile.getPhone() != null) {
            user.setPhone(profile.getPhone());
        }
        if (profile.getAvatar() != null) {
            user.setAvatar(profile.getAvatar());
        }
        if (profile.getGender() != null) {
            user.setGender(profile.getGender());
        }
        if (profile.getRemark() != null) {
            user.setRemark(profile.getRemark());
        }
        this.updateById(user);
    }

    @Override
    public List<SysUser> listAll() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getStatus, 1);
        wrapper.orderByAsc(SysUser::getId);
        List<SysUser> users = this.list(wrapper);
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    @Override
    public SysUser getByOpenId(String openId) {
        return this.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getOpenId, openId));
    }

    @Override
    public List<Long> getPostIds(Long userId) {
        return userPostMapper.selectList(new LambdaQueryWrapper<SysUserPost>()
                        .eq(SysUserPost::getUserId, userId))
                .stream()
                .map(SysUserPost::getPostId)
                .collect(Collectors.toList());
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }
    }

    private void saveUserPosts(Long userId, List<Long> postIds) {
        if (postIds != null && !postIds.isEmpty()) {
            for (Long postId : postIds) {
                SysUserPost userPost = new SysUserPost();
                userPost.setUserId(userId);
                userPost.setPostId(postId);
                userPostMapper.insert(userPost);
            }
        }
    }

    @Override
    public List<SysUserExcel> exportUsers(String username, Integer status, String userType, Long deptId, List<Long> ids) {
        // 查询用户列表
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        // 如果指定了ID列表，则只导出指定用户
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(SysUser::getId, ids);
        } else {
            wrapper.like(StringUtils.hasText(username), SysUser::getUsername, username)
                    .eq(status != null, SysUser::getStatus, status)
                    .eq(StringUtils.hasText(userType), SysUser::getUserType, userType)
                    .eq(deptId != null, SysUser::getDeptId, deptId);
        }
        wrapper.eq(SysUser::getDeleted, 0)
                .orderByDesc(SysUser::getCreateTime);

        List<SysUser> users = this.list(wrapper);

        // 获取部门映射
        List<SysDept> depts = deptMapper.selectList(null);
        Map<Long, String> deptMap = depts.stream().collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));

        // 获取岗位映射
        List<SysPost> posts = postMapper.selectList(null);
        Map<Long, String> postMap = posts.stream().collect(Collectors.toMap(SysPost::getId, SysPost::getPostName, (a, b) -> a));

        // 转换为 Excel DTO
        List<SysUserExcel> result = new ArrayList<>();
        for (SysUser user : users) {
            SysUserExcel excel = new SysUserExcel();
            excel.setUsername(user.getUsername());
            excel.setNickname(user.getNickname());
            excel.setDeptName(user.getDeptId() != null ? deptMap.get(user.getDeptId()) : null);
            excel.setEmail(user.getEmail());
            excel.setPhone(user.getPhone());

            // 性别转换
            if (user.getGender() != null) {
                switch (user.getGender()) {
                    case 1 -> excel.setGenderStr("男");
                    case 2 -> excel.setGenderStr("女");
                    default -> excel.setGenderStr("未知");
                }
            }

            // 用户类型转换
            if (user.getUserType() != null) {
                switch (user.getUserType()) {
                    case "admin" -> excel.setUserTypeStr("后台管理员");
                    case "pc" -> excel.setUserTypeStr("PC前台用户");
                    case "app" -> excel.setUserTypeStr("App/小程序用户");
                    default -> excel.setUserTypeStr(user.getUserType());
                }
            }

            // 状态转换
            if (user.getStatus() != null) {
                switch (user.getStatus()) {
                    case 1 -> excel.setStatusStr("启用");
                    case 0 -> excel.setStatusStr("禁用");
                    case 2 -> excel.setStatusStr("待审核");
                    default -> excel.setStatusStr("未知");
                }
            }

            // 获取用户角色
            List<SysRole> userRoles = roleMapper.selectRolesByUserId(user.getId());
            if (userRoles != null && !userRoles.isEmpty()) {
                String roleNames = userRoles.stream()
                        .map(SysRole::getName)
                        .collect(Collectors.joining(","));
                excel.setRoleNames(roleNames);
            }

            // 获取用户岗位
            List<Long> userPostIds = getPostIds(user.getId());
            if (userPostIds != null && !userPostIds.isEmpty()) {
                String postNames = userPostIds.stream()
                        .map(postMap::get)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.joining(","));
                excel.setPostNames(postNames);
            }

            result.add(excel);
        }
        return result;
    }

    @Override
    public Map<String, Object> importUsers(MultipartFile file) {
        try {
            SysUserImportListener listener = new SysUserImportListener(
                    baseMapper, deptMapper, roleMapper, postMapper, userRoleMapper, userPostMapper);
            EasyExcel.read(file.getInputStream(), SysUserExcel.class, listener).sheet().doRead();

            Map<String, Object> result = new HashMap<>();
            result.put("successCount", listener.getSuccessCount());
            result.put("failCount", listener.getFailCount());
            result.put("errors", listener.getErrorMessages());
            return result;
        } catch (IOException e) {
            log.error("读取Excel文件失败", e);
            throw new BusinessException("读取Excel文件失败");
        }
    }
}
