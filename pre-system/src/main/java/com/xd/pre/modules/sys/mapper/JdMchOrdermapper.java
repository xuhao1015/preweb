package com.xd.pre.modules.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xd.pre.modules.sys.domain.JdMchOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

public interface JdMchOrdermapper extends BaseMapper<JdMchOrder> {

    @Update("update  jd_mch_order  set click_pay = #{clickDate} where trade_no = #{orderId}")
    void updateClickDataTime(@Param("orderId") String orderId, @Param("clickDate") String clickDate);
}
