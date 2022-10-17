package com.xd.pre.modules.sys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayFindStatusByOderIdRes {
    private String orderId;
    private Long createTime;
    private BigDecimal price;
    private Integer orderStatus;
    private Long successTime;
}
