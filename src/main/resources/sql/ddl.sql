create table data_common_cache
(
  cache_id        bigint auto_increment comment '主键，生成序列号Id' primary key,
  cache_key       varchar(100)   not null comment '缓存的key 长度100 超过100的话通过编码后缩短',
  cache_value     text  default null comment '缓存的值',
  cache_expire    datetime default null comment '过期时间',
  constraint udx_cache_key unique (cache_key)
) comment '通用缓存表';

