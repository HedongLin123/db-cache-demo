package com.itdl.config;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.itdl.config.mybatis.EasySqlInjector;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * @Description 数据库相关配置
 * @Author itdl
 * @Date 2022/08/10 09:20
 */
@Configuration
@MapperScan(basePackages = "com.itdl.mapper", sqlSessionFactoryRef = "sqlSessionFactory")
public class DatasourceConfig {


    @Bean(name = "dataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() throws SQLException {
        return DataSourceBuilder.create().build();
    }


    @Bean("easySqlInjector")
    public EasySqlInjector easySqlInjector() {
        return new EasySqlInjector();
    }


    @Bean
    public GlobalConfig globalConfig(EasySqlInjector easySqlInjector){
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setSqlInjector(easySqlInjector);
        return globalConfig;
    }

    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource, GlobalConfig globalConfig) throws Exception {

        MybatisSqlSessionFactoryBean sessionFactoryBean = new MybatisSqlSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource);
        sessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().
                getResources("classpath*:mapper/**/*.xml"));
        sessionFactoryBean.setPlugins(new PaginationInterceptor());

        //添加自定义sql注入接口
        sessionFactoryBean.setGlobalConfig(globalConfig);//添加自定义sql注入接口
        return sessionFactoryBean.getObject();
    }

}
