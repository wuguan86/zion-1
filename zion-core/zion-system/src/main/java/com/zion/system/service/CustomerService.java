package com.zion.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zion.system.entity.Customer;

/**
 * 客户表 Service
 * 
 * @author Zion
 * @date 2026-02-02
 */
public interface CustomerService {

    /**
     * 分页查询
     */
    Page<Customer> page(Integer page, Integer pageSize, Long id, String name);

    /**
     * 根据ID查询
     */
    Customer getById(Long id);

    /**
     * 新增
     */
    void create(Customer customer);

    /**
     * 修改
     */
    void update(Customer customer);

    /**
     * 删除
     */
    void delete(Long[] ids);
}
