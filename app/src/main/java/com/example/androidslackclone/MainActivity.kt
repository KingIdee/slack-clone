package com.example.androidslackclone

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.BaseCallback
import com.auth0.android.management.ManagementException
import com.auth0.android.management.UsersAPIClient
import com.auth0.android.result.UserProfile
import com.pusher.chatkit.CurrentUser
import com.pusher.chatkit.messages.Direction
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomSubscriptionEvent
import com.pusher.chatkit.users.User
import com.pusher.platform.network.wait
import com.pusher.util.Result
import elements.Error
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory


class MainActivity : AppCompatActivity(), RoomsAdapter.RoomClickListener,
	ChatUserAdapter.UserClickedListener {
	
	
	private lateinit var authenticationAPIClient: AuthenticationAPIClient
	lateinit var usersClient: UsersAPIClient
	private val mAdapter = RoomsAdapter(this)
	private val chatAdapter = ChatMessageAdapter()
	private val chatUserAdapter = ChatUserAdapter(this)
	lateinit var currentRoom:Room
	
	private val slackCloneAPI:SlackCloneAPI by lazy {
		Retrofit.Builder()
			.baseUrl("https://wt-25e341bb2fca3ab10c862fb71cda965c-0.sandbox.auth0-extend.com/")
			.addConverterFactory(ScalarsConverterFactory.create())
			.addConverterFactory(GsonConverterFactory.create())
			.client(OkHttpClient.Builder().build())
			.build()
			.create(SlackCloneAPI::class.java)
	}
	
	companion object {
		private const val MESSAGE_LIMIT = 100
	}
	
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		setSupportActionBar(toolbar)
		
		
		val accessToken = intent.getStringExtra("access_token")
		
		
		val auth0 = Auth0(this)
		auth0.isOIDCConformant = true
		auth0.isLoggingEnabled = true
		authenticationAPIClient = AuthenticationAPIClient(auth0)
		
		
		
		usersClient = UsersAPIClient(auth0, accessToken)
		getProfile(accessToken)
		
		setupRecyclerView()
		setupClickListeners()
		
	}
	
	private fun setupClickListeners() {
		
		sendMessage.setOnClickListener {
			if (editTextMessage.text.isNotEmpty()){
				val sendMessageResult = SlackCloneApp.currentUser.sendMessage(currentRoom.id,editTextMessage.text.toString()).wait()
				when (sendMessageResult) { // Result<CurrentUser, Error>
					is Result.Success -> {
						editTextMessage.text.clear()
						Toast.makeText(this,"Message sent",Toast.LENGTH_SHORT).show()
					}
					is Result.Failure -> Log.d("SlackClone", sendMessageResult.error.toString())
				}
			}
		}
		
	}
	
	private fun setupRecyclerView() {
		
		with(recyclerViewRooms) {
			layoutManager = LinearLayoutManager(this@MainActivity)
			adapter = mAdapter
		}
		
		with(chatRecyclerView) {
			layoutManager = LinearLayoutManager(this@MainActivity)
			adapter = chatAdapter
		}
		
		with(recyclerViewMembers){
			layoutManager = LinearLayoutManager(this@MainActivity)
			adapter = chatUserAdapter
		}
		
	}
	
	private fun connectChatKit() {
		val result: Result<CurrentUser, Error>
		
		try {
			result = SlackCloneApp.chatManager.connect().wait()
			when (result) { // Result<CurrentUser, Error>
				is Result.Success -> {
					SlackCloneApp.currentUser = result.value
					loadRooms()
					val fetchUsersResult = SlackCloneApp.currentUser.users.wait()
					
					when(fetchUsersResult){
						
						is Result.Success -> {
							chatUserAdapter.setList(fetchUsersResult.value)
							
						}
						
						is Result.Failure -> {
							Log.d("SlackClone",fetchUsersResult.error.reason)
						}
						
					}
					
				}
				is Result.Failure -> {
					Toast.makeText(this,"Could not connect to ChatKit",Toast.LENGTH_SHORT).show()
					Log.d("Authentication", result.error.toString())
				}
			}
			
		} catch (e: Exception) {
			e.printStackTrace()
		}
		
	}
	
	private fun loadRooms() {
		
		val roomsResult = SlackCloneApp.currentUser.rooms
		runOnUiThread {
			
			for (room in roomsResult) {
				if (room.name == "General") {
					Log.d("SlackClone", "There is a channel called General")
					
					/*if (SlackCloneApp.currentUser.isSubscribedToRoom(room)){
						Log.d("SlackClone","User had subscribed before")
						fetchMessages(room)
					} else {
						Log.d("SlackClone","New time subscription")
						subscribeToRoom(room)
					}*/
					fetchMessages(room)
					subscribeToRoom(room)
					
				}
			}
			
			mAdapter.setList(roomsResult as ArrayList<Room>)
		}
		/*when (roomsResult) {
			
			is Result.Success -> {
				runOnUiThread {
					Log.d("Tag",roomsResult.value.toString())
					Log.d("Tag",roomsResult.value.size.toString())
					mAdapter.setList(roomsResult.value as ArrayList<Room>)
				}
				
			}
			
			is Result.Failure -> {
			
			}
			
		}*/
	}
	
	private fun subscribeToRoom(room: Room) {
		progressBarChat.visibility = View.VISIBLE
		SlackCloneApp.currentUser.subscribeToRoom(
			roomId = room.id,
			messageLimit = MESSAGE_LIMIT // Optional, 10 by default
		) { event ->
			when (event) {
				is RoomSubscriptionEvent.NewMessage -> {
					progressBarChat.visibility = View.GONE
					chatAdapter.addItem(event.message)
					Log.d("SlackClone", event.message.text)
				}
				is RoomSubscriptionEvent.ErrorOccurred -> {
					Log.d("SlackClone", event.error.reason)
					Toast.makeText(this,"Error trying to subscribe to Room",
						Toast.LENGTH_SHORT).show()
				}
			}
		}
		
	}
	
	private fun fetchMessages(room: Room) {
		
		val messagesResult = SlackCloneApp.currentUser.fetchMessages(
			room.id,
			direction = Direction.OLDER_FIRST, // Optional, OLDER_FIRST by default
			limit = MESSAGE_LIMIT // Optional, 10 by default
		).wait()
		
		when(messagesResult) {
			is Result.Success -> {
				progressBarChat.visibility = View.GONE
				if (messagesResult.value.isNotEmpty()) {
					chatAdapter.setList(messagesResult.value)
					hideError()
				} else {
					showError("No chat history found")
				}
			}
			is Result.Failure -> {
				Log.d("SlackClone",messagesResult.error.reason)
				showError(messagesResult.error.reason)
			}
		}
		
	}
	
	private fun showError(errorMessage:String){
		message.text = errorMessage
		message.visibility = View.VISIBLE
		progressBarChat.visibility = View.GONE
	}
	
	private fun hideError(){
		message.visibility = View.GONE
	}

	private fun getProfile(accessToken: String) {
		authenticationAPIClient.userInfo(accessToken)
		  .start(object : BaseCallback<UserProfile, AuthenticationException> {
			  override fun onSuccess(userinfo: UserProfile) {
				  Log.d("SlackClone", "First onSuccess called")
				  
				  usersClient.getProfile(userinfo.id)
					.start(object : BaseCallback<UserProfile, ManagementException> {
						override fun onSuccess(profile: UserProfile) {
							Log.d("SlackClone", "Time to connect to ChatKit")
							SlackCloneApp.userEmail = profile.email
							//connectChatKit()
							
							createUser(profile)
							
						}
						
						override fun onFailure(error: ManagementException) {
							error.printStackTrace()
							runOnUiThread { Toast.makeText(this@MainActivity, "User Profile Request Failed",
								Toast.LENGTH_SHORT).show() }
						}
					})
			  }
			  
			  override fun onFailure(error: AuthenticationException) {
					error.printStackTrace()
					runOnUiThread { Toast.makeText(this@MainActivity, "User Info Request Failed", Toast.LENGTH_SHORT).show() }
			  }
		  })
	}
	
	private fun createUser(profile: UserProfile) {
		val jsonObject = JSONObject()
		jsonObject.put("email",profile.email+"01")
		jsonObject.put("name",profile.name)
		jsonObject.put("imageURL",profile.pictureURL)
		
		val body = RequestBody.create(
			MediaType.parse("application/json; charset=utf-8"),
			jsonObject.toString()
		)
		
		slackCloneAPI.createUser("",body).enqueue(object:Callback<ResponseBody>{
			override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
				showError("Could not connect to ChatKit")
			}
			
			override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
				val res = response.body()!!.string()
				val jObject = JSONObject(res!!)
				try {
					if (jObject.getString("status").equals("400")) {
						Log.d("SlackClone", "User exists")
						connectChatKit()
					} else {
					}
				} catch (e:Exception){
					e.printStackTrace()
					Log.d("SlackClone","Catch Exception")
					connectChatKit()
				}
				
			}
			
		})
	}
	
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		// Inflate the menu; this adds items to the action bar if it is present.
		menuInflater.inflate(R.menu.menu_main, menu)
		return true
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return when (item.itemId) {
			R.id.action_settings -> true
			else -> super.onOptionsItemSelected(item)
		}
	}
	
	private fun logout() {
		val intent = Intent(this, LoginActivity::class.java)
		intent.putExtra("clear_credentials", true)
		startActivity(intent)
		finish()
	}
	
	override fun onUserClicked(user: User) {
		
		val privateRoomName = if (user.hashCode()>SlackCloneApp.currentUser.hashCode()){
			user.id+"_"+SlackCloneApp.currentUser.id
		} else {
			SlackCloneApp.currentUser.id+"_"+user.id
		}
		
		Log.d("SlackCloneUserClicked",privateRoomName)
		
		val memberList = ArrayList<String>()
		memberList.add(user.id)
		
		val createRoomResult = SlackCloneApp.currentUser.createRoom(privateRoomName,true,memberList).wait()
		
		when(createRoomResult){
			
			is Result.Success -> {
				Log.d("SlackCloneUserClicked",createRoomResult.value.name)
				chatAdapter.clear()
				subscribeToRoom(createRoomResult.value)
				fetchMessages(createRoomResult.value)
			}
			is Result.Failure -> {
				Log.d("SlackCloneUserClicked",createRoomResult.error.reason)
			}
		
		}
		
	}
	
	override fun onRoomClicked(item: Room) {
		chatAdapter.clear()
		subscribeToRoom(item)
		fetchMessages(item)
	}
	
}
