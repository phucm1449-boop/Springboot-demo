1. logout with JWT Token. Do we need to verify token before logout ? if need, pls mak a plan to enhance my code
2. when i access to PUT http://localhost:8088/api/v1/orders/5 with token role USER. Postman gets 401
   Unauthorized with JSON body {
   "code": 1005,
   "message": "Unauthenticated"
   }. but i expect Postman gets 403
3. why when i debug i see invalidatedToken has expiryDate = Sun Jun 07 19:06:05 ICT 2026 but when i check in DB, 
invalidated_token.expiry_date = 2026-06-07 12:06:05.000000. why ? i expect invalidated_token.expiry_date = 2026-06-07 19:06:05.000000
4. why i run createUser_validRequest_success in UserServiceTest, it shows tests failed

============================
   CONDITIONS EVALUATION REPORT
   ============================


Positive matches:
-----------------

AopAutoConfiguration matched:
- @ConditionalOnBooleanProperty (spring.aop.auto=true) matched (OnPropertyCondition)

AopAutoConfiguration.AspectJAutoProxyingConfiguration matched:
- @ConditionalOnClass found required class 'org.aspectj.weaver.Advice' (OnClassCondition)

AopAutoConfiguration.AspectJAutoProxyingConfiguration.CglibAutoProxyConfiguration matched:
- @ConditionalOnBooleanProperty (spring.aop.proxy-target-class=true) matched (OnPropertyCondition)

ApplicationAvailabilityAutoConfiguration#applicationAvailability matched:
- @ConditionalOnMissingBean (types: org.springframework.boot.availability.ApplicationAvailability; SearchStrategy: all) did not find any beans (OnBeanCondition)

DataSourceAutoConfiguration matched:
- @ConditionalOnClass found required classes 'javax.sql.DataSource', 'org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType' (OnClassCondition)
- @ConditionalOnMissingBean (types: ?; SearchStrategy: all) did not find any beans (OnBeanCondition)

DataSourceAutoConfiguration.PooledDataSourceConfiguration matched:
- AnyNestedCondition 1 matched 1 did not; NestedCondition on DataSourceAutoConfiguration.PooledDataSourceCondition.PooledDataSourceAvailable PooledDataSource found supported DataSource; NestedCondition on DataSourceAutoConfiguration.PooledDataSourceCondition.ExplicitType @ConditionalOnProperty (spring.datasource.type) did not find property 'spring.datasource.type' (DataSourceAutoConfiguration.PooledDataSourceCondition)
- @ConditionalOnMissingBean (types: javax.sql.DataSource,javax.sql.XADataSource; SearchStrategy: all) did not find any beans (OnBeanCondition)

DataSourceAutoConfiguration.PooledDataSourceConfiguration#jdbcConnectionDetails matched:
- @ConditionalOnMissingBean (types: org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails; SearchStrategy: all) did not find any beans (OnBeanCondition)
5. why it got this error when i start server
   E:\shopapp\demo\src\main\java\com\example\demo\config\ApplicationConfig.java:6:31
   java: cannot find symbol
   symbol:   class DTOMapperImpl
   location: package com.example.demo.mapper
6. pls check this error when i start server
   Parameter 3 of constructor in com.example.demo.services.ProductService required a bean of type 'com.example.demo.mapper.DTOMapper' that could not be found.


Action:

Consider defining a bean of type 'com.example.demo.mapper.DTOMapper' in your configuration.