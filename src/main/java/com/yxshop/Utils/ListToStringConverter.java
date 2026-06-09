/**
 * 将List<String>类型的数据转换为字符串存储，以及将字符串恢复为List<String>类型的转换器。
 * 这个转换器利用Jackson的ObjectMapper来处理JSON的序列化和反序列化。
 */
package com.yxshop.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import java.io.IOException;
import java.util.List;

//@Converter
public class ListToStringConverter implements AttributeConverter<List<String>, String> {
    /**
     * ObjectMapper实例，用于处理JSON的序列化和反序列化。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将List<String>类型的数据转换为JSON字符串，以存储到数据库中。
     *
     * @param attribute 待转换的List<String>对象。
     * @return 转换后的JSON字符串。
     * @throws IllegalArgumentException 如果转换过程中发生IO异常，则抛出此异常。
     */
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error converting list to JSON", e);
        }
    }

    /**
     * 将数据库中的字符串数据转换为List<String>类型。
     *
     * @param dbData 从数据库读取的JSON字符串。
     * @return 转换后的List<String>对象。
     * @throws IllegalArgumentException 如果转换过程中发生IO异常，则抛出此异常。
     */
    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, List.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error converting JSON to list", e);
        }
    }

}
