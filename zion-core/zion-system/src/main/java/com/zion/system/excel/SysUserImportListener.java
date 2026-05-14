package com.zion.system.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.zion.system.entity.SysDept;
import com.zion.system.entity.SysPost;
import com.zion.system.entity.SysRole;
import com.zion.system.entity.SysUser;
import com.zion.system.entity.SysUserPost;
import com.zion.system.entity.SysUserRole;
import com.zion.system.mapper.SysDeptMapper;
import com.zion.system.mapper.SysPostMapper;
import com.zion.system.mapper.SysRoleMapper;
import com.zion.system.mapper.SysUserMapper;
import com.zion.system.mapper.SysUserPostMapper;
import com.zion.system.mapper.SysUserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.crypto.digest.BCrypt;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户导入监听器
 */
@Slf4j
public class SysUserImportListener extends AnalysisEventListener<SysUserExcel> {

    private static final int BATCH_COUNT = 100;
    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserPostMapper userPostMapper;
    private final Map<String, Long> deptMap;
    private final Map<String, Long> roleMap;
    private final Map<String, Long> postMap;

    private final List<SysUserExcel> dataList = new ArrayList<>();
    private final List<String> errorMessages = new ArrayList<>();
    private int successCount = 0;
    private int failCount = 0;

    public SysUserImportListener(SysUserMapper userMapper, SysDeptMapper deptMapper,
                                   SysRoleMapper roleMapper, SysPostMapper postMapper,
                                   SysUserRoleMapper userRoleMapper, SysUserPostMapper userPostMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.userPostMapper = userPostMapper;
        // 预加载所有部门，建立名称到ID的映射
        List<SysDept> depts = deptMapper.selectList(null);
        this.deptMap = depts.stream().collect(Collectors.toMap(SysDept::getDeptName, SysDept::getId, (a, b) -> a));
        // 预加载所有角色，建立名称到ID的映射
        List<SysRole> roles = roleMapper.selectList(null);
        this.roleMap = roles.stream().collect(Collectors.toMap(SysRole::getName, SysRole::getId, (a, b) -> a));
        // 预加载所有岗位，建立名称到ID的映射
        List<SysPost> posts = postMapper.selectList(null);
        this.postMap = posts.stream().collect(Collectors.toMap(SysPost::getPostName, SysPost::getId, (a, b) -> a));
    }

    @Override
    public void invoke(SysUserExcel data, AnalysisContext context) {
        int rowNum = context.readRowHolder().getRowIndex() + 1;

        // 数据校验
        if (!StringUtils.hasText(data.getUsername())) {
            errorMessages.add("第" + rowNum + "行：用户名不能为空");
            failCount++;
            return;
        }

        if (!StringUtils.hasText(data.getNickname())) {
            data.setNickname(data.getUsername());
        }

        // 检查用户名是否已存在
        SysUser existUser = userMapper.selectByUsername(data.getUsername());
        if (existUser != null) {
             errorMessages.add("第" + rowNum + "行：用户名[" + data.getUsername() + "]已存在");
            failCount++;
            return;
        }

        // 解析部门
        if (StringUtils.hasText(data.getDeptName())) {
            Long deptId = deptMap.get(data.getDeptName().trim());
            if (deptId != null) {
                data.setDeptId(deptId);
            }
        }

        // 解析性别
        if (StringUtils.hasText(data.getGenderStr())) {
            switch (data.getGenderStr().trim()) {
                case "男" -> data.setGender(1);
                case "女" -> data.setGender(2);
                default -> data.setGender(0);
            }
        } else {
            data.setGender(0);
        }

        // 解析用户类型
        if (StringUtils.hasText(data.getUserTypeStr())) {
            switch (data.getUserTypeStr().trim()) {
                case "后台管理员" -> data.setUserType("admin");
                case "PC前台用户" -> data.setUserType("pc");
                case "App/小程序用户" -> data.setUserType("app");
                default -> data.setUserType("admin");
            }
        } else {
            data.setUserType("admin");
        }

        // 解析状态
        if (StringUtils.hasText(data.getStatusStr())) {
            switch (data.getStatusStr().trim()) {
                case "启用" -> data.setStatus(1);
                case "禁用" -> data.setStatus(0);
                default -> data.setStatus(1);
            }
        } else {
            data.setStatus(1);
        }

        // 解析角色（多个角色用逗号分隔）
        if (StringUtils.hasText(data.getRoleNames())) {
            List<Long> roleIds = new ArrayList<>();
            String[] roleNameArr = data.getRoleNames().split("[,，]");
            for (String roleName : roleNameArr) {
                Long roleId = roleMap.get(roleName.trim());
                if (roleId != null) {
                    roleIds.add(roleId);
                }
            }
            data.setRoleIds(roleIds);
        }

        // 解析岗位（多个岗位用逗号分隔）
        if (StringUtils.hasText(data.getPostNames())) {
            List<Long> postIds = new ArrayList<>();
            String[] postNameArr = data.getPostNames().split("[,，]");
            for (String postName : postNameArr) {
                Long postId = postMap.get(postName.trim());
                if (postId != null) {
                    postIds.add(postId);
                }
            }
            data.setPostIds(postIds);
        }

        dataList.add(data);

        if (dataList.size() >= BATCH_COUNT) {
            saveData();
            dataList.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
        log.info("用户导入完成，成功: {}, 失败: {}", successCount, failCount);
    }

    private void saveData() {
        if (dataList.isEmpty()) {
            return;
        }

        for (SysUserExcel excel : dataList) {
            try {
                SysUser user = new SysUser();
                user.setUsername(excel.getUsername());
                user.setNickname(excel.getNickname());
                user.setDeptId(excel.getDeptId());
                user.setEmail(excel.getEmail());
                user.setPhone(excel.getPhone());
                user.setGender(excel.getGender());
                user.setUserType(excel.getUserType());
                user.setStatus(excel.getStatus());
                user.setPassword(BCrypt.hashpw(DEFAULT_PASSWORD));
                user.setCreateTime(LocalDateTime.now());
                user.setUpdateTime(LocalDateTime.now());
                user.setDeleted(0);
                user.setIsQuit(0);

                userMapper.insert(user);

                // 保存用户角色关联
                if (excel.getRoleIds() != null && !excel.getRoleIds().isEmpty()) {
                    for (Long roleId : excel.getRoleIds()) {
                        SysUserRole userRole = new SysUserRole();
                        userRole.setUserId(user.getId());
                        userRole.setRoleId(roleId);
                        userRoleMapper.insert(userRole);
                    }
                }

                // 保存用户岗位关联
                if (excel.getPostIds() != null && !excel.getPostIds().isEmpty()) {
                    for (Long postId : excel.getPostIds()) {
                        SysUserPost userPost = new SysUserPost();
                        userPost.setUserId(user.getId());
                        userPost.setPostId(postId);
                        userPostMapper.insert(userPost);
                    }
                }

                successCount++;
            } catch (Exception e) {
                log.error("导入用户失败: {}", excel.getUsername(), e);
                 errorMessages.add("用户[" + excel.getUsername() + "]导入失败: " + e.getMessage());
                failCount++;
            }
        }
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }
}
