package com.xd.pre.modules.sys.jd;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/alipay")
@Slf4j
public class PayHtmlController {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdTenantService jdTenantService;


    @GetMapping("/payHtml")
    public String payH(@RequestParam("orderId") String orderId) {
        log.info("订单号{},此消息非常重要+++++++++++++++++++++++++++++++++++点击了支付msg:", orderId);
        //  redisTemplate.opsForValue().set("阿里支付数据:" + jdMchOrder.getTradeNo(), payUrl, 5, TimeUnit.MINUTES);
        String param = redisTemplate.opsForValue().get("阿里支付数据:" + orderId.trim());
        if (StrUtil.isBlank(param)) {
            log.info("订单号:{},或者支付错支付过期还在访问", orderId);
            return "支付时间已经过期,请重新支付";
        }
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("是否查询阿里支付数据:" + orderId.trim(), orderId, 60, TimeUnit.HOURS);
        if (ifAbsent) {
            jdTenantService.updateClickTime(orderId);
            redisTemplate.opsForValue().setIfAbsent("是否查询阿里支付数据:" + orderId.trim(), orderId, 60, TimeUnit.HOURS);
            redisTemplate.opsForValue().setIfAbsent("是否查询阿里支付数据:" + orderId.trim(), orderId, 60, TimeUnit.HOURS);
            redisTemplate.opsForValue().setIfAbsent("是否查询阿里支付数据:" + orderId.trim(), orderId, 60, TimeUnit.HOURS);
            log.info("订单号{},设置阿里云查询数据成功", orderId);
        }
        String payHtml = String.format("<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>支付页</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <script>\n" +
                "        var param = \"%s\";\n" +
                "            document.addEventListener(\"AlipayJSBridgeReady\", function(){\n" +
                "            AlipayJSBridge.call('tradePay', {\n" +
                "                orderStr: param\n" +
                "            })\n" +
                "        }, false);\n" +
                "    </script>\n" +
                "\n" +
                "</body>\n" +
                "</html>", param);
        log.info("订单号:{}返回完成支付数据+++++++++++", orderId);
        return payHtml;
    }

}
