package com.example.androidslackclone

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.pusher.chatkit.users.User

/**
 * Created by Idorenyin Obong on 15/10/2018
 *
 */

class ChatUserAdapter (val listener:UserClickedListener ): RecyclerView.Adapter<ChatUserAdapter.ViewHolder>() {
	
	private val chatUserList: ArrayList<User> = ArrayList()
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(
			LayoutInflater.from(parent.context)
				.inflate(android.R.layout.simple_list_item_1, parent, false))
	}
	
	override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(chatUserList.get(position))
	
	override fun getItemCount(): Int = chatUserList.size
	
	
	fun setList(items: List<User>){
		chatUserList.addAll(items)
		notifyDataSetChanged()
	}
	
	fun addItem(item:User){
		chatUserList.add(item)
		notifyDataSetChanged()
	}
	
	inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		
		private val userName: TextView = itemView.findViewById(android.R.id.text1)
		
		fun bind(item: User) = with(itemView) {
			userName.text = item.name
			itemView.setOnClickListener { listener.onUserClicked(item) }
		}
		
	}
	
	interface UserClickedListener{
		fun onUserClicked(user:User)
	}
	
	
}