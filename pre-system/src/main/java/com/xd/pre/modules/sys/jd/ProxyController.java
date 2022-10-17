package com.xd.pre.modules.sys.jd;

import com.alibaba.fastjson.JSON;
import com.xd.pre.common.utils.Gzip;
import com.xd.pre.common.utils.PreUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@RestController
@RequestMapping("jd")
@Slf4j
@CrossOrigin(origins = "*")
public class ProxyController {

    private String targetAddr = "https://wx.tenpay.com";
//
//    @Resource
//    private JdMchOrdermapper jdMchOrdermapper;
//
//    @Resource
//    private JdLogMapper jdLogMapper;
//
//    @Autowired
//    private StringRedisTemplate redisTemplate;
//
//    /**
//     * 代理所有请求
//     *
//     * @param request
//     * @param response
//     * @throws Exception
//     */
    @RequestMapping(value = "/proxy/**", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void proxy(HttpServletRequest request, HttpServletResponse response) throws Exception {
        URI uri = new URI(request.getRequestURI());
        String path = uri.getPath();
        String query = request.getQueryString();
        String target = targetAddr + path.replace("/jd/proxy", "");
        if (query != null && !query.equals("") && !query.equals("null")) {
            target = target + "?" + query;
        }
        URI newUri = new URI(target);
        // 执行代理查询
        String methodName = request.getMethod();
        HttpMethod httpMethod = HttpMethod.resolve(methodName);
        if (httpMethod == null) {
            return;
        }
        ClientHttpRequest delegate = new SimpleClientHttpRequestFactory().createRequest(newUri, httpMethod);
        buildOutHeader(request, delegate);
        requestHttps(request, response, delegate);
    }
//
//
//    private void buildLog(HttpServletRequest request, String target) {
//        try {
//            UrlEntity urlEntity = PreUtils.parseUrl(target);
//            log.info("json:{}", JSON.toJSONString(urlEntity));
//            String prepay_id = urlEntity.getParams().get("prepay_id");
//            JdOrderPt jdOrderPt = jdOrderPtMapper.selectOne(Wrappers.<JdOrderPt>lambdaQuery().like(JdOrderPt::getWxPayUrl, prepay_id));
//            JdLog jdLog = new JdLog();
//            if (ObjectUtil.isNotNull(jdOrderPt)) {
//                jdLog.setOrderId(jdOrderPt.getOrderId());
//            }
//            String user_agent = request.getHeader("user-agent");
//            jdLog.setType(2);
//            jdLog.setUserAgent(user_agent);
//            String ip = request.getHeader("X-Forwarded-For");
//            jdLog.setIp(ip);
//            jdLogMapper.insert(jdLog);
//            log.info("日志信息msg:{}", jdLog);
//        } catch (Exception e) {
//            log.error("第二种方式请求日志报错msg:{}", e.getMessage());
//        }
//    }
//
//    @Resource
//    private JdOrderPtMapper jdOrderPtMapper;
//
//    @RequestMapping(value = "/proxyJd/**", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
//    public void proxyJd(HttpServletRequest request, HttpServletResponse response) throws IOException, URISyntaxException {
//        //https://pay.m.jd.com/index.action?functionId=wapWeiXinPay&body={"appId":"jd_m_pay","payId":"f079ffa68f534f46b49dcfe8b49b75e0","eid":"Hsr2vxITFjuVpgI6T6z1E9JU1WBO1qbeKEAYZrzdIzDvCmjweBQilPm6IT7zL6zrJ5hOWGExVwHdC1PhbPfeaFnX9R7Kdnfn9zHLPvQSnzrVfsE586P2"}&appId=jd_m_pay
//        String url = "https://pay.m.jd.com/index.action?";
//        String param = "functionId=wapWeiXinPay&body=%s&appId=jd_m_pay";
//        String refer = "https://pay.m.jd.com/cpay/newPay-index.html?appId=jd_m_yxdk&payId=%s";
//        String prerId = request.getParameter("prerId");
//        String body = String.format("{\"appId\":\"jd_m_pay\",\"payId\":\"%s\",\"eid\":\"%s\"}"
//                , prerId, PreUtils.getRandomString("Hsr2vxITFjuVpgI6T6z1E9JU1WBO1qbeKEAYZrzdIzDvCmjweBQilPm6IT7zL6zrJ5hOWGExVwHdC1PhbPfeaFnX9R7Kdnfn9zHLPvQSnzrVfsE586P2".length()));
//        JdOrderPt jdOrderPt = jdOrderPtMapper.selectOne(Wrappers.<JdOrderPt>lambdaQuery().eq(JdOrderPt::getPrerId, prerId));
//
//        String paramBuild = String.format(param, URLEncoder.encode(body, "utf-8"));
//        URI newUri = new URI(url + paramBuild);
//        String methodName = request.getMethod();
//        HttpMethod httpMethod = HttpMethod.resolve(methodName);
//        ClientHttpRequest delegate = new SimpleClientHttpRequestFactory().createRequest(newUri, httpMethod);
//        delegate.getHeaders().add("referer", String.format(refer, prerId));
//        delegate.getHeaders().add("cookie", jdOrderPt.getCurrentCk());
//        delegate.getHeaders().remove("Host");
//        delegate.getHeaders().add("Host", "pay.m.jd.com");
//        delegate.getHeaders().add("user-agent", jdOrderPt.getCurrentCk());
//        requestHttps(request, response, delegate);
//    }
//
//    @RequestMapping(value = "/proxyOrderId/**", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
//    public void proxyOrderId(HttpServletRequest request, HttpServletResponse response) throws IOException, URISyntaxException {
//        //https://pay.m.jd.com/index.action?functionId=wapWeiXinPay&body={"appId":"jd_m_pay","payId":"f079ffa68f534f46b49dcfe8b49b75e0","eid":"Hsr2vxITFjuVpgI6T6z1E9JU1WBO1qbeKEAYZrzdIzDvCmjweBQilPm6IT7zL6zrJ5hOWGExVwHdC1PhbPfeaFnX9R7Kdnfn9zHLPvQSnzrVfsE586P2"}&appId=jd_m_pay
//        String url = "https://wq.jd.com/jdpaygw/jdappmpay?dealId=%s";
//        String dealId = request.getParameter("dealId");
//        String urlBuild = String.format(url, dealId);
//        URI newUri = new URI(urlBuild);
//        String methodName = request.getMethod();
//        HttpMethod httpMethod = HttpMethod.resolve(methodName);
//        JdOrderPt jdOrderPt = jdOrderPtMapper.selectOne(Wrappers.<JdOrderPt>lambdaQuery().eq(JdOrderPt::getOrderId, dealId));
//        ClientHttpRequest delegate = new SimpleClientHttpRequestFactory().createRequest(newUri, httpMethod);
//        delegate.getHeaders().add("cookie", jdOrderPt.getCurrentCk());
//        delegate.getHeaders().remove("Host");
//        delegate.getHeaders().remove("referer");
//        delegate.getHeaders().add("referer", "https://wqs.jd.com/");
//        delegate.getHeaders().add("Host", "pay.m.jd.com");
//        requestHttps(request, response, delegate);
//    }
//
//    @RequestMapping(value = "/proxyJum/**", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
//    public void proxyJum(HttpServletRequest request, HttpServletResponse response) throws IOException, URISyntaxException {
//        //https://pay.m.jd.com/index.action?functionId=wapWeiXinPay&body={"appId":"jd_m_pay","payId":"f079ffa68f534f46b49dcfe8b49b75e0","eid":"Hsr2vxITFjuVpgI6T6z1E9JU1WBO1qbeKEAYZrzdIzDvCmjweBQilPm6IT7zL6zrJ5hOWGExVwHdC1PhbPfeaFnX9R7Kdnfn9zHLPvQSnzrVfsE586P2"}&appId=jd_m_pay
//        String url = "https://pay.m.jd.com/cpay/newPay-index.html?payId=%s&appId=jd_m_pay";
//        String payId = request.getParameter("payId");
//        String urlBuild = String.format(url, payId);
//        URI newUri = new URI(urlBuild);
//        String methodName = request.getMethod();
//        HttpMethod httpMethod = HttpMethod.resolve(methodName);
//        JdOrderPt jdOrderPt = jdOrderPtMapper.selectOne(Wrappers.<JdOrderPt>lambdaQuery().eq(JdOrderPt::getPrerId, payId));
//        ClientHttpRequest delegate = new SimpleClientHttpRequestFactory().createRequest(newUri, httpMethod);
//        delegate.getHeaders().add("cookie", jdOrderPt.getCurrentCk());
//        delegate.getHeaders().remove("Host");
//        delegate.getHeaders().remove("referer");
//        delegate.getHeaders().add("referer", "https://wqs.jd.com/");
//        delegate.getHeaders().add("Host", "pay.m.jd.com");
//        requestHttps(request, response, delegate);
//    }
//
//    @RequestMapping(value = "/jumpurl/**", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
//    public void proxyJumpurl(HttpServletRequest request, HttpServletResponse response) throws IOException, URISyntaxException {
//        String url = "https://wq.jd.com/jdpaygw/jdappmpay?dealId=%s";
//        String orderId = request.getParameter("orderId");
//        log.info("根据最新匹配信息。获取到订单京东的订单Id");
//        String jdMchStr = redisTemplate.opsForValue().get("匹配锁定成功:" + orderId);
//        Assert.isTrue(StrUtil.isNotBlank(jdMchStr), "过期支付");
//        JdMchOrder jdMchOrder = JSON.parseObject(jdMchStr, JdMchOrder.class);
//        jdMchOrder = jdMchOrdermapper.selectById(jdMchOrder.getId());
//        JdOrderPt jdOrderPt = jdOrderPtMapper.selectOne(Wrappers.<JdOrderPt>lambdaQuery().eq(JdOrderPt::getOrderId, jdMchOrder.getOriginalTradeNo()));
//        String urlBuild = String.format(url, jdOrderPt.getOrderId());
//        log.info("支付订单信息msg:{}", urlBuild);
//        URI newUri = new URI("http://.16.122.100:8888/pre/jd/test");
////        HttpResponse execute = HttpRequest.get("http://110.16.122.100:8888/pre/jd/test").execute();
////        log.info("{}",execute.body());
////        URI newUri = new URI(urlBuild);
//        String methodName = request.getMethod();
//        HttpMethod httpMethod = HttpMethod.resolve(methodName);
//        ClientHttpRequest delegate = new SimpleClientHttpRequestFactory().createRequest(newUri, httpMethod);
//        log.info("封装请求头");
//        buildOutHeader(request, delegate);
//        delegate.getHeaders().add("cookie", jdOrderPt.getCurrentCk());
//        requestHttps(request, response, delegate);
//    }
//
//    @RequestMapping(value = "/check/**", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
//    public void proxyCheck(HttpServletRequest request, HttpServletResponse response) throws IOException, URISyntaxException {
//      /*  String url = "https://wq.jd.com/jdpaygw/jdappmpay?dealId=%s";
//        String orderId = request.getParameter("orderId");
//        log.info("根据最新匹配信息。获取到订单京东的订单Id");
//        String jdMchStr = redisTemplate.opsForValue().get("匹配锁定成功:" + orderId);
//        Assert.isTrue(StrUtil.isNotBlank(jdMchStr), "过期支付");
//        JdMchOrder jdMchOrder = JSON.parseObject(jdMchStr, JdMchOrder.class);
//        String urlBuild = String.format(url, orderId);
//        log.info("支付订单信息msg:{}", urlBuild);
////        URI newUri = new URI("http://505442n6n0.zicp.vip:34679/pre/jd/test");
//        URI newUri = new URI("urlBuild");
//        JdOrderPt jdOrderPt = jdOrderPtMapper.selectOne(Wrappers.<JdOrderPt>lambdaQuery().eq(JdOrderPt::getOrderId, jdMchOrder.getOriginalTradeNo()));
//        String methodName = request.getMethod();
//        HttpMethod httpMethod = HttpMethod.resolve(methodName);
//        ClientHttpRequest delegate = new SimpleClientHttpRequestFactory().createRequest(newUri, httpMethod);
//        log.info("封装请求头");
//        buildOutHeader(request, delegate);
//        delegate.getHeaders().add("cookie", jdOrderPt.getCurrentCk());
//        requestHttps(request, response, delegate);*/
//        return;
//    }
//
//
    private void requestHttps(HttpServletRequest request, HttpServletResponse response, ClientHttpRequest delegate) throws Exception {
        StreamUtils.copy(request.getInputStream(), delegate.getBody());
        // 执行远程调用
        ClientHttpResponse clientHttpResponse = delegate.execute();
        response.setStatus(clientHttpResponse.getStatusCode().value());
        // 设置响应头
        clientHttpResponse.getHeaders().forEach((key, value) -> value.forEach(it -> {
            response.setHeader(key, it);
        }));
        InputStream body = clientHttpResponse.getBody();
        String bodyData = Gzip.zipInputStream(body);
        log.info("请求地址的data：{}", bodyData);
        if (bodyData.contains("jumpurl")) {
            log.info("获取到跳转页面");
            String jumpurl = JSON.parseObject(JSON.parseObject(bodyData).get("data").toString()).get("jumpurl").toString();
            log.info("其实根据这个就可以请求校验信息了");
        }
        StreamUtils.copy(Gzip.compress(bodyData), response.getOutputStream());
    }
//
//
//    /**
//     * 封装额外的请求头
//     *
//     * @param request
//     * @param delegate
//     */
    private void buildOutHeader(HttpServletRequest request, ClientHttpRequest delegate) {
        Enumeration<String> headerNames = request.getHeaderNames();
        // 设置请求头
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> v = request.getHeaders(headerName);
            List<String> arr = new ArrayList<>();
            while (v.hasMoreElements()) {
                arr.add(v.nextElement());
            }
            delegate.getHeaders().addAll(headerName, arr);
        }
        String ip = PreUtils.getIPAddress(request);
        log.info("用户代理真时ipmsg:{}", ip);
        delegate.getHeaders().remove("X-Forwarded-For");
        delegate.getHeaders().remove("x-forwarded-for");
        delegate.getHeaders().remove("Host");
        delegate.getHeaders().add("Host", "pay.m.jd.com");
        delegate.getHeaders().remove("Referer");
        delegate.getHeaders().remove("referer");
        delegate.getHeaders().add("Referer", "https://pay.m.jd.com/");
        delegate.getHeaders().add("X-Forwarded-For", ip);
    }

}