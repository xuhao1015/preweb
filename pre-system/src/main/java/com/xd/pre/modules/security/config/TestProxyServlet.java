package com.xd.pre.modules.security.config;

import com.xd.pre.modules.sys.jd.LogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.conn.params.ConnRoutePNames;
import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Service
public class TestProxyServlet extends ProxyServlet {

    @Autowired
    private LogService logService;

    @Override
    protected HttpResponse doExecute(HttpServletRequest request, HttpServletResponse response,
                                     HttpRequest proxyRequest) throws IOException {
        logService.buildOutHeader(request, proxyRequest, "https://wqs.jd.com/");
        HttpHost proxy = new HttpHost("175.146.212.14", 4256);
        proxyRequest.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        HttpResponse httpResponse = super.doExecute(request, response, proxyRequest);
        return httpResponse;
    }

}
