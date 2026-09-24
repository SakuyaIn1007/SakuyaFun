package com.sakuya.data.content

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * ContentCacheInterceptor.kt
 * 职责说明：为内容接口响应合成缓存指令，弥补后端不下发 Cache-Control 的缺口。
 * 执行流程：请求放行 -> 按实际请求路径与状态码取策略 -> 覆盖写回响应头。
 *
 * 注册方式必须是 addNetworkInterceptor：应用拦截器看到的是已过缓存的响应，
 * 无法让响应本身变为可缓存。
 */
class ContentCacheInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        // 以网络层实际请求为准：重定向后 response.request 才是最终命中的 URL。
        val path = response.request.url.encodedPath
        return response.newBuilder()
            .header("Cache-Control", ContentCachePolicy.cacheControlFor(path, response.code))
            .build()
    }
}
