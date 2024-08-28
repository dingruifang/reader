package com.htmake.reader.verticle

import com.htmake.reader.api.ReturnData
import com.htmake.reader.utils.WxValAndConstUtil
import com.htmake.reader.utils.jsonEncode
import io.vertx.core.http.ServerWebSocket

open class WxHandleDataController {
    fun searchBookMultiSSE(ws: ServerWebSocket) {
        println("获取到的集合数据包数：${WxValAndConstUtil.bookInfoByJson.size}")

        val returnData = ReturnData()
//        if (WxValAndConstUtil.bookInfoByJson.isEmpty()) {
//            ws.writeTextMessage(jsonEncode(returnData.setErrorMsg("没有找到书信息，请重试"), false))
//
//        }
        WxValAndConstUtil.bookInfoByJson.forEach { item ->
            ws.writeTextMessage(item)
        }
        ws.end()
    }
}