package com.zion.system.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * IP地址工具类
 */
@Slf4j
public class IpUtils {

    /**
     * 太平洋网络IP查询API（国内访问稳定）
     */
    private static final String PCONLINE_API_URL = "https://whois.pconline.com.cn/ipJson.jsp?ip=%s&json=true";
    
    /**
     * 备用：ip-api.com
     */
    private static final String IP_API_URL = "http://ip-api.com/json/%s?lang=zh-CN";

    /**
     * 获取客户端真实IP地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (isInvalidIp(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isInvalidIp(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        // 本地回环地址处理
        if ("0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    private static boolean isInvalidIp(String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }

    /**
     * 根据IP获取地址
     * @param ip IP地址
     * @return 格式：中国 四川省 成都市
     */
    public static String getAddressByIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "未知";
        }
        // 内网IP
        if (isInternalIp(ip)) {
            return "内网IP";
        }
        
        // 优先使用太平洋网络API（国内访问稳定）
        String address = getAddressByPconline(ip);
        if (!"未知".equals(address)) {
            return address;
        }
        
        // 备用使用 ip-api.com
        return getAddressByIpApi(ip);
    }

    /**
     * 使用太平洋网络API查询IP地址
     */
    private static String getAddressByPconline(String ip) {
        try {
            String url = String.format(PCONLINE_API_URL, ip);
            String response = HttpUtil.get(url, 3000);
            if (response != null && !response.isEmpty()) {
                JSONObject json = JSONUtil.parseObj(response);
                String pro = json.getStr("pro", ""); // 省份
                String city = json.getStr("city", ""); // 城市
                String addr = json.getStr("addr", ""); // 完整地址
                
                if (pro != null && !pro.isEmpty()) {
                    StringBuilder address = new StringBuilder();
                    address.append("中国");
                    if (!pro.isEmpty()) {
                        address.append(" ").append(pro);
                    }
                    if (!city.isEmpty() && !city.equals(pro)) {
                        address.append(" ").append(city);
                    }
                    return address.toString();
                }
                // 如果是国外IP，返回完整地址
                if (addr != null && !addr.isEmpty()) {
                    return addr;
                }
            }
        } catch (Exception e) {
            log.warn("太平洋网络IP查询失败: {}", ip);
        }
        return "未知";
    }

    /**
     * 使用 ip-api.com 查询IP地址（备用）
     */
    private static String getAddressByIpApi(String ip) {
        try {
            String url = String.format(IP_API_URL, ip);
            String response = HttpUtil.get(url, 3000);
            if (response != null && !response.isEmpty()) {
                JSONObject json = JSONUtil.parseObj(response);
                if ("success".equals(json.getStr("status"))) {
                    String country = json.getStr("country", "");
                    String regionName = json.getStr("regionName", "");
                    String city = json.getStr("city", "");
                    return formatAddress(country, regionName, city);
                }
            }
        } catch (Exception e) {
            log.warn("ip-api.com IP查询失败: {}", ip);
        }
        return "未知";
    }

    /**
     * 格式化地址
     * @return 格式：中国 四川省 成都市
     */
    private static String formatAddress(String country, String region, String city) {
        StringBuilder address = new StringBuilder();
        if (country != null && !country.isEmpty()) {
            address.append(country);
        }
        if (region != null && !region.isEmpty() && !region.equals(country)) {
            if (address.length() > 0) {
                address.append(" ");
            }
            address.append(region);
        }
        if (city != null && !city.isEmpty() && !city.equals(region)) {
            if (address.length() > 0) {
                address.append(" ");
            }
            address.append(city);
        }
        return address.length() > 0 ? address.toString() : "未知";
    }

    /**
     * 判断是否为内网IP
     */
    public static boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        if ("127.0.0.1".equals(ip) || "localhost".equalsIgnoreCase(ip)) {
            return true;
        }
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            
            // 10.x.x.x
            if (first == 10) {
                return true;
            }
            // 172.16.x.x - 172.31.x.x
            if (first == 172 && second >= 16 && second <= 31) {
                return true;
            }
            // 192.168.x.x
            if (first == 192 && second == 168) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}
