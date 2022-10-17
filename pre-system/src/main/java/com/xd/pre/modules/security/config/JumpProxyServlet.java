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


@Slf4j
@Service
public class JumpProxyServlet extends ProxyServlet {


    @Autowired
    private LogService logService;


    @Override
    protected HttpResponse doExecute(HttpServletRequest request, HttpServletResponse response,
                                     HttpRequest proxyRequest) throws IOException {
        logService.buildOutHeader(request, proxyRequest, "https://wqs.jd.com/");
        log.info("获取当前ck");
        JdOrderPt jdOrderPt = logService.getJdOrderPt(request);
        log.info("记录日志开始，msg:{}", jdOrderPt.getOrderId());
        logService.buildLog(request, jdOrderPt.getOrderId(), PreConstant.step1);
        proxyRequest.setHeader("cookie", jdOrderPt.getCurrentCk());
        log.info("记录日志结束，msg:{}", jdOrderPt.getOrderId());
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
    protected void initTarget() {
        return;
    }

    @Override
    protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
            throws ServletException, IOException {
        String targetUrl = "https://wq.jd.com/jdpaygw/jdappmpay";
        servletRequest.setAttribute(ATTR_TARGET_URI, targetUrl);
        URI targetUriObj;
        try {
            targetUriObj = new URI(targetUrl);
        } catch (Exception e) {
            throw new ServletException("Rewritten targetUri is invalid: ", e);
        }
        servletRequest.setAttribute(ATTR_TARGET_HOST, URIUtils.extractHost(targetUriObj));
        //Determine the new query string based on removing the used names
        JdOrderPt jdOrderPt = logService.getJdOrderPt(servletRequest);
        log.info("jdOrderPt:{}", jdOrderPt);
        String newQueryBuf = String.format("dealId=%s", jdOrderPt.getOrderId());
        servletRequest.setAttribute(ATTR_QUERY_STRING, newQueryBuf);
        super.service(servletRequest, servletResponse);
    }


    @Override
    protected String rewriteQueryStringFromRequest(HttpServletRequest servletRequest, String queryString) {
        return (String) servletRequest.getAttribute(ATTR_QUERY_STRING);
    }


}
