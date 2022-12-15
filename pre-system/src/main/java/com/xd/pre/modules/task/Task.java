package com.xd.pre.modules.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xd.pre.common.constant.PreConstant;
import com.xd.pre.modules.sys.domain.*;
import com.xd.pre.modules.sys.mapper.*;
import com.xd.pre.modules.sys.util.BuildDouYinUrlUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class Task {


    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private JdMchOrdermapper jdMchOrderMapper;
    @Resource
    private JdOrderPtMapper jdOrderPtMapper;
    @Resource
    private JdLogMapper jdLogMapper;

    @Resource
    private DouyinAppCkMapper douyinAppCkMapper;

    @Resource
    private DouyinMethodNameParamMapper douyinMethodNameParamMapper;


    @Scheduled(cron = "0/20 * * * * ?")
    public void budan() {
        List<JdMchOrder> jdMchOrders = jdMchOrderMapper.selectBuDan();
        if (CollUtil.isEmpty(jdMchOrders)) {
            return;
        }
        findOrderStatusByOutOrder(jdMchOrders);
    }

    public void findOrderStatusByOutOrder(List<JdMchOrder> jdMchOrders) {
        for (JdMchOrder jdMchOrder : jdMchOrders) {
            try {
                Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("补单执行:" + jdMchOrder.getTradeNo(), JSON.toJSONString(jdMchOrder), 40, TimeUnit.SECONDS);
                if (!ifAbsent) {
                    continue;
                }
                JdOrderPt jdOrderPt = jdOrderPtMapper.selectOne(Wrappers.<JdOrderPt>lambdaQuery().eq(JdOrderPt::getOrderId, jdMchOrder.getOriginalTradeNo()));
                BuyRenderParam buyRenderParam = BuyRenderParam.buildBuyRenderParam();
                DouyinAppCk douyinAppCk = douyinAppCkMapper.selectOne(Wrappers.<DouyinAppCk>lambdaQuery().eq(DouyinAppCk::getUid, jdOrderPt.getPtPin()));
                DouyinMethodNameParam methodNameDetailInfo = douyinMethodNameParamMapper.selectOne(Wrappers.<DouyinMethodNameParam>lambdaQuery().eq(DouyinMethodNameParam::getMethodName, "detailInfo"));
                String info_url = BuildDouYinUrlUtils.buildSearchAndPackUrl(JSON.parseObject(JSON.toJSONString(buyRenderParam)), methodNameDetailInfo, douyinAppCk) + "&order_id=" + jdOrderPt.getOrderId();
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(info_url)
                        .get()
                        .addHeader("Cookie", PreAesUtils.decrypt解密(jdOrderPt.getCurrentCk()))
                        .addHeader("X-Khronos", "1665697911")
                        .addHeader("X-Gorgon", "8404d4860000775655c5b8f6315f8a608a802f3a78e4891a08cc")
                        .addHeader("User-Agent", "okhttp/3.10.0.1")
                        .addHeader("cache-control", "no-cache")
                        .build();
                Response response = client.newCall(request).execute();
                String body = response.body().string();
                System.out.println(body);
                if (StrUtil.isBlank(body)) {
                    log.info("对不起，没有查询成");
                    return;
                }
                log.info("订单号:{},查询成功数据:有数据", jdMchOrder.getTradeNo());
                jdOrderPt.setHtml(body);
                jdOrderPt.setOrgAppCk(DateUtil.formatDateTime(new Date()));
                /**
                 *    String html = JSON.parseObject(body).getString("order_detail_info");
                 *             if (StrUtil.isNotBlank(html)) {
                 *                 String shop_order_status_info = JSON.parseObject(html).getString("shop_order_status_info");
                 */
                if (StrUtil.isNotBlank(body) && body.contains("order_detail_info") && body.contains("shop_order_status_info")) {
                    String html = JSON.parseObject(JSON.parseObject(body).getString("order_detail_info")).getString("shop_order_status_info");
                    jdOrderPt.setHtml(html);
                }
                jdOrderPtMapper.updateById(jdOrderPt);
                String html = JSON.parseObject(body).getString("order_detail_info");
                String voucher_info_listStr = JSON.parseObject(html).getString("voucher_info_list");
                List<JSONObject> voucher_info_list = JSON.parseArray(voucher_info_listStr, JSONObject.class);
                if (CollUtil.isEmpty(voucher_info_list)) {
                    log.info("订单号:{},补单没有支付补单成功,没有支付", jdMchOrder.getTradeNo());
                    return;
                }
                JSONObject voucher_info = voucher_info_list.get(PreConstant.ZERO);
                String code = voucher_info.getString("code");
                if (StrUtil.isBlank(code)) {
                    log.info("没有支付");
                    return;
                }
                log.info("订单号:{}支付成功msg:", jdMchOrder.getTradeNo());
                if (StrUtil.isNotBlank(code)) {
                    updateSuccess(jdMchOrder, jdOrderPt, code, client);
                    return;
                }
            } catch (Exception e) {
                log.info("订单补单查询失败:{},{}", jdMchOrder.getTradeNo(), e.getMessage());
            }

        }
    }

    private void updateSuccess(JdMchOrder jdMchOrder, JdOrderPt jdOrderPt, String code, OkHttpClient client) {
        log.info("订单号{}，当前获取的卡密成功", jdMchOrder.getTradeNo());
        jdOrderPt.setCardNumber(PreAesUtils.encrypt加密(code));
        jdOrderPt.setCarMy(PreAesUtils.encrypt加密(code));
        jdOrderPt.setSuccess(PreConstant.ONE);
        jdOrderPt.setPaySuccessTime(new Date());
        jdOrderPtMapper.updateById(jdOrderPt);
        jdMchOrder.setStatus(PreConstant.TWO);
        jdMchOrderMapper.updateById(jdMchOrder);
        jdOrderPtMapper.updateById(jdOrderPt);
        jdOrderPtMapper.updateById(jdOrderPt);
        try {
            List<JdLog> jdLogs = jdLogMapper.selectList(Wrappers.<JdLog>lambdaQuery().eq(JdLog::getOrderId, jdMchOrder.getTradeNo()));
            if (CollUtil.isNotEmpty(jdLogs)) {
                log.info("订单号:{}删除redis黑名单:{}", jdMchOrder.getTradeNo(), jdLogs.get(PreConstant.ZERO).getIp());
                redisTemplate.delete("IP黑名单:" + jdLogs.get(PreConstant.ZERO).getIp());
                log.info("删除黑名单成功:{}", jdMchOrder.getTradeNo());
                redisTemplate.opsForValue().set("IP白名单:" + jdLogs.get(PreConstant.ZERO).getIp(), "1", 5, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.error("删除黑名单报错:{}", jdMchOrder.getTradeNo());
        }
        log.info("执行删除订单msg:{}", jdMchOrder.getTradeNo());
        try {
            isDelete(client, jdMchOrder, jdOrderPt);
        } catch (Exception e) {
            log.info("删除订单报错:{},e：{}", jdMchOrder.getTradeNo(), e.getMessage());
        }
    }

    public void isDelete(OkHttpClient client, JdMchOrder jdMchOrder, JdOrderPt jdOrderPt) {
        DouyinAppCk douyinAppCk = douyinAppCkMapper.selectOne(Wrappers.<DouyinAppCk>lambdaQuery().eq(DouyinAppCk::getUid, jdOrderPt.getPtPin()));
        jdOrderPt = jdOrderPtMapper.selectById(jdOrderPt.getId());
        DouyinMethodNameParam methodNamemethod_postExec = douyinMethodNameParamMapper.selectOne(Wrappers.<DouyinMethodNameParam>lambdaQuery().eq(DouyinMethodNameParam::getMethodName, "postExec"));

        if (jdOrderPt.getActionId().equals(100040)) {
            log.info("当前状态为100040。不需要修改", jdMchOrder.getTradeNo());
            return;
        }
        if (jdOrderPt.getActionId().equals(0)) {
            for (int i = 0; i < 2; i++) {
                Boolean isac100030 = isac100030Zr100040(client, jdMchOrder.getTradeNo(), jdOrderPt.getOrderId(), jdOrderPt.getCurrentCk(), "100030", douyinAppCk, methodNamemethod_postExec);
                if (isac100030) {
                    log.info("设置使用成功msg:{}", jdMchOrder.getTradeNo());
                    log.info("修改订单号的状态为msg:{}", jdMchOrder.getTradeNo(), "100030");
                    jdOrderPt = jdOrderPtMapper.selectById(jdOrderPt.getId());
                    jdOrderPt.setActionId(100030);
                    jdOrderPtMapper.updateById(jdOrderPt);
                    break;
                }
            }
        }
        jdOrderPt = jdOrderPtMapper.selectById(jdOrderPt.getId());
        if (jdOrderPt.getActionId().equals(100030)) {
            for (int i = 0; i < 2; i++) {

                Boolean isac100040 = isac100030Zr100040(client, jdMchOrder.getTradeNo(), jdOrderPt.getOrderId(), jdOrderPt.getCurrentCk(), "100040", douyinAppCk, methodNamemethod_postExec);
                if (isac100040) {
                    log.info("设置删除成功msg:{}", jdMchOrder.getTradeNo());
                    log.info("修改订单号的状态为msg:{}", jdMchOrder.getTradeNo(), "100040");
                    jdOrderPt = jdOrderPtMapper.selectById(jdOrderPt.getId());
                    jdOrderPt.setActionId(100040);
                    jdOrderPtMapper.updateById(jdOrderPt);
                    return;
                }
            }
        }
    }

    public Boolean isac100030Zr100040(OkHttpClient client, String tradeNo, String originalTradeNo, String currentCk, String ac, DouyinAppCk douyinAppCk, DouyinMethodNameParam methodNamemethod_postExec) {
        try {

            BuyRenderParam buyRenderParam = BuyRenderParam.buildBuyRenderParam();
            String postExec_url = BuildDouYinUrlUtils.buildSearchAndPackUrl(JSON.parseObject(JSON.toJSONString(buyRenderParam)), methodNamemethod_postExec, douyinAppCk);
            String datafromOri = "common_params=%7B%22enter_from%22%3A%22order_list_page%22%2C%22previous_page%22%3A%22mine_tab_order_list__order_homepage%22%7D&action_id=" + ac
                    + "&business_line=2&trade_type=0&source=1&ecom_appid=7386&lynx_support_version=1&order_id=" + originalTradeNo + "&page_size=15";
            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            RequestBody body = RequestBody.create(mediaType, datafromOri);
            Request request = new Request.Builder()
                    .url(postExec_url)
                    .post(body)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("Cookie", PreAesUtils.decrypt解密(currentCk))
                    .build();
            Response response = client.newCall(request).execute();
            String str100030 = response.body().string();
            if (str100030.contains("用户未登录")) {
                return true;
            }
            if (StrUtil.isNotBlank(str100030) && str100030.contains(ac) && JSON.parseObject(str100030).getInteger("status_code") == 0) {
                log.info("订单使用成功:{},msg:{},原始订单号:{}", ac, tradeNo, originalTradeNo);
                return true;
            }
        } catch (Exception e) {
            log.info("提交使用订单ac:{},数据报错msg:{},e:{}", ac, tradeNo, e.getMessage());
        }
        return false;
    }
}
