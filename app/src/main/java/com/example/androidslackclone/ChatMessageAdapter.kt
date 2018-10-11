package com.example.androidslackclone

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.pusher.chatkit.messages.Message

/**
 * Created by Idorenyin Obong on 11/10/2018
 *
 */

class ChatMessageAdapter : RecyclerView.Adapter<ChatMessageAdapter.ViewHolder>() {
	
	private var messageList  = ArrayList<Message>()
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(LayoutInflater.from(parent.context)
				.inflate(R.layout.list_row_chat, parent, false))
	}
	
	override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(messageList[position])
	
	override fun getItemCount(): Int = messageList.size
	
	fun addItem(item: Message){
		messageList.add(item)
		notifyDataSetChanged()
	}
	
	inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		
		private val userName: TextView = itemView.findViewById(R.id.userName)
		private val userMessage: TextView = itemView.findViewById(R.id.userMessage)
		
		fun bind(item: Message) = with(itemView) {
			Log.d("SlackClone","bind method called")
			userName.text = item.user!!.name
			userMessage.text = item.text
		}
		
	}
	
	
}