package com.zion.system.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zion.system.entity.Student;
import com.zion.system.mapper.StudentMapper;
import com.zion.system.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * 学生表 Service 实现
 * 
 * @author Zion
 * @date 2026-03-01
 */
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentMapper studentMapper;

    @Override
    public Page<Student> page(Integer page, Integer pageSize, Long id, String name, Integer status) {
        Page<Student> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        if (id != null) {
            wrapper.eq(Student::getId, id);
        }
        if (StringUtils.hasText(name)) {
            wrapper.eq(Student::getName, name);
        }
        if (status != null) {
            wrapper.eq(Student::getStatus, status);
        }
        wrapper.orderByDesc(Student::getId);
        return studentMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Student getById(Long id) {
        return studentMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(Student student) {
        studentMapper.insert(student);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Student student) {
        studentMapper.updateById(student);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long[] ids) {
        studentMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public List<Student> listForExport(List<Long> ids, Long id, String name, Integer status) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(Student::getId, ids);
        } else {
            if (id != null) {
                wrapper.eq(Student::getId, id);
            }
            if (StringUtils.hasText(name)) {
                wrapper.eq(Student::getName, name);
            }
            if (status != null) {
                wrapper.eq(Student::getStatus, status);
            }
        }
        wrapper.orderByDesc(Student::getId);
        return studentMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importData(MultipartFile file) {
        int success = 0;
        int fail = 0;
        List<String> errors = new ArrayList<>();
        try {
            List<Student> list = EasyExcel.read(file.getInputStream(), Student.class, null).sheet().doReadSync();
            for (int i = 0; i < list.size(); i++) {
                try {
                    Student item = list.get(i);
                    if (item != null) {
                        item.setId(null);
                        studentMapper.insert(item);
                        success++;
                    }
                } catch (Exception e) {
                    fail++;
                    errors.add("第" + (i + 2) + "行: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("fail", fail);
        result.put("errors", errors);
        return result;
    }
}
