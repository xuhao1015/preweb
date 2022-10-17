package com.xd.pre.modules.sys.jd.vo.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LogReq {
    private String orderId;
    private String userAgent;
}
