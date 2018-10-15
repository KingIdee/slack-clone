package com.example.androidslackclone

import com.google.gson.annotations.SerializedName

data class ChatKitUser(
	@SerializedName("id") val id: String,
	@SerializedName("name") val name: String,
	@SerializedName("created_at") val createdAt: String,
	@SerializedName("updated_at") val updatedAt: String
)