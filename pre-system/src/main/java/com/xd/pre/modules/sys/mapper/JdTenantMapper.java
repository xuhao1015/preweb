package com.xd.pre.modules.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xd.pre.modules.sys.domain.JdTenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JdTenantMapper extends BaseMapper<JdTenant> {
    @Select("select sku_id from jd_app_store_config where sku_price = #{money} and  is_product =1")
    String selectSkuId(@Param("money") String money);

    @Select("select sku_id from jd_app_store_config where sku_price = #{money} and  is_product =1 and group_num=#{douyin}")
    String selectSkuIdDouYin(@Param("money") String amount, @Param("douyin") Integer douyin);


}
