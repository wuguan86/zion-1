package com.zion.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zion.common.exception.BusinessException;
import com.zion.common.result.PageResult;
import com.zion.system.entity.SysPost;
import com.zion.system.mapper.SysPostMapper;
import com.zion.system.service.SysPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 岗位服务实现
 */
@Service
@RequiredArgsConstructor
public class SysPostServiceImpl extends ServiceImpl<SysPostMapper, SysPost> implements SysPostService {

    @Override
    public PageResult<SysPost> page(Integer page, Integer pageSize, String postCode, String postName, Integer status) {
        Page<SysPost> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(postCode), SysPost::getPostCode, postCode)
                .like(StringUtils.hasText(postName), SysPost::getPostName, postName)
                .eq(status != null, SysPost::getStatus, status)
                .orderByAsc(SysPost::getSort);
        return PageResult.of(this.page(pageParam, wrapper));
    }

    @Override
    public List<SysPost> listAll() {
        return this.list(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getStatus, 1)
                .orderByAsc(SysPost::getSort));
    }

    @Override
    public void create(SysPost post) {
        // 检查岗位编码是否存在
        if (this.getOne(new LambdaQueryWrapper<SysPost>().eq(SysPost::getPostCode, post.getPostCode())) != null) {
            throw new BusinessException("岗位编码已存在");
        }
        if (post.getParentId() == null) {
            post.setParentId(0L);
        }
        this.save(post);
    }

    @Override
    public void update(SysPost post) {
        SysPost existPost = this.getById(post.getId());
        if (existPost == null) {
            throw new BusinessException("岗位不存在");
        }
        // 检查岗位编码是否存在
        SysPost byCode = this.getOne(new LambdaQueryWrapper<SysPost>().eq(SysPost::getPostCode, post.getPostCode()));
        if (byCode != null && !byCode.getId().equals(post.getId())) {
            throw new BusinessException("岗位编码已存在");
        }
        this.updateById(post);
    }

    @Override
    public void delete(Long id) {
        // 检查是否有子岗位
        long count = this.count(new LambdaQueryWrapper<SysPost>().eq(SysPost::getParentId, id));
        if (count > 0) {
            throw new BusinessException("存在子岗位，不允许删除");
        }
        this.removeById(id);
    }

    @Override
    public List<SysPost> tree() {
        List<SysPost> list = this.list(new LambdaQueryWrapper<SysPost>().orderByAsc(SysPost::getSort));
        return buildTree(list, 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(Long id, Long parentId) {
        SysPost post = this.getById(id);
        if (post == null) {
            throw new BusinessException("岗位不存在");
        }
        if (id.equals(parentId)) {
            throw new BusinessException("不能移动到自身");
        }
        post.setParentId(parentId);
        this.updateById(post);
    }

    private List<SysPost> buildTree(List<SysPost> list, Long parentId) {
        List<SysPost> tree = new ArrayList<>();
        for (SysPost post : list) {
            if (parentId.equals(post.getParentId())) {
                post.setChildren(buildTree(list, post.getId()));
                tree.add(post);
            }
        }
        return tree;
    }
}
