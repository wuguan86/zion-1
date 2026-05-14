package com.zion.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.alibaba.excel.EasyExcel;
import com.zion.common.result.PageResult;
import com.zion.common.result.Result;
import com.zion.system.annotation.Log;
import com.zion.system.annotation.Log.BusinessType;
import com.zion.system.entity.Student;
import com.zion.system.service.StudentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学生表
 * 
 * @author Zion
 * @date 2026-03-01
 */
@RestController
@RequestMapping("/system/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    @SaCheckPermission("system:student:list")
    public Result<PageResult<Student>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        var result = studentService.page(page, pageSize, id, name, status);
        return Result.ok(PageResult.of(result));
    }

    /**
     * 获取详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("system:student:query")
    public Result<Student> getInfo(@PathVariable Long id) {
        return Result.ok(studentService.getById(id));
    }

    /**
     * 新增
     */
    @PostMapping
    @SaCheckPermission("system:student:add")
    @Log(title = "学生表", businessType = BusinessType.INSERT)
    public Result<Void> add(@RequestBody Student student) {
        studentService.create(student);
        return Result.ok();
    }

    /**
     * 修改
     */
    @PutMapping
    @SaCheckPermission("system:student:edit")
    @Log(title = "学生表", businessType = BusinessType.UPDATE)
    public Result<Void> edit(@RequestBody Student student) {
        studentService.update(student);
        return Result.ok();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{ids}")
    @SaCheckPermission("system:student:remove")
    @Log(title = "学生表", businessType = BusinessType.DELETE)
    public Result<Void> remove(@PathVariable Long[] ids) {
        studentService.delete(ids);
        return Result.ok();
    }

    /**
     * 导出
     */
    @GetMapping("/export")
    @SaCheckPermission("system:student:export")
    @Log(title = "学生表", businessType = BusinessType.EXPORT)
    public void export(HttpServletResponse response,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String ids) throws IOException {
        List<Long> idList = null;
        if (ids != null && !ids.isEmpty()) {
            idList = Arrays.stream(ids.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                    .map(s -> Long.parseLong(s)).collect(Collectors.toList());
        }
        List<Student> list = studentService.listForExport(idList, id, name, status);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("学生表数据", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), Student.class).sheet("学生表").doWrite(list);
    }

    /**
     * 导入
     */
    @PostMapping("/import")
    @SaCheckPermission("system:student:import")
    @Log(title = "学生表", businessType = BusinessType.IMPORT)
    public Result<Map<String, Object>> importData(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = studentService.importData(file);
        return Result.ok(result);
    }

    /**
     * 下载导入模板
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("学生表导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), Student.class).sheet("学生表").doWrite(new ArrayList<>());
    }
}
