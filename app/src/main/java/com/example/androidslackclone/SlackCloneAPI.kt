package com.example.androidslackclone

import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Created by Idorenyin Obong on 13/10/2018
 *
 */
interface SlackCloneAPI {

	@POST("/slack-clone/user")
	fun createUser(@Header("Authorization") authorization: String,
								 @Body body:RequestBody): Call<ResponseBody>
	
}