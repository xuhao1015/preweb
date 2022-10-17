package com.xd.pre.common.utils;

import lombok.Data;

import java.util.Map;

@Data
public class UrlEntity {
    /**
     * 基础url
     */
    public String baseUrl;
    /**
     * url参数
     */
    public Map<String, String> params;
}
