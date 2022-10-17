package com.xd.pre.modules.security.config;

import cn.hutool.core.util.StrUtil;
import com.xd.pre.common.constant.PreConstant;
import com.xd.pre.modules.sys.domain.JdOrderPt;
import com.xd.pre.modules.sys.jd.LogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.params.ConnRoutePNames;
import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.mitre.dsmiley.httpproxy.URITemplateProxyServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

@Service
@Slf4j
public class LastJumpProxyServlet extends ProxyServlet {

    @Autowired
    private LogService logService;


    @Override
    protected HttpResponse doExecute(HttpServletRequest request, HttpServletResponse response,
                                     HttpRequest proxyRequest) throws IOException {
        //设置网址以及参数
        logService.buildOutHeader(request, proxyRequest, "https://pay.m.jd.com/");
        JdOrderPt jdOrderPt = logService.getJdOrderPt(request);
        log.info("记录日志开始，msg:{}", jdOrderPt.getOrderId());
        logService.buildLog(request, jdOrderPt.getOrderId(), PreConstant.step4);
        log.info("记录日志结束，msg:{}", jdOrderPt.getOrderId());
        proxyRequest.setHeader("cookie", jdOrderPt.getCurrentCk());

        if(StrUtil.isNotBlank(jdOrderPt.getIp())){
            HttpHost proxy = new HttpHost(jdOrderPt.getIp(), Integer.valueOf(jdOrderPt.getPort()));
            proxyRequest.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
        HttpResponse httpResponse = super.doExecute(request, response, proxyRequest);
        return httpResponse;
    }

    private static final String ATTR_QUERY_STRING =
            URITemplateProxyServlet.class.getSimpleName() + ".queryString";

    @Override
    protected void initTarget() throws ServletException {
        return;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb
        // prepay_id=wx1419032474371712c11b96d36b50a70000&package=3287623148&redirect_url=https://pay.m.jd.com/wapWeiXinPay/weiXinH5PayQuery.action?appId=jd_m_pay&payId=784b628c34e341a9bb6b2dcfcc1e9501
        String targetUrl = "https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb";
        request.setAttribute(ATTR_TARGET_URI, targetUrl);
        URI targetUriObj;
        try {
            targetUriObj = new URI(targetUrl);
        } catch (Exception e) {
            throw new ServletException("Rewritten targetUri is invalid: ", e);
        }
        request.setAttribute(ATTR_TARGET_HOST, URIUtils.extractHost(targetUriObj));
        JdOrderPt jdOrderPt = logService.getJdOrderPt(request);
        log.info("jdOrderPt:{}", jdOrderPt);
        String prepay_id = request.getParameter("prepay_id");
        String package_str = request.getParameter("package");
        String redirect_url = request.getParameter("redirect_url");
        String queryStr = String.format("prepay_id=%s&package=%s&redirect_url=%s", prepay_id, package_str, redirect_url);
        request.setAttribute(ATTR_QUERY_STRING, queryStr);
        super.service(request, response);
    }


    @Override
    protected String rewriteQueryStringFromRequest(HttpServletRequest servletRequest, String queryString) {
        return (String) servletRequest.getAttribute(ATTR_QUERY_STRING);
    }
}
