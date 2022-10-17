package com.xd.pre.modules.sys.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jd_app_store_config")
public class JdAppStoreConfig {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String url;
    private String config;
    private String mark;

    private BigDecimal skuPrice;
    private String skuName;
    private String skuId;
    private Integer groupNum;
    private Integer expireTime;
    private Integer payIdExpireTime;
    private Integer productStockNum;
    private Integer isProduct;
    private Integer productNum;
    private Integer payType;


}
