package com.zion.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zion.common.exception.BusinessException;
import com.zion.common.result.PageResult;
import com.zion.system.entity.SysDictData;
import com.zion.system.mapper.SysDictDataMapper;
import com.zion.system.service.SysDictDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 字典数据服务实现
 */
@Service
@RequiredArgsConstructor
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements SysDictDataService {

    @Override
    public PageResult<SysDictData> page(Integer page, Integer pageSize, String dictType, String dictLabel, Integer status) {
        Page<SysDictData> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(dictType), SysDictData::getDictType, dictType)
                .like(StringUtils.hasText(dictLabel), SysDictData::getDictLabel, dictLabel)
                .eq(status != null, SysDictData::getStatus, status)
                .orderByAsc(SysDictData::getSort);
        return PageResult.of(this.page(pageParam, wrapper));
    }

    @Override
    public List<SysDictData> listByDictType(String dictType) {
        return baseMapper.selectByDictType(dictType);
    }

    @Override
    public void create(SysDictData dictData) {
        this.save(dictData);
    }

    @Override
    public void update(SysDictData dictData) {
        SysDictData existDictData = this.getById(dictData.getId());
        if (existDictData == null) {
            throw new BusinessException("字典数据不存在");
        }
        this.updateById(dictData);
    }

    @Override
    public void delete(Long id) {
        this.removeById(id);
    }
}
