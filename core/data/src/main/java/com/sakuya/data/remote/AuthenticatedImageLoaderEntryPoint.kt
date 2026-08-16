package com.sakuya.data.remote

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient

/**
 * AuthenticatedImageLoaderEntryPoint.kt
 * 职责说明：向 Application 级 Coil ImageLoader 提供已安装认证拦截器的 OkHttpClient。
 * 执行流程：Coil 请求 Wenku8 封面 -> 复用此客户端附加 JWT -> 后端封面网关认证并代理图片字节。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthenticatedImageLoaderEntryPoint {
    fun okHttpClient(): OkHttpClient
}
