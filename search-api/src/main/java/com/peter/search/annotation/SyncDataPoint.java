package com.peter.search.annotation;

import com.peter.search.dto.OP_TYPE;
import org.springframework.cloud.client.discovery.EnableDiscoveryClientImportSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({EnableDiscoveryClientImportSelector.class})
public @interface SyncDataPoint {

    /** 业务标识*/
    String serviceTag() default "";
    /** docId参数名称*/
    String docIdParamName() default "docId";
    /** 操作类型：INSERT、UPSERT、UPDATE、DELETE*/
    OP_TYPE opType() default OP_TYPE.UPSERT;
    String routing() default "";
    String docDataType() default "json";
}