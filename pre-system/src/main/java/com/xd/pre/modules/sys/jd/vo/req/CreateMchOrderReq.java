package com.xd.pre.modules.sys.jd.vo.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateMchOrderReq {

    @NotBlank(message = "商户id不能为空")
    private String mch_id;
    @NotBlank(message = "签证不能为空")
    private String sign;
    @NotBlank(message = "通道不能为空")
    private String pass_code;
    @NotBlank(message = "标题不能为空")
    private String subject;

    private String body;
    @NotBlank(message = "商户订单号不能为空")
    private String out_trade_no;
    @NotBlank(message = "金额不能为空")
    private String amount;

    @NotBlank(message = "调用ip不能为空")
    private String client_ip;

    @NotBlank(message = "通知地址不能为空")
    private String notify_url;

    private String return_url;
    @NotBlank(message = "请求创建订单时间不能为空")
    private String timestamp;

}
