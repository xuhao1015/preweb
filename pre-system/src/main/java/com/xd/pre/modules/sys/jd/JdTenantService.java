package com.xd.pre.modules.sys.jd;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xd.pre.common.dto.IpDto;
import com.xd.pre.common.enumcomm.OrderStatusEnum;
import com.xd.pre.common.utils.IPUtil;
import com.xd.pre.common.utils.PreUtils;
import com.xd.pre.common.utils.R;
import com.xd.pre.common.utils.UrlEntity;
import com.xd.pre.modules.sys.domain.*;
import com.xd.pre.modules.sys.dto.PayFindStatusByOderIdVo;
import com.xd.pre.modules.sys.dto.UpdateCallBackUrlVo;
import com.xd.pre.modules.sys.dto.UpdatePasswordVo;
import com.xd.pre.modules.sys.jd.vo.req.CreateMchOrderReq;
import com.xd.pre.modules.sys.jd.vo.res.CreateOrderRes;
import com.xd.pre.modules.sys.jd.vo.res.SelectOrderReq;
import com.xd.pre.modules.sys.mapper.*;
import com.xd.pre.modules.sys.vo.OrdrePayUrlDto;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;
import javax.jms.Queue;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JdTenantService {
    @Resource
    private JdTenantMapper jdTenantMapper;
    @Resource
    JdMchOrdermapper jdMchOrdermapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;
    @Autowired
    private Queue queue;
    @Resource
    private JdOrderPtMapper jdOrderPtMapper;

/*    @Resource(name = "match2_queue")
    private Queue match2_queue;*/

    @Resource
    private AreaIpMapper areaIpMapper;

    @Autowired
    private LogService logService;

    @Resource
    private JdAppStoreConfigMapper jdAppStoreConfigMapper;


    public R login(JdTenant jdTenantRe) {
        Assert.isTrue(StrUtil.isNotBlank(jdTenantRe.getUsername()), "有用户名不能为空");
        Assert.isTrue(StrUtil.isNotBlank(jdTenantRe.getPassword()), "密码不能为空");
        JdTenant jdTenant = jdTenantMapper.selectOne(Wrappers.<JdTenant>lambdaQuery()
                .eq(JdTenant::getUsername, jdTenantRe.getUsername())
                .eq(JdTenant::getPassword, jdTenantRe.getPassword())
                .eq(JdTenant::getIsEnable, 1)
                .ge(JdTenant::getExpirationTime, new Date()));
        if (ObjectUtil.isNull(jdTenant)) {
            return R.error("账号密码不存在");
        }
        log.info("某人登录了msg:[data:{}]", jdTenant.getUsername());
        String userNameAndPassword = jdTenant.getPassword() + jdTenant.getUsername();
        String md5 = PreUtils.getSign(userNameAndPassword);
//        redisTemplate.opsForValue().set(String.format("tenant:%s", md5), JSON.toJSONString(jdTenant));
        redisTemplate.opsForValue().set(String.format("tenant:%s", jdTenant.getId() + ""), JSON.toJSONString(jdTenant));
        return R.ok(md5);
    }

    private String weixinUrl(String mweb_url) {
        try {
            if (ObjectUtil.isNull(mweb_url)) {
                return null;
            }
            OkHttpClient client = new OkHttpClient().newBuilder().build();
            Request.Builder header = new Request.Builder()
                    .url(mweb_url)
                    .get()
                    .addHeader("Referer", "https://pay.m.jd.com/");
            Request request = header.build();
            Response response = client.newCall(request).execute();
            String jingdonghtml = response.body().string();
            response.close();
            String P_COMM = "[a-zA-z]+://[^\\s]*";
            Pattern pattern = Pattern.compile(P_COMM);
            Matcher matcher = pattern.matcher(jingdonghtml);
            if (matcher.find()) {
                String group = matcher.group();
                String replace = group.replace("\"", "");
                return replace;
            }
        } catch (Exception e) {

        }
        return null;
    }

    public OrdrePayUrlDto payWx(String orderId, String sign, HttpServletRequest request) {
        String md5 = PreUtils.getSign(orderId);
        String time = redisTemplate.opsForValue().get("orderId:" + orderId);
        Assert.isTrue(sign.equals(md5), "请勿骚整");
        Assert.isTrue(StrUtil.isNotBlank(time), "支付时间过期了");
        JdMchOrder jdMchOrder = jdMchOrdermapper.selectOne(Wrappers.<JdMchOrder>lambdaQuery().eq(JdMchOrder::getTradeNo, orderId));
        String originalTradeNo = jdMchOrder.getOriginalTradeNo();
        String wexUrl = redisTemplate.opsForValue().get("订单管理微信链接:" + originalTradeNo);
        String weixinUrl = weixinUrl(wexUrl);
        if (StrUtil.isBlank(weixinUrl)) {
            return null;
        }
        JdOrderPt jdOrderPt = jdOrderPtMapper.selectById(jdMchOrder.getOriginalTradeId());
        DateTime dateTime = DateUtil.offsetMinute(jdMchOrder.getCreateTime(), 4);
        OrdrePayUrlDto dto = OrdrePayUrlDto.builder().orderId(jdMchOrder.getTradeNo()).realOrderId(jdOrderPt.getOrderId())
                .wxPayExpireTime(dateTime.getTime()).wxPayUrl(weixinUrl)
                .createTime(jdMchOrder.getCreateTime()).build();
        return dto;
    }


    public OrdrePayUrlDto findByOrderId(String orderId, String sign, HttpServletRequest request) {
        if (!orderId.equals("1")) {
            Assert.isTrue(StrUtil.isNotBlank(sign), "请勿骚整");
        }
        String md5 = PreUtils.getSign(orderId);
        if (!orderId.equals("1")) {
            Assert.isTrue(sign.equals(md5), "请勿骚整");
        }
        String time = redisTemplate.opsForValue().get("orderId:" + orderId);
        Assert.isTrue(StrUtil.isNotBlank(time), "支付时间过期了");
        JdMchOrder jdMchOrder = jdMchOrdermapper.selectOne(Wrappers.<JdMchOrder>lambdaQuery().eq(JdMchOrder::getTradeNo, orderId));
        Map<String, Object> headMap = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (headerName.toLowerCase().equals("user-agent")
                    || headerName.toLowerCase().equals("x-forwarded-for")
                    || headerName.toLowerCase().equals("x-real-port")
                    || headerName.toLowerCase().equals("x-real-ip")) {
                Enumeration<String> v = request.getHeaders(headerName);
                List<String> arr = new ArrayList<>();
                while (v.hasMoreElements()) {
                    arr.add(v.nextElement());
                }
                if (CollUtil.isNotEmpty(arr)) {
                    headMap.put(headerName, arr.get(0));
                }
            } else {
                continue;
            }
        }
        String ip = PreUtils.getIPAddress(request);
        log.info("用户代理真时ip,msg:{}", ip);
        Assert.isTrue(ObjectUtil.isNotNull(jdMchOrder), "订单不存在");
        String redisStr = redisTemplate.opsForValue().get("用户IP:" + jdMchOrder.getTradeNo());
        if (StrUtil.isBlank(redisStr)) {
            log.info("记录日志");
            logService.buildLog(request, orderId, 15);
            logService.buildLog(request, orderId, 15);
            jdMchOrder.setCreateTime(new Date());
            this.jdMchOrdermapper.updateById(jdMchOrder);
            log.info("订单号:{},通知订单系统。开始搞匹配msg:{}", jdMchOrder.getTradeNo(), jdMchOrder);
            redisTemplate.opsForValue().set("用户IP:" + jdMchOrder.getTradeNo(), JSON.toJSONString(headMap), 4L, TimeUnit.MINUTES);
//            buildIpData(jdMchOrder.getTradeNo(), ip);
            log.info("订单号:{},完成用户的ip获取,用户ip:{}", jdMchOrder.getTradeNo(), ip);
//            this.sendMessage(this.match2_queue, JSON.toJSONString(jdMchOrder));
            this.sendMessage(this.queue, JSON.toJSONString(jdMchOrder));
        }
        if (ObjectUtil.isNull(jdMchOrder.getOriginalTradeNo())) {
            OrdrePayUrlDto nodto = new OrdrePayUrlDto();
            return nodto;
        }
        JdOrderPt jdOrderPt = jdOrderPtMapper.selectById(jdMchOrder.getOriginalTradeId());
        String config = redisTemplate.opsForValue().get("配置文件:" + jdOrderPt.getSkuId());
        if (StrUtil.isBlank(config)) {
            List<JdAppStoreConfig> jdAppStoreConfigs = jdAppStoreConfigMapper.selectList(Wrappers.emptyWrapper());
            for (JdAppStoreConfig jdAppStoreConfig : jdAppStoreConfigs) {
                redisTemplate.opsForValue().set("配置文件:" + jdAppStoreConfig.getSkuId(), JSON.toJSONString(jdAppStoreConfig), 1, TimeUnit.DAYS);
            }
            config = redisTemplate.opsForValue().get("配置文件:" + jdOrderPt.getSkuId());
        }
        JdAppStoreConfig jdAppStoreConfig = JSON.parseObject(config, JdAppStoreConfig.class);

        DateTime dateTime = DateUtil.offsetMinute(jdMchOrder.getCreateTime(), 4);
        OrdrePayUrlDto dto = OrdrePayUrlDto.builder().orderId(jdMchOrder.getTradeNo()).realOrderId(jdOrderPt.getOrderId())
                .wxPayExpireTime(dateTime.getTime()).wxPayUrl(jdOrderPt.getWxPayUrl())
                .payType(jdAppStoreConfig.getPayType())
                .createTime(jdMchOrder.getCreateTime()).returnUrl(jdMchOrder.getReturnUrl()).build();
        //  .hrefUrl(jdOrderPt.getHrefUrl())
        //1,待支付。2.直接代理拉起，3直接拉起 10,扫码支付  return dto;
        dto.setHrefUrl(jdOrderPt.getHrefUrl());
        if (jdAppStoreConfig.getPayType() == 1) {
            dto.setHrefUrl(jdOrderPt.getHrefUrl());
            return dto;
        }
        if (jdAppStoreConfig.getPayType() == 2) {
            dto.setHrefUrl(jdOrderPt.getWxPayUrl());
            return dto;
        }
        if (jdAppStoreConfig.getPayType() == 3) {
            dto.setHrefUrl(jdOrderPt.getWeixinUrl());
            return dto;
        }
        return dto;
    }

    private void buildIpData(String tradeNo, String ip) {
        try {
            IpDto ipDto = IPUtil.getIpDto(ip);
            if (ObjectUtil.isNotNull(ipDto)) {
                log.info("先查询市区。如果市区不对查询省，如果省都没有。那就算求了");
                List<AreaIp> areaIps = areaIpMapper.selectList(Wrappers.<AreaIp>lambdaQuery().like(AreaIp::getCityName, ipDto.getCity()));
                if (CollUtil.isNotEmpty(areaIps)) {
                    log.info("市区查询出来了。选择第一个");
                    AreaIp areaIp = areaIps.get(0);
                    redisTemplate.opsForValue().set("用户IP对应数据库的IP:" + tradeNo, JSON.toJSONString(areaIp), 60, TimeUnit.MINUTES);
                } else {
                    log.info("市区没有查询出来。代表不能查询市区只能用省匹配");
                    areaIps = areaIpMapper.selectList(Wrappers.<AreaIp>lambdaQuery().like(AreaIp::getProvinceName, ipDto.getProvince()));
                    if (CollUtil.isNotEmpty(areaIps)) {
                        log.debug("处理省会城市");
                        List<AreaIp> minToMax = areaIps.stream().sorted(Comparator.comparing(AreaIp::getCityId, Comparator.comparingInt(Integer::parseInt)))
                                .collect(Collectors.toList());
                        AreaIp areaIp = minToMax.get(0);
                        redisTemplate.opsForValue().set("用户IP对应数据库的IP:" + tradeNo, JSON.toJSONString(areaIp), 60, TimeUnit.MINUTES);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取ip地址失败");
        }
    }


    public R createOrder(CreateMchOrderReq reqVo) throws Exception {
        log.info("当前创建订单参数:{}", JSON.toJSONString(reqVo));
        JSONObject paramDataMap = JSON.parseObject(JSON.toJSONString(reqVo));
        R r = checkSign(paramDataMap);
        if (r.getCode() != 0) {
            return r;
        }
        JdTenant jdTenant = JSON.parseObject(redisTemplate.opsForValue().get("tenant:" + paramDataMap.getString("mch_id")), JdTenant.class);
        String localIP = redisTemplate.opsForValue().get("服务器地址");
        String tradeNo = PreUtils.productOrderId();
        String url = jdTenant.getUrlPre() + tradeNo;
        String sign = PreUtils.getSign(tradeNo);
        String payUrl = url + "&sign=" + sign;
        DateTime dateTime = DateUtil.offsetMinute(new Date(), 4);
        String expired_time = DateUtil.formatDateTime(dateTime);
        String skuId = jdTenantMapper.selectSkuIdDouYin(reqVo.getAmount(), Integer.valueOf(reqVo.getPass_code()));
        JdAppStoreConfig jdAppStoreConfig = jdAppStoreConfigMapper.selectOne(Wrappers.<JdAppStoreConfig>lambdaQuery().eq(JdAppStoreConfig::getSkuId, skuId));
        if (jdAppStoreConfig.getPayType() == 10) {
            UrlEntity urlEntity = PreUtils.parseUrl(payUrl);
            String orderId = urlEntity.getParams().get("orderId");
            payUrl = "http://" + localIP + "/qr.html?orderId=" + orderId + "&sign=" + sign;
        }
        if (jdAppStoreConfig.getPayType() == 20) {
            UrlEntity urlEntity = PreUtils.parseUrl(payUrl);
            String orderId = urlEntity.getParams().get("orderId");
            payUrl = "http://" + localIP + "/jd.html?orderId=" + orderId + "&sign=" + sign;
        }
        if (jdAppStoreConfig.getPayType() == 30) {
            UrlEntity urlEntity = PreUtils.parseUrl(payUrl);
            String orderId = urlEntity.getParams().get("orderId");
            payUrl = "http://" + localIP + "/zhifubao.html?orderId=" + orderId + "&sign=" + sign;
        }
        JdMchOrder build = JdMchOrder.builder().mchId(reqVo.getMch_id()).passCode(reqVo.getPass_code())
                .subject(reqVo.getSubject()).body(reqVo.getBody())
                .outTradeNo(reqVo.getOut_trade_no()).tradeNo(tradeNo)
                .amount(reqVo.getAmount()).money(reqVo.getAmount())
                .payUrl(payUrl)
                .clientIp(reqVo.getClient_ip())
                .notifyUrl(reqVo.getNotify_url())
                .returnUrl(reqVo.getReturn_url())
                .sign(reqVo.getSign())
                .notifySucc(OrderStatusEnum.未通知)
                .status(OrderStatusEnum.待支付)
                .timestamp(reqVo.getTimestamp())
                .expiredTime(expired_time)
                .createTime(new Date())
                .skuId(skuId)
                .tenantId(jdTenant.getTenantId())
                .build();
        CreateOrderRes createOrderRes = CreateOrderRes.builder().mch_id(reqVo.getMch_id()).trade_no(tradeNo)
                .out_trade_no(reqVo.getOut_trade_no()).money(reqVo.getAmount()).pay_url(payUrl).expired_time(expired_time).build();
        log.info("当前生产的订单号为msg:[JdMchOrder：{}]", build);
        jdMchOrdermapper.insert(build);
        redisTemplate.opsForValue().set("orderId:" + build.getTradeNo(), "4", 5L, TimeUnit.MINUTES);
        log.info("订单号{},创建订单成功,回调地址msg:{}", build.getTradeNo(), build.getNotifyUrl());
        return R.ok(createOrderRes);
    }

    private boolean checkSign(String reqSign, JSONObject paramDataMap, JdTenant jdTenant) {
        String userNameAndPassword = jdTenant.getPassword() + jdTenant.getUsername();
        String secret = PreUtils.getSign(userNameAndPassword);
        String asciiSort = PreUtils.getAsciiSort(paramDataMap);
        String signMy = asciiSort + "&sign=" + secret;
        String encode = Base64.encode(signMy);
        String signChcek = PreUtils.getSign(encode);
        if (signChcek.equals(reqSign)) {
            return true;
        }
        return false;
    }

    // 发送消息，destination是发送到的队列，message是待发送的消息
    private void sendMessage(Destination destination, final String message) {
        jmsMessagingTemplate.convertAndSend(destination, message);
    }

    public R updatePassword(UpdatePasswordVo updatePasswordVo) {
        String s = redisTemplate.opsForValue().get(String.format("tenant:%s", updatePasswordVo.getToken()));
        Assert.isTrue(ObjectUtil.isNotNull(s), "请登录");
        //启用。不过期的用户
        JdTenant jdTenant = jdTenantMapper.selectOne(Wrappers.<JdTenant>lambdaQuery()
                .eq(JdTenant::getUsername, updatePasswordVo.getUsername())
                .eq(JdTenant::getPassword, updatePasswordVo.getPassword())
                .eq(JdTenant::getIsEnable, 1)
                .ge(JdTenant::getExpirationTime, new Date()));
        Assert.isTrue(ObjectUtil.isNotNull(jdTenant), "账号密码不存在，或者已经过期。请联系管理员");
        jdTenant.setPassword(updatePasswordVo.getNewPassword());
        jdTenantMapper.updateById(jdTenant);
        redisTemplate.delete(String.format("tenant:%s", updatePasswordVo.getToken()));
        return R.ok("修改密码成功");
    }

    public R updateCallBackUrl(UpdateCallBackUrlVo updateCallBackUrlVo) {
        String s = redisTemplate.opsForValue().get(String.format("tenant:%s", updateCallBackUrlVo.getToken()));
        Assert.isTrue(ObjectUtil.isNotNull(s), "请登录");
        JdTenant jdTenant = JSON.parseObject(s, JdTenant.class);
        log.info("同步数据库信息");
        JdTenant jdTenantDb = jdTenantMapper.selectById(jdTenant.getId());
        jdTenantDb.setCallBackUrl(updateCallBackUrlVo.getCallBackUrl());
        jdTenantMapper.updateById(jdTenantDb);
        return R.ok("修改回调地址成功");
    }

    private R checkSign(JSONObject paramDataMap) {
        String tenantStr = redisTemplate.opsForValue().get("tenant:" + paramDataMap.getString("mch_id"));
        if (StrUtil.isBlank(tenantStr)) {
            return R.error("当前商户数据不存在，请校验后在重试");
        }
        JdTenant jdTenant = JSON.parseObject(tenantStr, JdTenant.class);
        if (StrUtil.isBlank(tenantStr)) {
            return R.error("当前商户不正确，请校验后在重试");
        }
        boolean b = checkSign(paramDataMap.getString("sign"), paramDataMap, jdTenant);
        if (!b) {
            log.info("创建订单参数不正确签名不正确");
            return R.error("签名不正确，请重新签名");
        } else {
            return R.ok();
        }

    }

    public R payFindStatusByOderId(PayFindStatusByOderIdVo payFindStatusByOderIdVo) {
        JSONObject paramDataMap = JSON.parseObject(JSON.toJSONString(payFindStatusByOderIdVo));
        R r = checkSign(paramDataMap);
        if (r.getCode() != 0) {
            return r;
        }
        JdMchOrder jdMchOrder = jdMchOrdermapper.selectOne(Wrappers.<JdMchOrder>lambdaQuery().
                eq(JdMchOrder::getOutTradeNo, payFindStatusByOderIdVo.getOut_trade_no()));
        Assert.isTrue(ObjectUtil.isNotNull(jdMchOrder), "订单不存在");
        SelectOrderReq selectOrderReq =
                SelectOrderReq.builder().pass_code(jdMchOrder.getPassCode()).money(jdMchOrder.getMoney()).trade_no(jdMchOrder.getTradeNo())
                        .out_trade_no(jdMchOrder.getOutTradeNo()).original_trade_no(jdMchOrder.getOriginalTradeNo())
                        .status(jdMchOrder.getStatus()).notify_succ(jdMchOrder.getNotifySucc()).notify_url(jdMchOrder.getNotifyUrl())
                        .pay_url(jdMchOrder.getPayUrl()).subject(jdMchOrder.getSubject()).body(jdMchOrder.getBody()).build();
        log.info("订单号{}，商户查询了订单状态msg:{}", jdMchOrder.getTradeNo(), jdMchOrder.getStatus());
        return R.ok(selectOrderReq);
    }

    @Async()
    public void updateClickTime(String orderId) {
        try {
            String clickDate = DateUtil.formatDateTime(new Date());
            log.info("订单号:{} 添加点击时间支付数据时间。后续可以用于统计和加黑", orderId);
            jdMchOrdermapper.updateClickDataTime(orderId, clickDate);
        } catch (Exception e) {
            log.info("订单号:{}修改订单时间报错:{}", orderId, e.getMessage());
        }
    }


    public static void main(String[] args) {
        创建订单();
//        查询订单();
    }

    private static void 查询订单() {
        String a = "{\t\n" +
                "\t\"mch_id\":\"1\",\n" +
                "\t\"out_trade_no\":\"750\",\n" +
                "\t\"sign\":\"04e68dccc9b4e011b0ccd2ab23733542\"\n" +
                "}";
        JSONObject parseObject = JSON.parseObject(a);
        String asciiSort = PreUtils.getAsciiSort(parseObject);
        String s = asciiSort + "&sign=" + "04e68dccc9b4e011b0ccd2ab23733542";
        String encode = Base64.encode(s);
        String sign = PreUtils.getSign(encode);
        cn.hutool.json.JSONObject hutoolsJson = new cn.hutool.json.JSONObject(a);
        hutoolsJson.put("sign", sign);
        System.out.println(JSON.toJSONString(hutoolsJson));
        HttpResponse execute = HttpRequest.post("http://103.235.174.176/api/px/payFindStatusByOderId").body(hutoolsJson).execute();
        String body = execute.body();
        System.out.println(body);
    }

    private static void 创建订单() {
        String a = "{\n" +
                "\t\"mch_id\": \"1\",\n" +
                "\t\"subject\": \"支付1000元\",\n" +
                "\t\"body\": \"支付1000元\",\n" +
                "\t\"out_trade_no\": \"758\",\n" +
                "\t\"amount\": \"100.00\",\n" +
                "\t\"notify_url\": \"http://103.235.174.176/pre/jd/callbackTemp\",\n" +
                "\t\"timestamp\": \"2014-07-24 03:07:50\",\n" +
                "\t\"sign\": \"%s\",\n" +
                "\t\"client_ip\":\"192.168.2.1\",\n" +
                "\t\"pass_code\":\"8\"\n" +
                "}";
        JSONObject parseObject = JSON.parseObject(a);
        String asciiSort = PreUtils.getAsciiSort(parseObject);
        String s = asciiSort + "&sign=" + "04e68dccc9b4e011b0ccd2ab23733542";
        String encode = Base64.encode(s);
        String sign = PreUtils.getSign(encode);
        cn.hutool.json.JSONObject hutoolsJson = new cn.hutool.json.JSONObject(a);
        hutoolsJson.put("sign", sign);
        System.out.println(JSON.toJSONString(hutoolsJson));
    }


}
