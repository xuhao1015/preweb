package com.xd.pre.modules.sys.jd;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.xd.pre.common.utils.PreUtils;
import com.xd.pre.common.utils.R;
import com.xd.pre.modules.sys.domain.DouyinSignData;
import com.xd.pre.modules.sys.domain.JdLog;
import com.xd.pre.modules.sys.domain.JdTenant;
import com.xd.pre.modules.sys.dto.PayFindStatusByOderIdVo;
import com.xd.pre.modules.sys.dto.UpdateCallBackUrlVo;
import com.xd.pre.modules.sys.dto.UpdatePasswordVo;
import com.xd.pre.modules.sys.jd.vo.req.CreateMchOrderReq;
import com.xd.pre.modules.sys.mapper.AreaIpMapper;
import com.xd.pre.modules.sys.mapper.DouyinSignDataMapper;
import com.xd.pre.modules.sys.mapper.JdLogMapper;
import com.xd.pre.modules.sys.vo.OrdrePayUrlDto;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/px")
@CrossOrigin
@Slf4j
public class JdTenantController {

    @Autowired
    private JdTenantService jdTenantService;


    @Resource
    private AreaIpMapper areaIpMapper;


    @Resource
    private JdLogMapper jdLogMapper;
    @Resource
    private DouyinSignDataMapper douyinSignDataMapper;


    @PostMapping("/login")
    public R login(@RequestBody JdTenant jdTenant) {
        R r = jdTenantService.login(jdTenant);
        return r;
    }

    @GetMapping("/test")
    public String login1() throws Exception {
        OkHttpClient client = new OkHttpClient();
        log.info("我爱北京天安门");
        log.error("我爱北京天安门");
        log.debug("我爱北京天安门");
        log.warn("我爱北京天安门");
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, "body=%257B%2522appId%2522%253A%2522jd_android_app4%2522%252C%2522payId%2522%253A%2522b8ee079293da4176bd8951e65039bf86%2522%257D&undefined=");
        Request request = new Request.Builder()
                .url("https://pay.m.jd.com/index.action?functionId=weixinPay&clientVersion=9.4.4&build=87076&client=android&d_brand=OPPO&d_model=PACT00&osVersion=10&screen=2200%2A1080&partner=lc031&oaid=&openudid=38dba8ea87c5a8ec&eid=eidAa29e812100s70W28drdeR8CHVNfyY4gIV9FtvfeMGb1PzIKW1UclvqAlNPO54C/OQDRSmiPmZf9T8Q5zNAMG9a8e3SFMDFradJ1Cof1wGJw7pkBm&sdkVersion=29&lang=zh_CN&uuid=38dba8ea87c5a8ec&aid=38dba8ea87c5a8ec&area=22_1930_49324_49399&networkType=wifi&wifiBssid=unknown&uts=zrHR4oLv7fO8bj08KaWkuJrGiAm%2FG6alpSm6Xi3w6q7mbMRNdfQnJPQCjmr9tJCaeWIodGATFEDuGdq0JpunSEbUYKUSicR67zcVj2Ih588lv5ucuMGLK397RuviO8opAi4Sz2rQntcwSalpdJHHH5FL7qUwQzKL&st=1652876578241&sign=be33e1ab58a87ca61902a77dcf636a35&sv=101")
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();
        Response response = client.newCall(request).execute();
        String string = response.body().string();
        response.close();
        return string;
    }

    @PostMapping("/updatePassword")
    public R login(@RequestBody UpdatePasswordVo updatePasswordVo) {
        Assert.isTrue(StrUtil.isNotBlank(updatePasswordVo.getUsername()), "账号不能为空");
        Assert.isTrue(StrUtil.isNotBlank(updatePasswordVo.getPassword()), "原始密码不能为空");
        Assert.isTrue(StrUtil.isNotBlank(updatePasswordVo.getToken()), "登录token不能为空");
        Assert.isTrue(StrUtil.isNotBlank(updatePasswordVo.getNewPassword()), "新密码不能为空");
        R r = jdTenantService.updatePassword(updatePasswordVo);
        return r;
    }

    @PostMapping("/updateCallBackUrl")
    public R updateCallBackUrlVo(@RequestBody UpdateCallBackUrlVo updateCallBackUrlVo) {
        Assert.isTrue(StrUtil.isNotBlank(updateCallBackUrlVo.getToken()), "登录token不能为空");
        Assert.isTrue(StrUtil.isNotBlank(updateCallBackUrlVo.getCallBackUrl()), "回调地址不能为空");
        R r = jdTenantService.updateCallBackUrl(updateCallBackUrlVo);
        return r;
    }

    @PostMapping("/createOrder")
    public R getOrder(@RequestBody @Valid CreateMchOrderReq orderCreateDto) throws Exception {
        R r = jdTenantService.createOrder(orderCreateDto);
        return r;
    }


    @GetMapping("/pay")
    public R pay(@RequestParam("orderId") String orderId, @RequestParam("sign") String sign, HttpServletRequest request) {
        OrdrePayUrlDto dto = jdTenantService.findByOrderId(orderId, sign, request);
        return R.ok(dto);
    }

    @GetMapping("/payWx")
    public R payWx(@RequestParam("orderId") String orderId, @RequestParam("sign") String sign, HttpServletRequest request) {
        OrdrePayUrlDto dto = jdTenantService.payWx(orderId, sign, request);
        return R.ok(dto);
    }

    @PostMapping("/payFindStatusByOderId")
    public R payFindStatusByOderId(@RequestBody PayFindStatusByOderIdVo payFindStatusByOderIdVo) {
        R r = jdTenantService.payFindStatusByOderId(payFindStatusByOderIdVo);
        return r;
    }


    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("browser_sign")
    public R browser_sign(HttpServletRequest request, String orderId, String sign, String browserSign, String dataSign) {
        if (!PreUtils.getSign(orderId).equals(sign)) {
            log.info("订单号串改数据:{}", orderId);
            return R.error();
        }
        String dataSignMy = String.format("orderId=%s&sign=%s&browserSign=%s", orderId, sign, browserSign);
        if (!PreUtils.getSign(dataSignMy).equals(dataSign)) {
            log.info("订单号串改数据:{},签证串改", orderId);
            return R.error();
        }
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent("订单签证记录:" + orderId, browserSign, 3, TimeUnit.HOURS);
        if (!aBoolean) {
            log.info("订单号当前已经存过了。丢去:{}", orderId);
            return R.ok();
        }
        String user_agent = request.getHeader("user-agent");
        String ip = PreUtils.getIPAddress(request);
        try {
            DouyinSignData build = DouyinSignData.builder().orderId(orderId).createTime(new Date()).userAgent(user_agent).browserSign(browserSign).ip(ip).build();
            douyinSignDataMapper.insert(build);
        } catch (Exception e) {
            log.info("订单号:{},报错了:{}", orderId, e.getMessage());
        }
        return R.ok();
    }

    @PostMapping("/log")
    public R log(HttpServletRequest request) {
        JdLog jdLog = new JdLog();
        String user_agent = request.getHeader("user-agent");
        jdLog.setUserAgent(user_agent);
        String type = request.getParameter("type");
        String orderId = request.getParameter("orderId");
        if (StrUtil.isNotBlank(type)) {
            jdLog.setType(Integer.valueOf(type));
        }
        jdLog.setOrderId(orderId);
        String ip = request.getHeader("X-Forwarded-For");
        jdLog.setIp(ip);
        jdLogMapper.insert(jdLog);
        return R.ok();
    }
}
