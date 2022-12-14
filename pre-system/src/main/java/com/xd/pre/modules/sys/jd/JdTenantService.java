package com.xd.pre.modules.sys.jd;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
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
        Assert.isTrue(StrUtil.isNotBlank(jdTenantRe.getUsername()), "????????????????????????");
        Assert.isTrue(StrUtil.isNotBlank(jdTenantRe.getPassword()), "??????????????????");
        JdTenant jdTenant = jdTenantMapper.selectOne(Wrappers.<JdTenant>lambdaQuery()
                .eq(JdTenant::getUsername, jdTenantRe.getUsername())
                .eq(JdTenant::getPassword, jdTenantRe.getPassword())
                .eq(JdTenant::getIsEnable, 1)
                .ge(JdTenant::getExpirationTime, new Date()));
        if (ObjectUtil.isNull(jdTenant)) {
            return R.error("?????????????????????");
        }
        log.info("???????????????msg:[data:{}]", jdTenant.getUsername());
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
        Assert.isTrue(sign.equals(md5), "????????????");
        Assert.isTrue(StrUtil.isNotBlank(time), "?????????????????????");
        JdMchOrder jdMchOrder = jdMchOrdermapper.selectOne(Wrappers.<JdMchOrder>lambdaQuery().eq(JdMchOrder::getTradeNo, orderId));
        String originalTradeNo = jdMchOrder.getOriginalTradeNo();
        String wexUrl = redisTemplate.opsForValue().get("????????????????????????:" + originalTradeNo);
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
        TimeInterval timer = DateUtil.timer();
        Assert.isTrue(StrUtil.isNotBlank(sign), "????????????");
        String md5 = PreUtils.getSign(orderId);
        Assert.isTrue(sign.equals(md5), "????????????");
        String timeData = redisTemplate.opsForValue().get("orderId:" + orderId);
        Assert.isTrue(StrUtil.isNotBlank(timeData), "?????????????????????");
        JdMchOrder jdMchOrder = null;
        try {
            jdMchOrder = JSON.parseObject(timeData, JdMchOrder.class);
            log.info("?????????{}????????????????????????", jdMchOrder.getTradeNo());
        } catch (Exception e) {
            log.info("?????????{},??????????????????:{}", orderId, e.getMessage());
        }
        if (ObjectUtil.isNull(jdMchOrder)) {
            jdMchOrder = jdMchOrdermapper.selectOne(Wrappers.<JdMchOrder>lambdaQuery().eq(JdMchOrder::getTradeNo, orderId));
        }
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
        log.info("??????????????????ip,msg:{}", ip);
        Assert.isTrue(ObjectUtil.isNotNull(jdMchOrder), "???????????????");
        String redisStr = redisTemplate.opsForValue().get("??????IP:" + jdMchOrder.getTradeNo());
        if (StrUtil.isBlank(redisStr)) {
            log.info("?????????:{}????????????,??????:{}", jdMchOrder.getTradeNo(), timer.interval());
            jdMchOrder = jdMchOrdermapper.selectOne(Wrappers.<JdMchOrder>lambdaQuery().eq(JdMchOrder::getTradeNo, orderId));
            logService.buildLog(request, orderId, 15);
            logService.buildLog(request, orderId, 15);
            jdMchOrder.setCreateTime(new Date());
            this.jdMchOrdermapper.updateById(jdMchOrder);
            log.info("?????????:{},????????????????????????????????????msg:{}", jdMchOrder.getTradeNo(), jdMchOrder);
            redisTemplate.opsForValue().set("??????IP:" + jdMchOrder.getTradeNo(), JSON.toJSONString(headMap), 4L, TimeUnit.MINUTES);
//            buildIpData(jdMchOrder.getTradeNo(), ip);
            log.info("?????????:{},???????????????ip??????,??????ip:{}", jdMchOrder.getTradeNo(), ip);
//            this.sendMessage(this.match2_queue, JSON.toJSONString(jdMchOrder));
            this.sendMessage(this.queue, JSON.toJSONString(jdMchOrder));
        }
        String lockDataMatch = redisTemplate.opsForValue().get("??????????????????:" + orderId);
        if (StrUtil.isBlank(lockDataMatch)) {
            log.info("?????????{}????????????????????????:{}", jdMchOrder.getTradeNo(), timer.interval());
            OrdrePayUrlDto nodto = new OrdrePayUrlDto();
            return nodto;
        } else {
            log.info("?????????:{}??????????????????,??????:{}", jdMchOrder.getTradeNo(), timer.interval());
            jdMchOrder = jdMchOrdermapper.selectOne(Wrappers.<JdMchOrder>lambdaQuery().eq(JdMchOrder::getTradeNo, orderId));
        }
        if (ObjectUtil.isNull(jdMchOrder.getOriginalTradeNo())) {
            OrdrePayUrlDto nodto = new OrdrePayUrlDto();
            return nodto;
        }
        JdOrderPt jdOrderPt = jdOrderPtMapper.selectById(jdMchOrder.getOriginalTradeId());
        String config = redisTemplate.opsForValue().get("????????????:" + jdOrderPt.getSkuId());
        if (StrUtil.isBlank(config)) {
            List<JdAppStoreConfig> jdAppStoreConfigs = jdAppStoreConfigMapper.selectList(Wrappers.emptyWrapper());
            for (JdAppStoreConfig jdAppStoreConfig : jdAppStoreConfigs) {
                redisTemplate.opsForValue().set("????????????:" + jdAppStoreConfig.getSkuId(), JSON.toJSONString(jdAppStoreConfig), 1, TimeUnit.DAYS);
            }
            config = redisTemplate.opsForValue().get("????????????:" + jdOrderPt.getSkuId());
        }
        JdAppStoreConfig jdAppStoreConfig = JSON.parseObject(config, JdAppStoreConfig.class);
        DateTime dateTime = DateUtil.offsetMinute(jdMchOrder.getCreateTime(), 4);
        OrdrePayUrlDto dto = OrdrePayUrlDto.builder().orderId(jdMchOrder.getTradeNo()).realOrderId(jdOrderPt.getOrderId())
                .wxPayExpireTime(dateTime.getTime()).wxPayUrl(jdOrderPt.getWxPayUrl())
                .payType(jdAppStoreConfig.getPayType())
                .createTime(jdMchOrder.getCreateTime()).returnUrl(jdMchOrder.getReturnUrl()).build();
        //  .hrefUrl(jdOrderPt.getHrefUrl())
        //1,????????????2.?????????????????????3???????????? 10,????????????  return dto;
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
                log.info("????????????????????????????????????????????????????????????????????????????????????");
                List<AreaIp> areaIps = areaIpMapper.selectList(Wrappers.<AreaIp>lambdaQuery().like(AreaIp::getCityName, ipDto.getCity()));
                if (CollUtil.isNotEmpty(areaIps)) {
                    log.info("???????????????????????????????????????");
                    AreaIp areaIp = areaIps.get(0);
                    redisTemplate.opsForValue().set("??????IP??????????????????IP:" + tradeNo, JSON.toJSONString(areaIp), 60, TimeUnit.MINUTES);
                } else {
                    log.info("?????????????????????????????????????????????????????????????????????");
                    areaIps = areaIpMapper.selectList(Wrappers.<AreaIp>lambdaQuery().like(AreaIp::getProvinceName, ipDto.getProvince()));
                    if (CollUtil.isNotEmpty(areaIps)) {
                        log.debug("??????????????????");
                        List<AreaIp> minToMax = areaIps.stream().sorted(Comparator.comparing(AreaIp::getCityId, Comparator.comparingInt(Integer::parseInt)))
                                .collect(Collectors.toList());
                        AreaIp areaIp = minToMax.get(0);
                        redisTemplate.opsForValue().set("??????IP??????????????????IP:" + tradeNo, JSON.toJSONString(areaIp), 60, TimeUnit.MINUTES);
                    }
                }
            }
        } catch (Exception e) {
            log.error("??????ip????????????");
        }
    }


    public R createOrder(CreateMchOrderReq reqVo) {
        log.info("????????????????????????:{}", JSON.toJSONString(reqVo));
        JSONObject paramDataMap = JSON.parseObject(JSON.toJSONString(reqVo));
        R r = checkSign(paramDataMap);
        if (r.getCode() != 0) {
            return r;
        }
        JdTenant jdTenant = JSON.parseObject(redisTemplate.opsForValue().get("tenant:" + paramDataMap.getString("mch_id")), JdTenant.class);
        String localIP = redisTemplate.opsForValue().get("???????????????");
        String tradeNo = PreUtils.productOrderId();
        String url = jdTenant.getUrlPre() + tradeNo;
        String sign = PreUtils.getSign(tradeNo);
        String payUrl = url + "&sign=" + sign;
        DateTime dateTime = DateUtil.offsetMinute(new Date(), 4);
        String expired_time = DateUtil.formatDateTime(dateTime);
        JdAppStoreConfig jdAppStoreConfig = getJdAppStoreConfig(reqVo);
        if (ObjectUtil.isNull(jdAppStoreConfig)) {
            return R.error("?????????????????????");
        }
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
                .notifySucc(OrderStatusEnum.?????????)
                .status(OrderStatusEnum.?????????)
                .timestamp(reqVo.getTimestamp())
                .expiredTime(expired_time)
                .createTime(new Date())
                .skuId(jdAppStoreConfig.getSkuId())
                .tenantId(jdTenant.getTenantId())
                .build();
        CreateOrderRes createOrderRes = CreateOrderRes.builder().mch_id(reqVo.getMch_id()).trade_no(tradeNo)
                .out_trade_no(reqVo.getOut_trade_no()).money(reqVo.getAmount()).pay_url(payUrl).expired_time(expired_time).build();
        log.info("???????????????????????????msg:[JdMchOrder???{}]", build);
        jdMchOrdermapper.insert(build);
        redisTemplate.opsForValue().set("orderId:" + build.getTradeNo(), JSON.toJSONString(build), 5L, TimeUnit.MINUTES);
        log.info("?????????{},??????????????????,????????????msg:{}", build.getTradeNo(), build.getNotifyUrl());
        return R.ok(createOrderRes);
    }

    private JdAppStoreConfig getJdAppStoreConfig(CreateMchOrderReq reqVo) {
        String redisDataKey = String.format("????????????:%s_%s", reqVo.getPass_code(), reqVo.getAmount());
        String redisData = redisTemplate.opsForValue().get(redisDataKey);
        if (StrUtil.isBlank(redisData)) {
            String skuId = jdTenantMapper.selectSkuIdDouYin(reqVo.getAmount(), Integer.valueOf(reqVo.getPass_code()));
            if (StrUtil.isBlank(skuId)) {
                log.info("?????????????????????");
                return null;
            }
            JdAppStoreConfig jdAppStoreConfig = jdAppStoreConfigMapper.selectOne(Wrappers.<JdAppStoreConfig>lambdaQuery().eq(JdAppStoreConfig::getSkuId, skuId));
            redisTemplate.opsForValue().set(redisDataKey, JSON.toJSONString(jdAppStoreConfig), 1, TimeUnit.HOURS);
            return jdAppStoreConfig;
        } else {
            JdAppStoreConfig jdAppStoreConfig = JSON.parseObject(redisData, JdAppStoreConfig.class);
            return jdAppStoreConfig;
        }
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

    // ???????????????destination????????????????????????message?????????????????????
    private void sendMessage(Destination destination, final String message) {
        jmsMessagingTemplate.convertAndSend(destination, message);
    }

    public R updatePassword(UpdatePasswordVo updatePasswordVo) {
        String s = redisTemplate.opsForValue().get(String.format("tenant:%s", updatePasswordVo.getToken()));
        Assert.isTrue(ObjectUtil.isNotNull(s), "?????????");
        //???????????????????????????
        JdTenant jdTenant = jdTenantMapper.selectOne(Wrappers.<JdTenant>lambdaQuery()
                .eq(JdTenant::getUsername, updatePasswordVo.getUsername())
                .eq(JdTenant::getPassword, updatePasswordVo.getPassword())
                .eq(JdTenant::getIsEnable, 1)
                .ge(JdTenant::getExpirationTime, new Date()));
        Assert.isTrue(ObjectUtil.isNotNull(jdTenant), "???????????????????????????????????????????????????????????????");
        jdTenant.setPassword(updatePasswordVo.getNewPassword());
        jdTenantMapper.updateById(jdTenant);
        redisTemplate.delete(String.format("tenant:%s", updatePasswordVo.getToken()));
        return R.ok("??????????????????");
    }

    public R updateCallBackUrl(UpdateCallBackUrlVo updateCallBackUrlVo) {
        String s = redisTemplate.opsForValue().get(String.format("tenant:%s", updateCallBackUrlVo.getToken()));
        Assert.isTrue(ObjectUtil.isNotNull(s), "?????????");
        JdTenant jdTenant = JSON.parseObject(s, JdTenant.class);
        log.info("?????????????????????");
        JdTenant jdTenantDb = jdTenantMapper.selectById(jdTenant.getId());
        jdTenantDb.setCallBackUrl(updateCallBackUrlVo.getCallBackUrl());
        jdTenantMapper.updateById(jdTenantDb);
        return R.ok("????????????????????????");
    }

    private R checkSign(JSONObject paramDataMap) {
        String tenantStr = redisTemplate.opsForValue().get("tenant:" + paramDataMap.getString("mch_id"));
        if (StrUtil.isBlank(tenantStr)) {
            return R.error("???????????????????????????????????????????????????");
        }
        JdTenant jdTenant = JSON.parseObject(tenantStr, JdTenant.class);
        if (StrUtil.isBlank(tenantStr)) {
            return R.error("?????????????????????????????????????????????");
        }
        boolean b = checkSign(paramDataMap.getString("sign"), paramDataMap, jdTenant);
        if (!b) {
            log.info("??????????????????????????????????????????");
            return R.error("?????????????????????????????????");
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
        Assert.isTrue(ObjectUtil.isNotNull(jdMchOrder), "???????????????");
        SelectOrderReq selectOrderReq =
                SelectOrderReq.builder().pass_code(jdMchOrder.getPassCode()).money(jdMchOrder.getMoney()).trade_no(jdMchOrder.getTradeNo())
                        .out_trade_no(jdMchOrder.getOutTradeNo()).original_trade_no(jdMchOrder.getOriginalTradeNo())
                        .status(jdMchOrder.getStatus()).notify_succ(jdMchOrder.getNotifySucc()).notify_url(jdMchOrder.getNotifyUrl())
                        .pay_url(jdMchOrder.getPayUrl()).subject(jdMchOrder.getSubject()).body(jdMchOrder.getBody()).build();
        log.info("?????????{}??????????????????????????????msg:{}", jdMchOrder.getTradeNo(), jdMchOrder.getStatus());
        return R.ok(selectOrderReq);
    }

    @Async()
    public void updateClickTime(String orderId) {
        try {
            String clickDate = DateUtil.formatDateTime(new Date());
            log.info("?????????:{} ????????????????????????????????????????????????????????????????????????", orderId);
            jdMchOrdermapper.updateClickDataTime(orderId, clickDate);
        } catch (Exception e) {
            log.info("?????????:{}????????????????????????:{}", orderId, e.getMessage());
        }
    }


    public static void main(String[] args) {
        ????????????();
//        ????????????();
    }

    private static void ????????????() {
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
        HttpResponse execute = HttpRequest.post("http://103.235.174.139/api/px/payFindStatusByOderId").body(hutoolsJson).execute();
        String body = execute.body();
        System.out.println(body);
    }

    private static void ????????????() {
        String a = "{\n" +
                "\t\"mch_id\": \"1\",\n" +
                "\t\"subject\": \"??????1000???\",\n" +
                "\t\"body\": \"??????1000???\",\n" +
                "\t\"out_trade_no\": \"758\",\n" +
                "\t\"amount\": \"100.00\",\n" +
                "\t\"notify_url\": \"http://103.235.174.139/pre/jd/callbackTemp\",\n" +
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
