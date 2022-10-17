package com.xd.pre.modules.sys.jd.vo.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateOrderRes {
    private String mch_id;
    private String trade_no;
    private String original_trade_no;
    private String out_trade_no;
    private String money;
    private String pay_url;
    private String expired_time;
}
