package com.zion.gen.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zion.system.entity.SysMenu;
import com.zion.gen.entity.*;
import com.zion.gen.mapper.GenTableColumnMapper;
import com.zion.gen.mapper.GenTableMapper;
import com.zion.system.mapper.SysMenuMapper;
import com.zion.system.entity.SysRole;
import com.zion.system.entity.SysRoleMenu;
import com.zion.system.mapper.SysRoleMapper;
import com.zion.system.mapper.SysRoleMenuMapper;
import com.zion.gen.service.GenTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenTableServiceImpl implements GenTableService {

    private final GenTableMapper genTableMapper;
    private final GenTableColumnMapper genTableColumnMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysRoleMapper sysRoleMapper;

    // Java类型映射
    private static final Map<String, String> TYPE_MAP = new HashMap<>();
    static {
        TYPE_MAP.put("tinyint", "Integer");
        TYPE_MAP.put("smallint", "Integer");
        TYPE_MAP.put("mediumint", "Integer");
        TYPE_MAP.put("int", "Integer");
        TYPE_MAP.put("integer", "Integer");
        TYPE_MAP.put("bigint", "Long");
        TYPE_MAP.put("float", "Double");
        TYPE_MAP.put("double", "Double");
        TYPE_MAP.put("decimal", "BigDecimal");
        TYPE_MAP.put("bit", "Boolean");
        TYPE_MAP.put("char", "String");
        TYPE_MAP.put("varchar", "String");
        TYPE_MAP.put("tinytext", "String");
        TYPE_MAP.put("text", "String");
        TYPE_MAP.put("mediumtext", "String");
        TYPE_MAP.put("longtext", "String");
        TYPE_MAP.put("date", "LocalDate");
        TYPE_MAP.put("datetime", "LocalDateTime");
        TYPE_MAP.put("timestamp", "LocalDateTime");
        TYPE_MAP.put("time", "LocalTime");
        TYPE_MAP.put("json", "String");

        // 初始化 Velocity
        Properties props = new Properties();
        props.put("resource.loader.file.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        props.put("resource.loader", "class");
        props.put("resource.loader.class.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        props.put("input.encoding", "UTF-8");
        props.put("output.encoding", "UTF-8");
        Velocity.init(props);
    }

    @Override
    public Page<DatabaseTable> selectDbTableList(Integer page, Integer pageSize, String tableName) {
        int offset = (page - 1) * pageSize;
        List<DatabaseTable> list = genTableMapper.selectDbTableList(tableName, offset, pageSize);
        long total = genTableMapper.countDbTable(tableName);
        
        Page<DatabaseTable> result = new Page<>(page, pageSize);
        result.setRecords(list);
        result.setTotal(total);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importTable(String[] tableNames) {
        for (String tableName : tableNames) {
            // 查询表信息
            DatabaseTable dbTable = genTableMapper.selectDbTableByName(tableName);
            if (dbTable == null) {
                continue;
            }

            // 创建表记录
            GenTable table = new GenTable();
            table.setTableName(tableName);
            table.setTableComment(dbTable.getTableComment());
            table.setClassName(toClassName(tableName));
            table.setPackageName("com.zion.biz");
            table.setModuleName("biz");
            table.setBusinessName(toBusinessName(tableName));
            table.setFormLayout("vertical");
            table.setFunctionName(StrUtil.isNotBlank(dbTable.getTableComment()) ? 
                    dbTable.getTableComment() : toClassName(tableName));
            table.setAuthor("Zion");
            table.setGenType("crud");
            table.setFrontType("naive-ui");
            genTableMapper.insert(table);

            // 查询列信息
            List<DatabaseColumn> columns = genTableMapper.selectDbColumnsByTableName(tableName);
            for (DatabaseColumn col : columns) {
                GenTableColumn column = new GenTableColumn();
                column.setTableId(table.getId());
                column.setColumnName(col.getColumnName());
                column.setColumnComment(StrUtil.isNotBlank(col.getColumnComment()) ? 
                        col.getColumnComment() : col.getColumnName());
                column.setColumnType(col.getColumnType());
                column.setJavaType(toJavaType(col.getDataType()));
                column.setJavaField(toCamelCase(col.getColumnName()));
                column.setIsPk("PRI".equals(col.getColumnKey()) ? 1 : 0);
                column.setIsIncrement("auto_increment".equals(col.getExtra()) ? 1 : 0);
                column.setIsRequired("NO".equals(col.getIsNullable()) ? 1 : 0);
                column.setSort(col.getOrdinalPosition());

                // 设置默认显示配置
                String javaField = column.getJavaField();
                String columnName = column.getColumnName().toLowerCase();
                
                // 基础字段（不需要插入和编辑）
                boolean isBaseField = Arrays.asList("createBy", "createTime", "updateBy", "updateTime", "remark")
                        .contains(javaField);
                
                // 删除标记字段（不需要显示、插入、编辑、查询）
                boolean isDeleteField = Arrays.asList("deleted", "delFlag", "isDeleted", "del_flag", "is_deleted")
                        .contains(javaField) || columnName.contains("deleted") || columnName.contains("del_flag");
                
                column.setIsInsert(column.getIsPk() == 0 && !isBaseField && !isDeleteField ? 1 : 0);
                column.setIsEdit(column.getIsPk() == 0 && !isBaseField && !isDeleteField ? 1 : 0);
                column.setIsList(!isDeleteField ? 1 : 0);  // 删除标记不显示在列表
                column.setIsQuery(column.getIsPk() == 1 || "name".equals(javaField) || "status".equals(javaField) ? 1 : 0);
                column.setQueryType("EQ");
                column.setHtmlType(getHtmlType(column));
                column.setDictType(getDictType(column));

                genTableColumnMapper.insert(column);
            }
        }
    }

    @Override
    public Page<GenTable> page(Integer page, Integer pageSize, String tableName) {
        Page<GenTable> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<GenTable> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(tableName)) {
            wrapper.like(GenTable::getTableName, tableName);
        }
        wrapper.orderByDesc(GenTable::getCreateTime);
        return genTableMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public GenTable getTableById(Long id) {
        GenTable table = genTableMapper.selectById(id);
        if (table != null) {
            List<GenTableColumn> columns = genTableColumnMapper.selectByTableId(id);
            table.setColumns(columns);
            // 设置主键列
            columns.stream()
                    .filter(c -> c.getIsPk() == 1)
                    .findFirst()
                    .ifPresent(table::setPkColumn);
        }
        return table;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTable(GenTable table) {
        genTableMapper.updateById(table);
        if (table.getColumns() != null) {
            for (GenTableColumn column : table.getColumns()) {
                genTableColumnMapper.updateById(column);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTable(Long[] ids) {
        for (Long id : ids) {
            genTableMapper.deleteById(id);
            genTableColumnMapper.deleteByTableId(id);
        }
    }

    @Override
    public Map<String, String> previewCode(Long tableId) {
        GenTable table = getTableById(tableId);
        if (table == null) {
            throw new RuntimeException("表不存在");
        }
        
        VelocityContext context = prepareContext(table);
        Map<String, String> codeMap = new LinkedHashMap<>();
        
        // 生成各类代码
        List<String[]> templates = getTemplateList();
        for (String[] tpl : templates) {
            String code = renderTemplate(tpl[0], context);
            codeMap.put(tpl[1], code);
        }
        
        return codeMap;
    }

    @Override
    public byte[] generateCode(Long[] tableIds) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Long tableId : tableIds) {
                GenTable table = getTableById(tableId);
                if (table == null) continue;
                
                VelocityContext context = prepareContext(table);
                List<String[]> templates = getTemplateList();
                
                for (String[] tpl : templates) {
                    String code = renderTemplate(tpl[0], context);
                    String filePath = getFilePath(table, tpl[1]);
                    
                    zos.putNextEntry(new ZipEntry(filePath));
                    IoUtil.writeUtf8(zos, false, code);
                    zos.closeEntry();
                }
            }
        } catch (Exception e) {
            log.error("生成代码失败", e);
            throw new RuntimeException("生成代码失败");
        }
        return baos.toByteArray();
    }

    @Override
    public List<String> previewGenerateFiles(Long tableId) {
        GenTable table = getTableById(tableId);
        if (table == null) {
            throw new RuntimeException("表不存在");
        }
        
        List<String> files = new ArrayList<>();
        List<String[]> templates = getTemplateList();
        
        String projectRoot = System.getProperty("user.dir");
        if (projectRoot.endsWith("Zion-web")) {
            projectRoot = projectRoot.substring(0, projectRoot.length() - 8);
        }
        
        for (String[] tpl : templates) {
            String relativePath = getProjectFilePath(table, tpl[1], projectRoot);
            if (relativePath != null) {
                files.add(relativePath.replace(projectRoot, "").replace("\\", "/"));
            }
        }
        
        // 添加菜单信息
        String menuPath = table.getModuleName() + "/" + table.getBusinessName();
        LambdaQueryWrapper<SysMenu> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(SysMenu::getPath, menuPath).eq(SysMenu::getDeleted, 0);
        if (sysMenuMapper.selectCount(existWrapper) == 0) {
            files.add("[数据库] 将创建菜单: " + table.getFunctionName());
        } else {
            files.add("[数据库] 菜单已存在，将跳过创建");
        }
        
        return files;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> generateToProject(Long tableId) {
        GenTable table = getTableById(tableId);
        if (table == null) {
            throw new RuntimeException("表不存在");
        }
        
        List<String> generatedFiles = new ArrayList<>();
        VelocityContext context = prepareContext(table);
        List<String[]> templates = getTemplateList();
        
        // 获取项目根目录
        String projectRoot = System.getProperty("user.dir");
        // 如果是在 Zion-web 目录下运行，需要找到项目根目录
        if (projectRoot.endsWith("Zion-web")) {
            projectRoot = projectRoot.substring(0, projectRoot.length() - 8);
        }
        
        for (String[] tpl : templates) {
            String code = renderTemplate(tpl[0], context);
            String relativePath = getProjectFilePath(table, tpl[1], projectRoot);
            
            if (relativePath == null) {
                continue; // SQL 文件不写入项目
            }
            
            try {
                java.io.File file = new java.io.File(relativePath);
                // 创建目录
                java.io.File parentDir = file.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }
                // 写入文件
                cn.hutool.core.io.FileUtil.writeUtf8String(code, file);
                generatedFiles.add(relativePath.replace(projectRoot, "").replace("\\", "/"));
                log.info("生成文件: {}", relativePath);
            } catch (Exception e) {
                log.error("写入文件失败: {}", relativePath, e);
                throw new RuntimeException("写入文件失败: " + relativePath);
            }
        }
        
        // 自动创建菜单
        createMenus(table);
        generatedFiles.add("[数据库] 菜单数据已自动创建");
        
        return generatedFiles;
    }
    
    /**
     * 自动创建菜单
     */
    private void createMenus(GenTable table) {
        String moduleName = table.getModuleName();
        String businessName = table.getBusinessName();
        String functionName = table.getFunctionName();
        
        // 查找"开发工具"菜单作为父菜单
        Long parentMenuId = table.getParentMenuId();
        if (parentMenuId == null || parentMenuId == 0) {
            // 查询开发工具菜单
            LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysMenu::getName, "开发工具")
                   .eq(SysMenu::getDeleted, 0)
                   .last("LIMIT 1");
            SysMenu toolMenu = sysMenuMapper.selectOne(wrapper);
            parentMenuId = toolMenu != null ? toolMenu.getId() : 0L;
        }
        
        // 检查菜单是否已存在（避免重复创建）
        String menuPath = moduleName + "/" + businessName;
        LambdaQueryWrapper<SysMenu> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(SysMenu::getPath, menuPath)
                    .eq(SysMenu::getDeleted, 0);
        if (sysMenuMapper.selectCount(existWrapper) > 0) {
            log.info("菜单已存在，跳过创建: {}", menuPath);
            return;
        }
        
        // 创建主菜单
        SysMenu mainMenu = new SysMenu();
        mainMenu.setParentId(parentMenuId);
        mainMenu.setName(functionName);
        mainMenu.setType(2); // 菜单
        mainMenu.setPath(menuPath);
        mainMenu.setComponent(moduleName + "/" + businessName + "/index");
        mainMenu.setPermission(moduleName + ":" + businessName + ":list");
        mainMenu.setIcon("ListOutline");
        mainMenu.setSort(1);
        mainMenu.setVisible(1);
        mainMenu.setStatus(1);
        mainMenu.setIsFrame(0);
        mainMenu.setDeleted(0);
        sysMenuMapper.insert(mainMenu);
        
        Long mainMenuId = mainMenu.getId();
        
        // 自动授权给超级管理员 (role.code = admin)
        authorizeToAdmin(mainMenuId);
        
        // 创建按钮权限
        String[][] buttons = {
            {functionName + "查询", moduleName + ":" + businessName + ":list", "1"},
            {functionName + "详情", moduleName + ":" + businessName + ":query", "2"},
            {functionName + "新增", moduleName + ":" + businessName + ":add", "3"},
            {functionName + "修改", moduleName + ":" + businessName + ":edit", "4"},
            {functionName + "删除", moduleName + ":" + businessName + ":remove", "5"}
        };
        
        for (String[] btn : buttons) {
            SysMenu btnMenu = new SysMenu();
            btnMenu.setParentId(mainMenuId);
            btnMenu.setName(btn[0]);
            btnMenu.setType(3); // 按钮
            btnMenu.setPermission(btn[1]);
            btnMenu.setSort(Integer.parseInt(btn[2]));
            btnMenu.setVisible(1);
            btnMenu.setStatus(1);
            btnMenu.setIsFrame(0);
            btnMenu.setDeleted(0);
            sysMenuMapper.insert(btnMenu);
            
            // 按钮也授权给超级管理员
            authorizeToAdmin(btnMenu.getId());
        }
        
        log.info("菜单创建成功并已授权给超级管理员: {}", functionName);
    }

    /**
     * 授权给超级管理员
     */
    private void authorizeToAdmin(Long menuId) {
        // 查找超级管理员角色 (role.code = admin)
        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(SysRole::getCode, "admin")
                .last("LIMIT 1");
        SysRole adminRole = sysRoleMapper.selectOne(roleWrapper);
        if (adminRole == null) {
            log.warn("未找到超级管理员角色(role.code=admin)，跳过菜单授权: menuId={}", menuId);
            return;
        }

        // 避免重复授权
        LambdaQueryWrapper<SysRoleMenu> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(SysRoleMenu::getRoleId, adminRole.getId())
                .eq(SysRoleMenu::getMenuId, menuId)
                .last("LIMIT 1");
        if (sysRoleMenuMapper.selectCount(existWrapper) > 0) {
            return;
        }

        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setRoleId(adminRole.getId());
        roleMenu.setMenuId(menuId);
        sysRoleMenuMapper.insert(roleMenu);
    }

    @Override
    public List<String> previewRemoveFiles(Long tableId) {
        GenTable table = getTableById(tableId);
        if (table == null) {
            throw new RuntimeException("表不存在");
        }
        
        List<String> files = new ArrayList<>();
        
        String projectRoot = System.getProperty("user.dir");
        if (projectRoot.endsWith("Zion-web")) {
            projectRoot = projectRoot.substring(0, projectRoot.length() - 8);
        }
        
        // 检查哪些文件存在
        List<String[]> templates = getTemplateList();
        for (String[] tpl : templates) {
            String filePath = getProjectFilePath(table, tpl[1], projectRoot);
            if (filePath == null) {
                continue;
            }
            
            java.io.File file = new java.io.File(filePath);
            if (file.exists()) {
                files.add(filePath.replace(projectRoot, "").replace("\\", "/"));
            }
        }
        
        // 检查菜单是否存在
        String menuPath = table.getModuleName() + "/" + table.getBusinessName();
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getPath, menuPath).eq(SysMenu::getDeleted, 0);
        if (sysMenuMapper.selectCount(wrapper) > 0) {
            files.add("[数据库] 将删除菜单: " + table.getFunctionName());
        }
        
        if (files.isEmpty()) {
            files.add("没有找到需要删除的文件");
        }
        
        return files;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> removeGeneratedCode(Long tableId) {
        GenTable table = getTableById(tableId);
        if (table == null) {
            throw new RuntimeException("表不存在");
        }
        
        List<String> removedFiles = new ArrayList<>();
        
        // 获取项目根目录
        String projectRoot = System.getProperty("user.dir");
        if (projectRoot.endsWith("Zion-web")) {
            projectRoot = projectRoot.substring(0, projectRoot.length() - 8);
        }
        
        // 获取所有生成的文件路径
        List<String[]> templates = getTemplateList();
        for (String[] tpl : templates) {
            String filePath = getProjectFilePath(table, tpl[1], projectRoot);
            if (filePath == null) {
                continue;
            }
            
            java.io.File file = new java.io.File(filePath);
            if (file.exists()) {
                if (file.delete()) {
                    removedFiles.add(filePath.replace(projectRoot, "").replace("\\", "/"));
                    log.info("删除文件: {}", filePath);
                    
                    // 如果是 vue 文件，尝试删除空的父目录
                    if (filePath.endsWith(".vue")) {
                        java.io.File parentDir = file.getParentFile();
                        if (parentDir.isDirectory() && parentDir.list() != null && parentDir.list().length == 0) {
                            parentDir.delete();
                            log.info("删除空目录: {}", parentDir.getPath());
                        }
                    }
                } else {
                    log.warn("删除文件失败: {}", filePath);
                }
            }
        }
        
        // 删除对应的菜单
        removeMenus(table);
        removedFiles.add("[数据库] 菜单数据已删除");
        
        return removedFiles;
    }
    
    /**
     * 删除对应的菜单
     */
    private void removeMenus(GenTable table) {
        String moduleName = table.getModuleName();
        String businessName = table.getBusinessName();
        String menuPath = moduleName + "/" + businessName;
        
        // 查找主菜单
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getPath, menuPath)
               .eq(SysMenu::getDeleted, 0);
        SysMenu mainMenu = sysMenuMapper.selectOne(wrapper);
        
        if (mainMenu != null) {
            // 删除子菜单（按钮权限）
            LambdaQueryWrapper<SysMenu> childWrapper = new LambdaQueryWrapper<>();
            childWrapper.eq(SysMenu::getParentId, mainMenu.getId());
            sysMenuMapper.delete(childWrapper);
            
            // 删除主菜单
            sysMenuMapper.deleteById(mainMenu.getId());
            
            log.info("菜单删除成功: {}", menuPath);
        }
    }

    /**
     * 获取项目中的实际文件路径
     */
    private String getProjectFilePath(GenTable table, String fileName, String projectRoot) {
        String className = table.getClassName();
        String moduleName = table.getModuleName();
        String businessName = table.getBusinessName();
        
        return switch (fileName) {
            case "Entity.java" -> projectRoot + "/zion-core/zion-biz/src/main/java/com/Zion/biz/entity/" + className + ".java";
            case "Mapper.java" -> projectRoot + "/zion-core/zion-biz/src/main/java/com/Zion/biz/mapper/" + className + "Mapper.java";
            case "Service.java" -> projectRoot + "/zion-core/zion-biz/src/main/java/com/Zion/biz/service/" + className + "Service.java";
            case "ServiceImpl.java" -> projectRoot + "/zion-core/zion-biz/src/main/java/com/Zion/biz/service/impl/" + className + "ServiceImpl.java";
            case "Controller.java" -> projectRoot + "/zion-core/zion-biz/src/main/java/com/Zion/biz/controller/" + className + "Controller.java";
            case "api.ts" -> projectRoot + "/zion-ui/src/api/" + businessName + ".ts";
            case "index.vue" -> projectRoot + "/zion-ui/src/views/" + moduleName + "/" + businessName + "/index.vue";
            case "menu.sql" -> null; // SQL 文件不自动写入
            default -> null;
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncTable(Long tableId) {
        GenTable table = genTableMapper.selectById(tableId);
        if (table == null) {
            throw new RuntimeException("表不存在");
        }
        
        // 获取数据库最新列信息
        List<DatabaseColumn> dbColumns = genTableMapper.selectDbColumnsByTableName(table.getTableName());
        List<GenTableColumn> existColumns = genTableColumnMapper.selectByTableId(tableId);
        
        // 列名映射
        Map<String, GenTableColumn> existColumnMap = new HashMap<>();
        for (GenTableColumn col : existColumns) {
            existColumnMap.put(col.getColumnName(), col);
        }
        
        // 同步列
        for (DatabaseColumn dbCol : dbColumns) {
            GenTableColumn existCol = existColumnMap.get(dbCol.getColumnName());
            if (existCol != null) {
                // 更新已存在的列
                existCol.setColumnType(dbCol.getColumnType());
                existCol.setJavaType(toJavaType(dbCol.getDataType()));
                
                // 检查是否为删除标记字段，如果是则更新配置
                String javaField = existCol.getJavaField();
                String columnNameLower = dbCol.getColumnName().toLowerCase();
                boolean isDeleteField = Arrays.asList("deleted", "delFlag", "isDeleted", "del_flag", "is_deleted")
                        .contains(javaField) || columnNameLower.contains("deleted") || columnNameLower.contains("del_flag");
                if (isDeleteField) {
                    existCol.setIsInsert(0);
                    existCol.setIsEdit(0);
                    existCol.setIsList(0);
                    existCol.setIsQuery(0);
                }
                
                genTableColumnMapper.updateById(existCol);
                existColumnMap.remove(dbCol.getColumnName());
            } else {
                // 新增列
                GenTableColumn newCol = new GenTableColumn();
                newCol.setTableId(tableId);
                newCol.setColumnName(dbCol.getColumnName());
                newCol.setColumnComment(StrUtil.isNotBlank(dbCol.getColumnComment()) ? 
                        dbCol.getColumnComment() : dbCol.getColumnName());
                newCol.setColumnType(dbCol.getColumnType());
                newCol.setJavaType(toJavaType(dbCol.getDataType()));
                String javaField = toCamelCase(dbCol.getColumnName());
                newCol.setJavaField(javaField);
                newCol.setIsPk("PRI".equals(dbCol.getColumnKey()) ? 1 : 0);
                newCol.setIsIncrement("auto_increment".equals(dbCol.getExtra()) ? 1 : 0);
                newCol.setIsRequired("NO".equals(dbCol.getIsNullable()) ? 1 : 0);
                
                // 判断是否为删除标记字段
                String columnNameLower = dbCol.getColumnName().toLowerCase();
                boolean isDeleteField = Arrays.asList("deleted", "delFlag", "isDeleted", "del_flag", "is_deleted")
                        .contains(javaField) || columnNameLower.contains("deleted") || columnNameLower.contains("del_flag");
                
                newCol.setIsInsert(isDeleteField ? 0 : 1);
                newCol.setIsEdit(isDeleteField ? 0 : 1);
                newCol.setIsList(isDeleteField ? 0 : 1);
                newCol.setIsQuery(0);
                newCol.setQueryType("EQ");
                newCol.setHtmlType(getHtmlType(newCol));
                newCol.setDictType(getDictType(newCol));
                newCol.setSort(dbCol.getOrdinalPosition());
                genTableColumnMapper.insert(newCol);
            }
        }
        
        // 删除数据库中已不存在的列
        for (GenTableColumn col : existColumnMap.values()) {
            genTableColumnMapper.deleteById(col.getId());
        }
    }

    // ========== 工具方法 ==========

    /**
     * 表名转类名
     */
    private String toClassName(String tableName) {
        // 去除表前缀
        String name = tableName;
        if (name.contains("_")) {
            String prefix = name.substring(0, name.indexOf("_"));
            if (prefix.length() <= 4) {
                name = name.substring(name.indexOf("_") + 1);
            }
        }
        return toPascalCase(name);
    }

    /**
     * 表名转业务名
     */
    private String toBusinessName(String tableName) {
        String name = tableName;
        if (name.contains("_")) {
            String prefix = name.substring(0, name.indexOf("_"));
            if (prefix.length() <= 4) {
                name = name.substring(name.indexOf("_") + 1);
            }
        }
        return toCamelCase(name);
    }

    /**
     * 下划线转驼峰
     */
    private String toCamelCase(String name) {
        if (name == null || name.isEmpty()) return name;
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                upperNext = true;
            } else {
                if (upperNext) {
                    result.append(Character.toUpperCase(c));
                    upperNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        return result.toString();
    }

    /**
     * 下划线转帕斯卡命名
     */
    private String toPascalCase(String name) {
        String camel = toCamelCase(name);
        if (camel == null || camel.isEmpty()) return camel;
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    /**
     * 数据库类型转 Java 类型
     */
    private String toJavaType(String dbType) {
        return TYPE_MAP.getOrDefault(dbType.toLowerCase(), "String");
    }

    /**
     * 获取 HTML 类型
     */
    private String getHtmlType(GenTableColumn column) {
        String javaType = column.getJavaType();
        String columnName = column.getColumnName().toLowerCase();
        
        if (columnName.contains("status") || columnName.contains("type") || columnName.contains("sex") || columnName.contains("gender")) {
            return "select";
        }
        if (columnName.contains("image") || columnName.contains("img") || columnName.contains("avatar") || columnName.contains("logo")) {
            return "imageUpload";
        }
        if (columnName.contains("file") || columnName.contains("attachment")) {
            return "fileUpload";
        }
        if (columnName.contains("content") || columnName.contains("remark") || columnName.contains("desc")) {
            return "textarea";
        }
        if ("LocalDateTime".equals(javaType) || "LocalDate".equals(javaType)) {
            return "datetime";
        }
        if ("Integer".equals(javaType) || "Long".equals(javaType)) {
            return "input";
        }
        return "input";
    }

    /**
     * 根据字段名推断字典类型（仅对 select/radio/checkbox 有效）
     */
    private String getDictType(GenTableColumn column) {
        if (!"select".equals(column.getHtmlType()) && !"radio".equals(column.getHtmlType()) && !"checkbox".equals(column.getHtmlType())) {
            return "";
        }
        String columnName = column.getColumnName().toLowerCase();
        String javaField = column.getJavaField().toLowerCase();
        if (columnName.contains("gender") || columnName.contains("sex") || javaField.contains("gender") || javaField.contains("sex")) {
            return "sex";
        }
        if (columnName.contains("status") || javaField.contains("status")) {
            return "sys_status";
        }
        if (columnName.contains("yes_no") || columnName.contains("yn") || javaField.contains("yesno")) {
            return "sys_yes_no";
        }
        return "";
    }

    /**
     * 准备模板上下文
     */
    private VelocityContext prepareContext(GenTable table) {
        VelocityContext context = new VelocityContext();
        context.put("tableName", table.getTableName());
        context.put("tableComment", table.getTableComment());
        context.put("className", table.getClassName());
        context.put("classname", StrUtil.lowerFirst(table.getClassName()));
        context.put("packageName", table.getPackageName());
        context.put("moduleName", table.getModuleName());
        context.put("businessName", table.getBusinessName());
        context.put("functionName", table.getFunctionName());
        context.put("author", table.getAuthor());
        context.put("datetime", java.time.LocalDate.now().toString());
        context.put("columns", table.getColumns());
        context.put("pkColumn", table.getPkColumn());
        context.put("table", table);
        context.put("formLayout", table.getFormLayout() != null ? table.getFormLayout() : "vertical");
        
        // 判断是否需要导入 BigDecimal、LocalDateTime 等
        boolean hasBigDecimal = false;
        boolean hasLocalDateTime = false;
        boolean hasLocalDate = false;
        for (GenTableColumn col : table.getColumns()) {
            if ("BigDecimal".equals(col.getJavaType())) hasBigDecimal = true;
            if ("LocalDateTime".equals(col.getJavaType())) hasLocalDateTime = true;
            if ("LocalDate".equals(col.getJavaType())) hasLocalDate = true;
        }
        context.put("hasBigDecimal", hasBigDecimal);
        context.put("hasLocalDateTime", hasLocalDateTime);
        context.put("hasLocalDate", hasLocalDate);
        
        return context;
    }

    /**
     * 获取模板列表 [模板路径, 文件名称]
     */
    private List<String[]> getTemplateList() {
        List<String[]> templates = new ArrayList<>();
        templates.add(new String[]{"templates/gen/java/entity.java.vm", "Entity.java"});
        templates.add(new String[]{"templates/gen/java/mapper.java.vm", "Mapper.java"});
        templates.add(new String[]{"templates/gen/java/service.java.vm", "Service.java"});
        templates.add(new String[]{"templates/gen/java/serviceImpl.java.vm", "ServiceImpl.java"});
        templates.add(new String[]{"templates/gen/java/controller.java.vm", "Controller.java"});
        templates.add(new String[]{"templates/gen/vue/api.ts.vm", "api.ts"});
        templates.add(new String[]{"templates/gen/vue/index.vue.vm", "index.vue"});
        templates.add(new String[]{"templates/gen/sql/menu.sql.vm", "menu.sql"});
        return templates;
    }

    /**
     * 渲染模板
     */
    private String renderTemplate(String templatePath, VelocityContext context) {
        try {
            Template template = Velocity.getTemplate(templatePath, "UTF-8");
            StringWriter writer = new StringWriter();
            template.merge(context, writer);
            return writer.toString();
        } catch (Exception e) {
            log.error("渲染模板失败: {}", templatePath, e);
            return "// 模板渲染失败: " + e.getMessage();
        }
    }

    /**
     * 获取生成文件路径
     */
    private String getFilePath(GenTable table, String fileName) {
        String packagePath = table.getPackageName().replace(".", "/");
        String className = table.getClassName();
        
        return switch (fileName) {
            case "Entity.java" -> packagePath + "/entity/" + className + ".java";
            case "Mapper.java" -> packagePath + "/mapper/" + className + "Mapper.java";
            case "Service.java" -> packagePath + "/service/" + className + "Service.java";
            case "ServiceImpl.java" -> packagePath + "/service/impl/" + className + "ServiceImpl.java";
            case "Controller.java" -> packagePath + "/controller/" + className + "Controller.java";
            case "api.ts" -> "frontend/api/" + table.getBusinessName() + ".ts";
            case "index.vue" -> "frontend/views/" + table.getModuleName() + "/" + table.getBusinessName() + "/index.vue";
            case "menu.sql" -> "sql/" + table.getTableName() + "_menu.sql";
            default -> fileName;
        };
    }
}
