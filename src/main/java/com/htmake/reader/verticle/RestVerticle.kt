package com.htmake.reader.verticle

import com.htmake.reader.api.ReturnData
import com.htmake.reader.utils.*
import com.htmake.reader.utils.WxValAndConstUtil.beforeSearchUri
import com.htmake.reader.utils.WxValAndConstUtil.bookInfoByJson
import io.vertx.core.AsyncResult
import io.vertx.core.Promise
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.SessionHandler
import io.vertx.ext.web.handler.sockjs.SockJSSocket
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.http.RealResponseBody
import okhttp3.internal.wait
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.io.InputStream
import java.net.*
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread


private val logger = KotlinLogging.logger {}

abstract class RestVerticle : CoroutineVerticle() {

    protected lateinit var router: Router

    open var port: Int = 8080

    // 创建一个 HttpClient 实例
//    val client = HttpClient()

    override suspend fun start() {
        println(123456)
        super.start()
        router = Router.router(vertx)

        val cookieName = "reader.session"
        router.route().handler(
            SessionHandler.create(LocalSessionStore.create(vertx))
                .setSessionCookieName(cookieName)
                .setSessionTimeout(7L * 86400 * 1000)
                .setSessionCookiePath("/")
        );
        router.route().handler {
            it.addHeadersEndHandler { _ ->
                val cookie = it.getCookie(cookieName)
                if (cookie != null) {
                    // 每次访问都延长cookie有效期
                    cookie.setMaxAge(2L * 86400 * 1000)
                    cookie.setPath("/")
                }
            }
            it.next()
        }

        // CORS support
        router.route().handler {
            it.addHeadersEndHandler { _ ->
                val origin = it.request().getHeader("Origin")
                if (origin != null && origin.isNotEmpty()) {
                    var res = it.response()
                    res.putHeader("Access-Control-Allow-Origin", origin)
                    res.putHeader("Access-Control-Allow-Credentials", "true")
                    res.putHeader("Access-Control-Allow-Methods", "GET, POST, PATCH, PUT, DELETE")
                    res.putHeader(
                        "Access-Control-Allow-Headers",
                        "Authorization, Content-Type, If-Match, If-Modified-Since, If-None-Match, If-Unmodified-Since, X-Requested-With"
                    )
                }
            }
            val origin = it.request().getHeader("Origin")
            if (origin != null && origin.isNotEmpty() && it.request().method() == HttpMethod.OPTIONS) {
                it.removeCookie(cookieName)
                it.success("")
            } else {
                it.next()
            }
        }

        router.route().handler(BodyHandler.create())

        router.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT));
        router.route("/reader3/*").handler {
            logger.info("{} {}", it.request().rawMethod(), URLDecoder.decode(it.request().absoluteURI(), "UTF-8"))
            if (!it.request().rawMethod().equals("PUT") && (it.fileUploads() == null || it.fileUploads()
                    .isEmpty()) && it.bodyAsString != null && (it.bodyAsString.isNotEmpty() && it.bodyAsString.length < 1000)
            ) {
                logger.info("Request body: {}", it.bodyAsString)
            }
            it.next()
        }

        router.get("/health").handler { it.success("ok!") }

        initRouter(router)

        router.route().last().failureHandler { ctx ->
            ctx.error(ctx.failure())
        }

        logger.info("port: {}", port)
        val wxHandleDataController = WxHandleDataController()
        vertx.createHttpServer()
            .websocketHandler { ws ->
                bookInfoByJson.clear()
                // 创建了链接
                println("Client connected: ${ws.remoteAddress()}")

                val url =
                    "http://localhost:8080" + ws.uri()
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .build()
                fetchData(client, request)

                var count = 1
                ws.handler { buffer ->
                    println("Received data: ${buffer.toString()}")
                    if (buffer.toString() == "searchBookMultiSSE") {
                        println("第${count++}次查询")
                        if (bookInfoByJson.isNotEmpty()) {
                            wxHandleDataController.searchBookMultiSSE(ws)

                            bookInfoByJson.clear()
                            ws.close()
                            return@handler
                        }
                    }
                }

                ws.endHandler {
                    println("Client disconnected: ${ws.remoteAddress()}")
                }

                ws.closeHandler {
                    println("WebSocket closed")
                }
            }
            .requestHandler(router)
            .exceptionHandler { error ->
                onException(error)
            }
            .listen(port) { res ->
                if (res.succeeded()) {
                    logger.info("Server running at: http://localhost:{}", port)
                    logger.info("Web reader running at: http://localhost:{}", port)
                    logger.info("WebSocket running at: http://localhost:{}", port)
                    started()
                } else {
                    onStartError()
                }
            }
    }

    // 创建一个协程函数来处理请求
    private fun fetchData(client: OkHttpClient, request: Request) {
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                // 处理失败
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                // 回调的方法执行在子线程。
                if (response.isSuccessful) {
                    println("获取数据成功了")
                    println("response.code()=${response.code}")
//                    writeTextMessage = response.body?.string().toString()
//                    val body = response.body
//                    println("writeTextMessage=${body?.string().toString()}\n")
//                            ws.writeTextMessage(response.body?.string())
//                    ws.close()
                }
            }
        })
    }

    abstract suspend fun initRouter(router: Router);

    open fun onException(error: Throwable) {
        logger.error("vertx exception: {}", error)
    }

    open fun onStartError() {
    }

    open fun started() {

    }

    open fun onHandlerError(ctx: RoutingContext, error: Exception) {
        logger.error("Error: {}", error)
        ctx.error(error)
    }

    /**
     * An extension method for simplifying coroutines usage with Vert.x Web routers
     */
    fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Any) {
        handler { ctx ->
            val job = launch(Dispatchers.IO) {
                try {
                    ctx.success(fn(ctx))
//                    fn(ctx)
                } catch (e: Exception) {
                    onHandlerError(ctx, e)
                }
            }
        }
    }

    fun Route.coroutineHandlerWithoutRes(fn: suspend (RoutingContext) -> Any) {
        handler { ctx ->
            val job = launch(Dispatchers.IO) {
                try {
                    fn(ctx)
                } catch (e: Exception) {
                    onHandlerError(ctx, e)
                }
            }
        }
    }
}
