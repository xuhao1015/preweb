package com.xd.pre.modules.security.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Servlet;
import java.util.Map;

/**
 * 实现代理配置
 *
 * @author zz
 * @since 2020/7/7
 */
@Configuration
public class ProxyServletConfiguration {

    /**
     * https://github.com/mitre/HTTP-Proxy-Servlet
     */
    // 读取配置文件中路由设置
    private String jumpUrlMapping = "jd/jdappmpay/*";
    // 读取配置文件中路由设置
    private String checkUrlMapping = "jd/check/*";

    private String wxUrlMapping = "jd/wx/*";

    private String lastJumpUrlMapping = "jd/lastJump/*";

    private String testJumpUrlMapping = "jd/test/*";


    // 临时跳转。实际还是得重写才有跳转实际意义
    private String jumpTargetUrl = "https://xxxx";

    private String checkTatgetUrl = "https://xxxxxx";

    private String txTatgetUrl = "https://xxxxxx";

    private String lastJumpTatgetUrl = "https://xxxxxx";

    private String testJumpTatgetUrl = "http://10.16.122.100:8081/jd/test";

    @Bean
    public Servlet createJumpProxyServlet() {
        return new JumpProxyServlet();
    }

    @Bean
    public Servlet createCheckProxyServlet() {
        return new CheckProxyServlet();
    }

    @Bean
    public Servlet createWxProxyServlet() {
        return new WxTenpayCom();
    }

    @Bean
    public Servlet createLastJumpProxyServlet() {
        return new LastJumpProxyServlet();
    }

    @Bean
    public Servlet creatTestLastJumpProxyServlet() {
        return new TestProxyServlet();
    }

    @Bean
    public ServletRegistrationBean testServletRegistrationBean() {
        Servlet testProxyServlet = creatTestLastJumpProxyServlet();
        return getServletRegistrationBean(testProxyServlet, testJumpUrlMapping, testJumpTatgetUrl);
    }

    @Bean
    public ServletRegistrationBean jumServletRegistrationBean() {
        Servlet jumpProxyServlet = createJumpProxyServlet();
        return getServletRegistrationBean(jumpProxyServlet, jumpUrlMapping, jumpTargetUrl);
    }

    @Bean
    public ServletRegistrationBean checkServletRegistrationBean() {
        Servlet checkProxyServlet = createCheckProxyServlet();
        return getServletRegistrationBean(checkProxyServlet, checkUrlMapping, checkTatgetUrl);
    }

    @Bean
    public ServletRegistrationBean wxServletRegistrationBean() {
        Servlet wxProxyServlet = createWxProxyServlet();
        return getServletRegistrationBean(wxProxyServlet, wxUrlMapping, txTatgetUrl);
    }

    @Bean
    public ServletRegistrationBean lastServletRegistrationBean() {
        Servlet lastJumpProxyServlet = createLastJumpProxyServlet();
        return getServletRegistrationBean(lastJumpProxyServlet, lastJumpUrlMapping, lastJumpTatgetUrl);
    }

    private ServletRegistrationBean getServletRegistrationBean(Servlet checkProxyServlet, String checkUrl, String targetUrl) {
        ServletRegistrationBean registrationBean = new ServletRegistrationBean(checkProxyServlet, checkUrl);
        //设置网址以及参数
       /* Map<String, String> params = ImmutableMap.of(
                "targetUri", targetUrl,
                "log", "true"
        ,"forwardip","false");
//        "forwardip", "true"
        registrationBean.setInitParameters(params);*/
        return registrationBean;
    }

}

