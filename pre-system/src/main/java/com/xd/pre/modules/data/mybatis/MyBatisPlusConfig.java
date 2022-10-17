package com.xd.pre.modules.data.mybatis;

import com.baomidou.mybatisplus.core.parser.ISqlParser;
import com.baomidou.mybatisplus.extension.parsers.BlockAttackSqlParser;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName MyBatisPlusConfig
 * @Author lihaodong
 * @Date 2019/1/10 18:02
 * @Mail lihaodongmail@163.com
 * @Description Mybatis-Plus配置
 * @Version 1.0
 **/

@EnableTransactionManagement
@Configuration
@MapperScan({"com.xd.pre.**.mapper"})
public class MyBatisPlusConfig {



    /**
     * 分页插件
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        List<ISqlParser> sqlParserList = new ArrayList<>();
        // 攻击 SQL 阻断解析器、加入解析链
        sqlParserList.add(new BlockAttackSqlParser());
        // 多租户拦截
        paginationInterceptor.setSqlParserList(sqlParserList);
        return paginationInterceptor;
    }


}
