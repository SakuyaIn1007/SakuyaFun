package com.sakuya.data.content

import android.content.Context
import com.google.gson.Gson
import com.sakuya.data.BuildConfig
import com.sakuya.data.remote.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * 内容接口专用的带缓存客户端，与全业务共用的 OkHttpClient 区分开。
 *
 * 必须是白名单：OkHttp 缓存键只含 URL、不含 Authorization，且后端未下发 Vary，
 * 若把缓存挂在共享客户端上，聊天、通知等个人数据会按 URL 落盘并在切换账号后串号。
 * 只在内容客户端上启用缓存，则未列入的接口天然不会被缓存。
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ContentCacheClient

/** 在应用级 Retrofit 上创建唯一 ContentApiService，避免 Feature 重复声明同一后端协议。 */
@Module
@InstallIn(SingletonComponent::class)
object ContentNetworkModule {

    /**
     * 单本全文可达数 MB，100 MiB 约可容纳二十本。
     * 位于 cacheDir，系统可随时回收，因此这里的缓存只是尽力而为，不构成离线保证。
     */
    private const val CONTENT_CACHE_BYTES = 100L * 1024L * 1024L

    @Provides
    @Singleton
    fun provideContentCache(@ApplicationContext context: Context): Cache =
        Cache(File(context.cacheDir, "okhttp-content"), CONTENT_CACHE_BYTES)

    @Provides
    @Singleton
    @ContentCacheClient
    fun provideContentOkHttpClient(
        cache: Cache,
        authInterceptor: AuthInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .cache(cache)
        .connectTimeout(15, TimeUnit.SECONDS)
        // 与共享客户端保持一致：Wenku8 首次刷新需经过后端抓取，后端缓存建立后通常不会走到上限。
        .readTimeout(100, TimeUnit.SECONDS)
        .callTimeout(110, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        // 必须注册为网络拦截器，否则看到的是已过缓存的响应，无法让响应变为可缓存。
        .addNetworkInterceptor(ContentCacheInterceptor())
        .build()

    @Provides
    @Singleton
    @ContentCacheClient
    fun provideContentRetrofit(@ContentCacheClient client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideContentApiService(@ContentCacheClient retrofit: Retrofit): ContentApiService =
        retrofit.create(ContentApiService::class.java)
}
