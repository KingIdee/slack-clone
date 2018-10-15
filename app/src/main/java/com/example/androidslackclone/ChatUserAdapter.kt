package com.example.androidslackclone

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * Created by Idorenyin Obong on 15/10/2018
 *
 */

class ChatUserAdapter : RecyclerView.Adapter<ChatUserAdapter.ViewHolder>() {
	
	private val chatuserList: ArrayList<ChatKitUser> = ArrayList()
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(
			LayoutInflater.from(parent.context)
				.inflate(android.R.layout.simple_list_item_1, parent, false))
	}
	
	override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(chatuserList.get(position))
	
	override fun getItemCount(): Int = chatuserList.size
	
	fun addItem(item: ChatKitUser){
		chatuserList.add(item)
		notifyDataSetChanged()
	}
	
	fun setList(items: List<ChatKitUser>){
		chatuserList.addAll(items)
		notifyDataSetChanged()
	}
	
	inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		
		private val userName: TextView = itemView.findViewById(android.R.id.text1)
		private val userMessage: TextView = itemView.findViewById(R.id.userMessage)
		
		fun bind(item: ChatKitUser) = with(itemView) {
			userName.text = item.name
		}
		
	}
	
	
}