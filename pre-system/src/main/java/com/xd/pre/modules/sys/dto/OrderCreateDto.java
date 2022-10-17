package com.xd.pre.modules.sys.dto;

import lombok.Data;

@Data
public class OrderCreateDto {
    private String token;
    private String price;
    private Integer groupNum;
    private String callBackUrl;
    private String tenantOrderId;

}
