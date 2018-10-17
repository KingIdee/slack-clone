package com.example.androidslackclone

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.pusher.chatkit.rooms.Room

/**
 * Created by Idorenyin Obong on 10/10/2018
 *
 */

class RoomsAdapter(val listener:RoomClickListener) : RecyclerView.Adapter<RoomsAdapter.ViewHolder>() {
	
	private var roomList  = ArrayList<Room>()
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(LayoutInflater.from(parent.context)
				.inflate(android.R.layout.simple_list_item_1, parent, false))
	}
	
	override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(roomList[position])
	
	override fun getItemCount(): Int = roomList.size
	
	fun addItem(item: Room){
		roomList.add(item)
		notifyDataSetChanged()
	}
	
	fun setList(newList: ArrayList<Room>){
		roomList = newList
		notifyDataSetChanged()
	}
	
	inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		
		private val roomName: TextView = itemView.findViewById(android.R.id.text1)
		
		fun bind(item: Room) = with(itemView) {
			roomName.text = item.name
			this.setOnClickListener { listener.onRoomClicked(item) }
		}
		
	}
	
	interface RoomClickListener{
		fun onRoomClicked(item:Room)
	}
	
}