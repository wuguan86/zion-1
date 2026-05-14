package com.zion.admin.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.zion.common.result.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.lang.management.*;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 系统监控控制器
 */
@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final StringRedisTemplate redisTemplate;

    /**
     * 获取在线用户列表
     */
    @GetMapping("/online/list")
    @SaCheckPermission("monitor:online:list")
    public Result<List<OnlineUser>> onlineList() {
        List<String> sessionIds = StpUtil.searchSessionId("", 0, -1, false);
        List<OnlineUser> onlineUsers = new ArrayList<>();

        for (String sessionId : sessionIds) {
            try {
                SaSession session = StpUtil.getSessionBySessionId(sessionId);
                if (session != null) {
                    OnlineUser user = new OnlineUser();
                    user.setTokenId(sessionId);
                    user.setLoginName(getSessionString(session, "loginName"));
                    user.setDeptName(getSessionString(session, "deptName"));
                    user.setIpaddr(getSessionString(session, "ipaddr"));
                    user.setLoginLocation(getSessionString(session, "loginLocation"));
                    user.setBrowser(getSessionString(session, "browser"));
                    user.setOs(getSessionString(session, "os"));
                    user.setStatus(getSessionInt(session, "status", 1));
                    long loginTime = getSessionLong(session, "loginTime", session.getCreateTime());
                    long lastAccessTime = getSessionLong(session, "lastAccessTime", loginTime);
                    user.setLoginTime(formatTime(loginTime));
                    user.setLastAccessTime(formatTime(lastAccessTime));
                    user.setTokenValue(getTokenValue(session));
                    onlineUsers.add(user);
                }
            } catch (Exception e) {
                // 忽略无效的session
            }
        }
        return Result.ok(onlineUsers);
    }

    /**
     * 强制下线
     */
    @DeleteMapping("/online/{tokenId}")
    @SaCheckPermission("monitor:online:forceLogout")
    public Result<Void> forceLogout(@PathVariable String tokenId) {
        String tokenValue = tokenId;
        try {
            SaSession session = StpUtil.getSessionBySessionId(tokenId);
            if (session != null) {
                String sessionToken = getTokenValue(session);
                if (StringUtils.hasText(sessionToken)) {
                    tokenValue = sessionToken;
                }
            }
        } catch (Exception e) {
            // 忽略非法的sessionId
        }
        StpUtil.logoutByTokenValue(tokenValue);
        StpUtil.kickoutByTokenValue(tokenValue);
        return Result.ok();
    }

    /**
     * 获取缓存统计（用于图表：内存、QPS、命中率、连接数）
     */
    @GetMapping("/cache/stats")
    @SaCheckPermission("monitor:cache:list")
    public Result<Map<String, Object>> cacheStats() {
        Properties info = redisTemplate.execute((RedisCallback<Properties>) connection ->
            connection.serverCommands().info());

        Map<String, Object> result = new HashMap<>();
        if (info != null) {
            long usedMemory = parseLong(info.getProperty("used_memory", "0"));
            long maxMemory = parseLong(info.getProperty("maxmemory", "0"));
            if (maxMemory == 0) maxMemory = usedMemory > 0 ? usedMemory * 2 : 1;

            long hits = parseLong(info.getProperty("keyspace_hits", "0"));
            long misses = parseLong(info.getProperty("keyspace_misses", "0"));
            long ops = parseLong(info.getProperty("instantaneous_ops_per_sec", "0"));
            long clients = parseLong(info.getProperty("connected_clients", "0"));

            double hitRate = (hits + misses) == 0 ? 1.0 : (double) hits / (hits + misses);

            result.put("usedMemory", usedMemory);
            result.put("maxMemory", maxMemory);
            result.put("ops", ops);
            result.put("hitRate", hitRate);
            result.put("connectedClients", clients);
        }
        return Result.ok(result);
    }

    private long parseLong(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 获取缓存监控信息
     */
    @GetMapping("/cache/info")
    @SaCheckPermission("monitor:cache:list")
    public Result<Map<String, Object>> cacheInfo() {
        Map<String, Object> result = new HashMap<>();

        Properties info = redisTemplate.execute((RedisCallback<Properties>) connection ->
            connection.serverCommands().info());

        if (info != null) {
            result.put("info", info);

            // 内存使用情况
            Map<String, Object> memory = new HashMap<>();
            memory.put("used_memory", info.getProperty("used_memory", "0"));
            memory.put("used_memory_human", info.getProperty("used_memory_human", "0"));
            memory.put("used_memory_peak", info.getProperty("used_memory_peak", "0"));
            memory.put("used_memory_peak_human", info.getProperty("used_memory_peak_human", "0"));
            result.put("memory", memory);

            // 命令统计
            Long dbSize = redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.serverCommands().dbSize());
            result.put("dbSize", dbSize);

            // 统计信息
            Map<String, Object> stats = new HashMap<>();
            stats.put("redis_version", info.getProperty("redis_version", "unknown"));
            stats.put("uptime_in_days", info.getProperty("uptime_in_days", "0"));
            stats.put("connected_clients", info.getProperty("connected_clients", "0"));
            stats.put("total_commands_processed", info.getProperty("total_commands_processed", "0"));
            stats.put("instantaneous_ops_per_sec", info.getProperty("instantaneous_ops_per_sec", "0"));
            result.put("stats", stats);
        }

        return Result.ok(result);
    }

    /**
     * 获取缓存键列表
     */
    @GetMapping("/cache/keys")
    @SaCheckPermission("monitor:cache:list")
    public Result<Set<String>> cacheKeys(@RequestParam(defaultValue = "*") String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return Result.ok(keys);
    }

    /**
     * 删除缓存
     */
    @DeleteMapping("/cache")
    @SaCheckPermission("monitor:cache:delete")
    public Result<Void> deleteCache(@RequestParam String key) {
        Boolean deleted = redisTemplate.delete(key);
        if (Boolean.TRUE.equals(deleted)) {
            return Result.ok();
        }
        return Result.fail("缓存键不存在或已被删除");
    }

    /**
     * 获取缓存详情
     */
    @GetMapping("/cache/value")
    @SaCheckPermission("monitor:cache:list")
    public Result<Map<String, Object>> getCacheValue(@RequestParam("key") String key) {
        String type = Objects.requireNonNull(redisTemplate.type(key)).code();
        Long ttl = redisTemplate.getExpire(key);
        Object value = null;

        if ("string".equals(type)) {
            value = redisTemplate.opsForValue().get(key);
        } else if ("list".equals(type)) {
            value = redisTemplate.opsForList().range(key, 0, -1);
        } else if ("set".equals(type)) {
            value = redisTemplate.opsForSet().members(key);
        } else if ("zset".equals(type)) {
            value = redisTemplate.opsForZSet().range(key, 0, -1);
        } else if ("hash".equals(type)) {
            value = redisTemplate.opsForHash().entries(key);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("type", type);
        result.put("ttl", ttl);
        result.put("value", value);

        return Result.ok(result);
    }


    /**
     * 获取服务器信息
     */
    @GetMapping("/server/info")
    @SaCheckPermission("monitor:server:list")
    public Result<Map<String, Object>> serverInfo() {
        Map<String, Object> result = new HashMap<>();

        // CPU信息
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Map<String, Object> cpu = new HashMap<>();
        cpu.put("name", osBean.getName());
        cpu.put("arch", osBean.getArch());
        cpu.put("availableProcessors", osBean.getAvailableProcessors());
        cpu.put("systemLoadAverage", osBean.getSystemLoadAverage());
        result.put("cpu", cpu);

        // 内存信息
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        Map<String, Object> memory = new HashMap<>();
        memory.put("heapInit", formatBytes(heapUsage.getInit()));
        memory.put("heapUsed", formatBytes(heapUsage.getUsed()));
        memory.put("heapMax", formatBytes(heapUsage.getMax()));
        memory.put("heapCommitted", formatBytes(heapUsage.getCommitted()));
        memory.put("nonHeapUsed", formatBytes(nonHeapUsage.getUsed()));
        result.put("memory", memory);

        // JVM信息
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        Map<String, Object> jvm = new HashMap<>();
        jvm.put("name", runtimeBean.getVmName());
        jvm.put("vendor", runtimeBean.getVmVendor());
        jvm.put("version", runtimeBean.getVmVersion());
        jvm.put("specVersion", runtimeBean.getSpecVersion());
        jvm.put("startTime", formatTime(runtimeBean.getStartTime()));
        jvm.put("uptime", formatDuration(runtimeBean.getUptime()));
        result.put("jvm", jvm);

        // 系统信息
        try {
            InetAddress addr = InetAddress.getLocalHost();
            Map<String, Object> sys = new HashMap<>();
            sys.put("hostName", addr.getHostName());
            sys.put("hostAddress", addr.getHostAddress());
            sys.put("osName", System.getProperty("os.name"));
            sys.put("osVersion", System.getProperty("os.version"));
            sys.put("userDir", System.getProperty("user.dir"));
            result.put("sys", sys);
        } catch (Exception e) {
            // ignore
        }

        // 磁盘信息
        List<Map<String, Object>> disks = new ArrayList<>();
        java.io.File[] roots = java.io.File.listRoots();
        for (java.io.File root : roots) {
            Map<String, Object> disk = new HashMap<>();
            disk.put("path", root.getPath());
            disk.put("total", formatBytes(root.getTotalSpace()));
            disk.put("free", formatBytes(root.getFreeSpace()));
            disk.put("usable", formatBytes(root.getUsableSpace()));
            disk.put("usedPercent", root.getTotalSpace() > 0
                ? String.format("%.2f", (root.getTotalSpace() - root.getFreeSpace()) * 100.0 / root.getTotalSpace())
                : "0");
            disks.add(disk);
        }
        result.put("disks", disks);

        return Result.ok(result);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatTime(long timestamp) {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            java.time.ZoneId.systemDefault()
        ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%d天%d小时%d分%d秒", days, hours, minutes, secs);
    }

    private String getSessionString(SaSession session, String key) {
        Object value = session.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private int getSessionInt(SaSession session, String key, int defaultValue) {
        Object value = session.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private long getSessionLong(SaSession session, String key, long defaultValue) {
        Object value = session.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String getTokenValue(SaSession session) {
        String token = session.getToken();
        if (StringUtils.hasText(token)) {
            return token;
        }
        Object loginId = session.getLoginId();
        if (loginId == null) {
            return "";
        }
        try {
            return StpUtil.getTokenValueByLoginId(loginId);
        } catch (Exception e) {
            return "";
        }
    }

    @Data
    public static class OnlineUser {
        private String tokenId;
        private String loginName;
        private String deptName;
        private String ipaddr;
        private String loginLocation;
        private String browser;
        private String os;
        private Integer status;
        private String loginTime;
        private String lastAccessTime;
        private String tokenValue;
    }
}
