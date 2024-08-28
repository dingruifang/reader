package com.htmake.reader.utils;

import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class WxValAndConstUtil {
    /**
     * JSON格式的书本信息
     */
    public static ArrayList<String> bookInfoByJson = new ArrayList<>();

    /**
     * 上一次查询的uri
     */
    public static String beforeSearchUri = "";
}
