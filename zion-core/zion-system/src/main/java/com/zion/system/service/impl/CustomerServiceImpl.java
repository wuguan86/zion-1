package com.zion.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zion.system.entity.Customer;
import com.zion.system.mapper.CustomerMapper;
import com.zion.system.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 客户表 Service 实现
 * 
 * @author Zion
 * @date 2026-02-02
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;

    @Override
    public Page<Customer> page(Integer page, Integer pageSize, Long id, String name) {
        Page<Customer> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (id != null) {
            wrapper.eq(Customer::getId, id);
        }
        if (StringUtils.hasText(name)) {
            wrapper.eq(Customer::getName, name);
        }
        wrapper.orderByDesc(Customer::getId);
        return customerMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Customer getById(Long id) {
        return customerMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(Customer customer) {
        customerMapper.insert(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Customer customer) {
        customerMapper.updateById(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long[] ids) {
        customerMapper.deleteBatchIds(Arrays.asList(ids));
    }
}
