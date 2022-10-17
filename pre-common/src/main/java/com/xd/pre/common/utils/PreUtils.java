package com.xd.pre.common.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PreUtils {
    /**
     * 生成订单号
     *
     * @return
     */
    public static String productOrderId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String newDate = sdf.format(new Date());
        String result = "";
        Random random = new Random();
        for (int i = 0; i < 4; i++) {
            result += random.nextInt(10);
        }
        return newDate + result;
    }

    /**
     * 随机指定范围内N个不重复的数
     * 最简单最基本的方法
     *
     * @param min 指定范围最小值
     * @param max 指定范围最大值
     * @param n   随机数个数
     */
    public static int[] randomCommon(int min, int max, int n) {
        if (n > (max - min + 1) || max < min) {
            return null;
        }
        int[] result = new int[n];
        int count = 0;
        while (count < n) {
            int num = (int) (Math.random() * (max - min)) + min;
            boolean flag = true;
            for (int j = 0; j < n; j++) {
                if (num == result[j]) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                result[count] = num;
                count++;
            }
        }
        return result;
    }

    /**
     * 随机生成字符串
     *
     * @param length
     * @return
     */
    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public static String get_pt_pin(String Cookie) {
        try {
            String[] split = Cookie.split(";");
            for (int i = 0; i < split.length; i++) {
                String trim = split[i].trim();
                if (StrUtil.isNotBlank(trim) && trim.contains("pt_pin=")) {
                    return trim;
                }
                if (StrUtil.isNotBlank(trim) && trim.contains("pin=")) {
                    return trim;
                }
            }
        } catch (Exception e) {
            log.error("解析ck失败msg:[Cookie:{}]", Cookie);
        }
        return null;

    }

    /**
     * 获取字符串中的数字 分段提取
     *
     * @param str
     * @return
     */
    public static List<String> getNum(String str) {
        String regex = "(\\d+)";
        List<String> nums = new LinkedList<>();
        Pattern r = Pattern.compile(regex);
        Matcher m = r.matcher(str);
        while (m.find()) {
            nums.add(m.group());
        }
        return nums;
    }

    public static Map<String, String> getCookies(String ckContext) {
        Map<String, String> cookies = new HashMap<>();
        String[] split = ckContext.split(";");
        for (String ckKey : split) {
            String[] keyAndValue = ckKey.trim().split("=");
            if (keyAndValue.length == 2) {
                cookies.put(keyAndValue[0], keyAndValue[1]);
            }
        }
        return cookies;
    }


    // 正确的IP拿法，即优先拿site-local地址
    public static InetAddress getLocalHostLANAddress() {
        try {
            InetAddress candidateAddress = null;
            // 遍历所有的网络接口
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // 在所有的接口下再遍历IP
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {// 排除loopback类型地址
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // site-local类型的地址未被发现，先记录候选地址
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            // 如果没有发现 non-loopback地址.只能用最次选的方案
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析url
     *
     * @param url
     * @return
     */
    public static UrlEntity parseUrl(String url) {
        try {
            UrlEntity entity = new UrlEntity();
            if (url == null) {
                return entity;
            }
            url = url.trim();
            if (url.equals("")) {
                return entity;
            }
            String[] urlParts = url.split("\\?");
            entity.baseUrl = urlParts[0];
            //没有参数
            if (urlParts.length == 1) {
                return entity;
            }
            //有参数
            String[] params = urlParts[1].split("&");
            entity.params = new HashMap<>();
            for (String param : params) {
                String[] keyValue = param.split("=");
                entity.params.put(keyValue[0], keyValue[1]);
            }
            return entity;
        } catch (Exception e) {

        }
        return null;

    }

    public static String getIPAddress(HttpServletRequest request) {
        String ip = null;

        //X-Forwarded-For：Squid 服务代理
        String ipAddresses = request.getHeader("X-Forwarded-For");
        if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
            //Proxy-Client-IP：apache 服务代理
            ipAddresses = request.getHeader("Proxy-Client-IP");
        }

        if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
            //WL-Proxy-Client-IP：weblogic 服务代理
            ipAddresses = request.getHeader("WL-Proxy-Client-IP");
        }

        if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
            //HTTP_CLIENT_IP：有些代理服务器
            ipAddresses = request.getHeader("HTTP_CLIENT_IP");
        }

        if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
            //X-Real-IP：nginx服务代理
            ipAddresses = request.getHeader("X-Real-IP");
        }

        //有些网络通过多层代理，那么获取到的ip就会有多个，一般都是通过逗号（,）分割开来，并且第一个ip为客户端的真实IP
        if (ipAddresses != null && ipAddresses.length() != 0) {
            ip = ipAddresses.split(",")[0];
        }

        //还是不能获取到，最后再通过request.getRemoteAddr();获取
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    public static String getSign(String orderId) {
        String md5 = DigestUtils.md5DigestAsHex(orderId.getBytes());
        md5 = DigestUtils.md5DigestAsHex(md5.getBytes());
        return md5;
    }

    /**
     * 获取一个随机IP
     */
    public static String getRandomIp() {

        // 指定 IP 范围
        int[][] range = {
                {607649792, 608174079}, // 36.56.0.0-36.63.255.255
                {1038614528, 1039007743}, // 61.232.0.0-61.237.255.255
                {1783627776, 1784676351}, // 106.80.0.0-106.95.255.255
                {2035023872, 2035154943}, // 121.76.0.0-121.77.255.255
                {2078801920, 2079064063}, // 123.232.0.0-123.235.255.255
                {-1950089216, -1948778497}, // 139.196.0.0-139.215.255.255
                {-1425539072, -1425014785}, // 171.8.0.0-171.15.255.255
                {-1236271104, -1235419137}, // 182.80.0.0-182.92.255.255
                {-770113536, -768606209}, // 210.25.0.0-210.47.255.255
                {-569376768, -564133889}, // 222.16.0.0-222.95.255.255
        };

        Random random = new Random();
        int index = random.nextInt(10);
        String ip = num2ip(range[index][0] + random.nextInt(range[index][1] - range[index][0]));
        return ip;
    }

    /*
     * 将十进制转换成IP地址
     */
    public static String num2ip(int ip) {
        int[] b = new int[4];
        b[0] = (ip >> 24) & 0xff;
        b[1] = (ip >> 16) & 0xff;
        b[2] = (ip >> 8) & 0xff;
        b[3] = ip & 0xff;
        // 拼接 IP
        String x = b[0] + "." + b[1] + "." + b[2] + "." + b[3];
        return x;
    }

    public static String getAsciiSort(Map<String, Object> map) {
        // 移除值为空的
        map.entrySet().removeIf(entry -> Objects.isNull(entry.getValue()) || "".equals(entry.getValue()));

        List<Map.Entry<String, Object>> infoIds = new ArrayList<Map.Entry<String, Object>>(map.entrySet());
        // 对所有传入参数按照字段名的 ASCII 码从小到大排序（字典序）
        infoIds.sort((o1, o2) -> o1.getKey().compareToIgnoreCase(o2.getKey()));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> infoId : infoIds) {
            if (infoId.getKey().equals("sign")) {
                continue;
            }
            sb.append(infoId.getKey());
            sb.append("=");
            sb.append(infoId.getValue());
            sb.append("&");
        }
        return sb.substring(0, sb.length() - 1);
    }
}
