package com.sakuya.model.exception

class ApiException(
    val code: Int,
    override val message: String,
    cause: Throwable? = null
) : Exception(message, cause){
    fun isUnauthorized(): Boolean = code == 401

    fun isServerError(): Boolean = code in 500..599
    
    fun isClientError(): Boolean = code in 400.. 499
    fun isNetworkError(): Boolean = code == -1
    companion object{
//        错误码
        const val CODE_NETWORK_ERROR = -1
        const val CODE_UNKNOWN_ERROR = -2
        const val CODE_PARSE_ERROR = -3
    }
// 工厂方法
    fun networkError(cause: Throwable? = null) : ApiException{
        return ApiException(CODE_NETWORK_ERROR,"网络错误", cause)
    }
    fun unknownError(cause: Throwable? = null) : ApiException{
        return ApiException(CODE_UNKNOWN_ERROR,"未知错误", cause)
    }
    fun parseError(cause: Throwable? = null): ApiException{
        return ApiException(CODE_PARSE_ERROR,"数据错误", cause)
    }
}