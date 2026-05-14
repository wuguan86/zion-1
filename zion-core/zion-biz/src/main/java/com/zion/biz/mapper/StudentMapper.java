package com.zion.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.system.entity.Student;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生表 Mapper
 * 
 * @author Zion
 * @date 2026-03-01
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {

}
