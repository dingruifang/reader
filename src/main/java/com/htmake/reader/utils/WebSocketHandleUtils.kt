package com.htmake.reader.utils

import io.legado.app.data.entities.BookSource
import io.vertx.core.json.JsonArray
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay


fun parseQueryParams(query: String): Map<String, String> {
    val params = mutableMapOf<String, String>()
    query.split("&").forEach { param ->
        val keyValue = param.split("=")
        if (keyValue.size == 2) {
            params[keyValue[0]] = keyValue[1]
        }
    }
    return params
}

fun loadBookSourceStringList(userNameSpace: String, bookSourceGroup: String = ""): List<String> {
    var bookSourceList: JsonArray? = asJsonArray(getUserStorage(userNameSpace, "bookSource"))
    var userBookSourceList = arrayListOf<String>()
    if (bookSourceList != null) {
        for (i in 0 until bookSourceList.size()) {
            var isAdd = true
            if (bookSourceGroup.isNotEmpty()) {
                val bookSource = bookSourceList.getJsonObject(i).mapTo(BookSource::class.java)
                if (!bookSource.bookSourceGroup.equals(bookSourceGroup)) {
                    isAdd = false
                }
            }
            if (isAdd) {
                userBookSourceList.add(bookSourceList.getJsonObject(i).toString())
            }
        }
    }
    return userBookSourceList
}

fun getUserStorage(context: Any, vararg path: String): String? {
    var userNameSpace = ""
    when(context) {
        is RoutingContext -> userNameSpace = getUserNameSpace(context)
        is String -> userNameSpace = context
    }
    if (userNameSpace.isEmpty()) {
        return getStorage("data", *path)
    }
    return getStorage("data", userNameSpace, *path)
}

fun getUserNameSpace(context: RoutingContext): String {
//    if (!appConfig.secure) {
//        return "default"
//    }
//    // 管理权限，可以修改 userNameSpace 来获取任意用户信息
//    checkManagerAuth(context)
//    var userNS = context.get("userNameSpace") as String?
//    if (userNS != null && userNS.isNotEmpty()) {
//        return userNS
//    }
//    var username = context.get("username") as String?
//    if (username != null) {
//        return username;
//    }
    return "default"
}