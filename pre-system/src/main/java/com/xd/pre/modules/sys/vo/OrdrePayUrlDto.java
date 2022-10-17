package com.xd.pre.modules.sys.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrdrePayUrlDto {
    /**
     * SELECT
     * co.order_client_id AS orderId,
     * co.order_pt_id as realOrderId,
     * op.wx_pay_expire_time as wxPayExpireTime,
     * op.wx_pay_url as wxPayUrl
     * FROM
     * jd_client_order co
     * LEFT JOIN jd_order_pt op ON co.order_pt_id = op.order_id
     * WHERE
     * co.order_client_id = 123456
     */
    private String orderId;
    private String realOrderId;
    private Long wxPayExpireTime;
    private String wxPayUrl;
    private Date createTime;
    private String returnUrl;
    private String  hrefUrl;
    private Integer payType;
}
