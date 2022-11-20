package com.xd.pre.modules.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xd.pre.modules.sys.domain.JdMchOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

public interface JdMchOrdermapper extends BaseMapper<JdMchOrder> {

    @Update("update  jd_mch_order  set click_pay = #{clickDate} where trade_no = #{orderId}")
    void updateClickDataTime(@Param("orderId") String orderId, @Param("clickDate") String clickDate);


    @Select("SELECT " +
            " mo.* " +
            " FROM " +
            " jd_mch_order mo " +
            " LEFT JOIN jd_order_pt op ON op.id = mo.original_trade_id  " +
            " WHERE " +
            " mo.create_time > DATE_SUB( SYSDATE( ), INTERVAL 30 MINUTE )  " +
            " and mo.create_time < DATE_SUB( SYSDATE( ), INTERVAL  2  MINUTE )  " +
            " and mo.original_trade_id is not null  and mo.click_pay is not null  " +
            " and mo.click_pay !='1970-01-01 08:00:00' " +
            " AND op.html IS NULL " +
            " and mo.`status`!=2;")
    List<JdMchOrder> selectBuDan();

}
