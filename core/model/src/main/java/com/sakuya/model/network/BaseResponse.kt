package com.sakuya.model.network

import com.sakuya.model.exception.ApiException

data class BaseResponse<T>(
    val code:Int,
    val message: String,
    val data: T?
){
    fun isSuccess(): Boolean = code == 200

    fun toResult(): Result<T>{
        return if(isSuccess() && data != null) {
            Result.success(data)
        }else {
            Result.failure(ApiException(code, message))
            
        }
    }
}
