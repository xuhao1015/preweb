package com.xd.pre.modules.sys.jd.vo.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SelectOrderReq {
    private String pass_code;
    private String money;
    private String trade_no;
    private String out_trade_no;
    private String original_trade_no;
    private Integer status;
    private Integer notify_succ;
    private String notify_url;
    private String pay_url;
    private String subject;
    private String body;
}
