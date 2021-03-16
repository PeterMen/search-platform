package com.peter.search.util;

import tk.mybatis.mapper.entity.Example;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ExampleUtil {

    /**
     * 组装查询条件
     * @param obj
     * @return
     * @throws Exception
     */
    public static Example getCondition(Object obj, Consumer<Example.Criteria>... consumers)  {
        Map<?, ?> map = null;
        try {
            map = getObjectValue(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Example example = new Example(obj.getClass());
        Example.Criteria criteria = example.createCriteria();
        map.forEach((k, v) -> {
            criteria.andEqualTo(String.valueOf(k), v);
        });
        if(consumers != null && consumers.length > 0){
            for(Consumer<Example.Criteria> consumer : consumers){
                consumer.accept(criteria);
            }
        }
        return example;
    }

    private static String splitString(String key, Integer start, Integer end) {
        return key.substring(start, end);
    }

    public static Map getObjectValue(Object object) throws Exception {
        Map map = new HashMap<>();
        if (object != null) {
            for(Class<?> clazz = object.getClass() ; clazz != Object.class ; clazz = clazz.getSuperclass()) {
                // 获取实体类的所有属性，返回Field数组
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    /**
                     * 这里需要说明一下：他是根据拼凑的字符来找你写的getter方法的
                     * 在Boolean值的时候是isXXX（默认使用ide生成getter的都是isXXX）
                     * 如果出现NoSuchMethod异常 就说明它找不到那个gettet方法 需要做个规范
                     */
                    addFiled(field, map, object);// 如果type是类类型，则前面包含"class
                }
            }
        }
        return map;
    }

    public static void addFiled(Field field, Map map, Object object) throws IllegalArgumentException, IllegalAccessException{
        field.setAccessible(true);
        Object object2 = field.get(object);
        if (object2 != null) {
            map.put(field.getName(), object2);
        }
    }
}
