package com.xd.pre.modules.sys.jd;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.xd.pre.common.utils.PreUtils;
import com.xd.pre.modules.sys.domain.JdLog;
import com.xd.pre.modules.sys.domain.JdMchOrder;
import com.xd.pre.modules.sys.domain.JdOrderPt;
import com.xd.pre.modules.sys.mapper.JdLogMapper;
import com.xd.pre.modules.sys.mapper.JdMchOrdermapper;
import com.xd.pre.modules.sys.mapper.JdOrderPtMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Service
@Slf4j
public class LogService {

    @Resource
    private JdLogMapper jdLogMapper;

    @Resource
    private JdMchOrdermapper jdMchOrdermapper;

    @Resource
    private JdOrderPtMapper jdOrderPtMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;


    public void buildLog(HttpServletRequest request, String orderId, Integer step) {
        try {
            JdLog jdLog = new JdLog();
            jdLog.setOrderId(orderId);
            String user_agent = request.getHeader("user-agent");
            jdLog.setType(step);
            jdLog.setUserAgent(user_agent);
            String ip = request.getHeader("X-Forwarded-For");
            jdLog.setIp(ip);
            jdLogMapper.insert(jdLog);
            log.info("日志信息msg:{}", jdLog);
        } catch (Exception e) {
            log.error("第二种方式请求日志报错msg:{}", e.getMessage());
        }
    }

    public static void main(String[] args) {
        String sign = PreUtils.getSign("202204141855553580");
        System.out.println(sign);
    }

    public JdOrderPt getJdOrderPt(HttpServletRequest servletRequest) {
        String orderId = servletRequest.getParameter("orderId");
        String md5 = PreUtils.getSign(orderId);
        String sign = servletRequest.getParameter("sign");
        Assert.isTrue(md5.equals(sign), "请勿骚整");
        log.info("根据最新匹配信息。获取到订单京东的订单Id ");
        String jdMchStr = redisTemplate.opsForValue().get("匹配锁定成功:" + orderId);
        Assert.isTrue(StrUtil.isNotBlank(jdMchStr), "过期支付");

        JdMchOrder jdMchOrder = jdMchOrdermapper.selectById(JSON.parseObject(jdMchStr, JdMchOrder.class).getId());
        JdOrderPt jdOrderPt = jdOrderPtMapper.selectById(jdMchOrder.getOriginalTradeId());
        return jdOrderPt;
    }

    public void buildOutHeader(HttpServletRequest request, HttpRequest proxyRequest, String referer) {
        Enumeration<String> headerNames = request.getHeaderNames();
        // 设置请求头
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> v = request.getHeaders(headerName);
            List<String> arr = new ArrayList<>();
            while (v.hasMoreElements()) {
                arr.add(v.nextElement());
            }
            if (CollUtil.isNotEmpty(arr)) {
                proxyRequest.setHeader(headerName, arr.get(0));
            }
        }
        String ip = PreUtils.getIPAddress(request);
        log.info("用户代理真时ip,msg:{}", ip);
        Header[] headers = proxyRequest.getHeaders("X-Forwarded-For");
      /*  if (headers != null && headers.length > 0) {
            proxyRequest.setHeader("X-Forwarded-For", PreUtils.getRandomIp());
        }*/
//        proxyRequest.removeHeaders("X-Forwarded-For");
        proxyRequest.removeHeaders("cookie");
        proxyRequest.removeHeaders("Host");
        proxyRequest.removeHeaders("Referer");
        proxyRequest.removeHeaders("Origin");
        proxyRequest.removeHeaders("X-Real-IP");
        proxyRequest.removeHeaders("content-length");
        proxyRequest.setHeader("referer", referer);
        proxyRequest.setHeader("origin", "https://pay.m.jd.com");

    }

}
