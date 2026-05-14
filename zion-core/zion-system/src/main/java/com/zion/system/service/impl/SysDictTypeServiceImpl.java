package com.zion.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zion.common.exception.BusinessException;
import com.zion.common.result.PageResult;
import com.zion.system.entity.SysDictType;
import com.zion.system.mapper.SysDictTypeMapper;
import com.zion.system.service.SysDictTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 字典类型服务实现
 */
@Service
@RequiredArgsConstructor
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements SysDictTypeService {

    @Override
    public PageResult<SysDictType> page(Integer page, Integer pageSize, String dictName, String dictType, Integer status) {
        Page<SysDictType> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(dictName), SysDictType::getDictName, dictName)
                .like(StringUtils.hasText(dictType), SysDictType::getDictType, dictType)
                .eq(status != null, SysDictType::getStatus, status)
                .orderByDesc(SysDictType::getCreateTime);
        return PageResult.of(this.page(pageParam, wrapper));
    }

    @Override
    public List<SysDictType> listAll() {
        return this.list(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getStatus, 1)
                .orderByDesc(SysDictType::getCreateTime));
    }

    @Override
    public void create(SysDictType dictType) {
        // 检查字典类型是否存在
        if (this.getOne(new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, dictType.getDictType())) != null) {
            throw new BusinessException("字典类型已存在");
        }
        this.save(dictType);
    }

    @Override
    public void update(SysDictType dictType) {
        SysDictType existDictType = this.getById(dictType.getId());
        if (existDictType == null) {
            throw new BusinessException("字典类型不存在");
        }
        // 检查字典类型是否存在
        SysDictType byType = this.getOne(new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, dictType.getDictType()));
        if (byType != null && !byType.getId().equals(dictType.getId())) {
            throw new BusinessException("字典类型已存在");
        }
        this.updateById(dictType);
    }

    @Override
    public void delete(Long id) {
        this.removeById(id);
    }
}
