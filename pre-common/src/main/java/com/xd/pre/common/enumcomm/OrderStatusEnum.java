package com.xd.pre.common.enumcomm;

import lombok.Data;

@Data
public class OrderStatusEnum {

    public static final Integer 支付失败 = 0;
    public static final Integer 待支付 = 1;
    public static final Integer 支付成功 = 2;

    public static final Integer 未通知 = 0;
    public static final Integer 已经通知 = 1;

    public static final Integer 抖音通道 = 4;
    public static final Integer 门店 = 5;
}
