package com.zion.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zion.system.entity.Student;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 学生表 Service
 * 
 * @author Zion
 * @date 2026-03-01
 */
public interface StudentService {

    /**
     * 分页查询
     */
    Page<Student> page(Integer page, Integer pageSize, Long id, String name, Integer status);

    /**
     * 根据ID查询
     */
    Student getById(Long id);

    /**
     * 新增
     */
    void create(Student student);

    /**
     * 修改
     */
    void update(Student student);

    /**
     * 删除
     */
    void delete(Long[] ids);

    /**
     * 导出数据列表
     */
    List<Student> listForExport(java.util.List<Long> ids, Long id, String name, Integer status);

    /**
     * 导入数据
     */
    Map<String, Object> importData(MultipartFile file);
}
