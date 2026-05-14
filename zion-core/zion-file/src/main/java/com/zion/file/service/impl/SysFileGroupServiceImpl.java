package com.zion.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zion.file.entity.SysFile;
import com.zion.file.entity.SysFileGroup;
import com.zion.file.mapper.SysFileGroupMapper;
import com.zion.file.mapper.SysFileMapper;
import com.zion.file.service.SysFileGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文件分组 Service 实现
 */
@Service
@RequiredArgsConstructor
public class SysFileGroupServiceImpl extends ServiceImpl<SysFileGroupMapper, SysFileGroup> implements SysFileGroupService {

    private final SysFileMapper fileMapper;

    @Override
    public List<SysFileGroup> listWithFileCount() {
        return baseMapper.selectListWithFileCount();
    }

    @Override
    public Integer getUngroupedFileCount() {
        return baseMapper.selectUngroupedFileCount();
    }

    @Override
    public void create(SysFileGroup group) {
        if (group.getSort() == null) {
            group.setSort(0);
        }
        save(group);
    }

    @Override
    public void update(SysFileGroup group) {
        updateById(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 将该分组下的文件移动到未分组
        fileMapper.update(null, new LambdaUpdateWrapper<SysFile>()
                .set(SysFile::getGroupId, null)
                .eq(SysFile::getGroupId, id));
        // 删除分组
        removeById(id);
    }
}
