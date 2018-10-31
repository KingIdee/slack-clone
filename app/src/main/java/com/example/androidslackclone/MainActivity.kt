package com.example.androidslackclone

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
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
import com.pusher.chatkit.messages.Message
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomSubscriptionEvent
import com.pusher.chatkit.users.User
import com.pusher.platform.network.wait
import com.pusher.util.Result
import elements.Error
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
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
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), RoomsAdapter.RoomClickListener,
	ChatUserAdapter.UserClickedListener {
	
	private lateinit var authenticationAPIClient: AuthenticationAPIClient
	lateinit var usersClient: UsersAPIClient
	private val mAdapter = RoomsAdapter(this)
	private val chatAdapter = ChatMessageAdapter()
	private val chatUserAdapter = ChatUserAdapter(this)
	private var currentRoom:Room? = null
	private lateinit var accessToken :String
	
	private val slackCloneAPI:SlackCloneAPI by lazy {
		Retrofit.Builder()
			.baseUrl(SlackCloneApp.BASE_URL)
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
		
		
		val toggle = ActionBarDrawerToggle(
			this,
			drawer_layout,
			toolbar,
			R.string.navigation_drawer_open,
			R.string.navigation_drawer_close
		)
		drawer_layout.addDrawerListener(toggle)
		toggle.syncState()
		
		accessToken = intent.getStringExtra("access_token")
		
		
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
				currentRoom!!.id.apply {
					val sendMessageResult = SlackCloneApp.currentUser.sendMessage(currentRoom!!.id,editTextMessage.text.toString()).wait()
					
					when (sendMessageResult) { // Result<CurrentUser, Error>
						is Result.Success -> {
							editTextMessage.text.clear()
							Toast.makeText(this@MainActivity,"Message sent",Toast.LENGTH_SHORT).show()
							hideKeyboard()
						}
						is Result.Failure -> Log.d("SlackClone", (sendMessageResult as Result.Failure<Int, Error>).error.toString())
					}
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
		
			try {
				val result =  SlackCloneApp.chatManager.connect().wait()
				
				when (result) { // Result<CurrentUser, Error>
					is Result.Success -> {
						SlackCloneApp.currentUser = result.value
						Toast.makeText(this@MainActivity,"Finished connecting to ChatKit",Toast.LENGTH_SHORT).show()
						loadRooms()
						fetchUsers()
					}
					is Result.Failure -> {
						Toast.makeText(this@MainActivity,"Could not connect to ChatKit",Toast.LENGTH_SHORT).show()
						Log.d("Authentication", result.error.toString())
					}
				}
				
			} catch (e: Exception) {
				e.printStackTrace()
			}
			
		
	}
	
	private fun fetchUsers(){
		val authHeader = "Bearer $accessToken"
		slackCloneAPI.getUsers(authHeader).enqueue(object: Callback<List<User>> {
			override fun onFailure(call: Call<List<User>>, t: Throwable) {
				Log.d("SlackClone",t.message)
			}
			
			override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
				Log.d("SlackClone",response.body().toString())
				response.body()?.apply {
					for (user in this){
						if (user.id != SlackCloneApp.currentUser.id && user.name !="admin"){
							chatUserAdapter.addItem(user)
						}
					}
				}
				
			}
			
		})
	}
	
	private fun loadRooms() {
		
		val currentUserRooms = SlackCloneApp.currentUser.rooms as ArrayList<Room>
		val joinableRoomsResult =  SlackCloneApp.currentUser.getJoinableRooms().wait()
		
		val combinedList = ArrayList<Room>()
		combinedList.addAll(currentUserRooms)
		when(joinableRoomsResult){
			
			is Result.Success -> {
				Log.d("SlackCloneJoinable",joinableRoomsResult!!.toString())
				combinedList.addAll((joinableRoomsResult as Result.Success<List<Room>, Error>).value)
			}
			
		}
		//runOnUiThread {
			
			for (room in combinedList) {
				Log.d("SlackCloneRooms",room.name+"is"+room.isPrivate.toString())
				
				if (room.name == "general") {
					mAdapter.addItem(room)
					Log.d("SlackClone", "There is a channel called General")
					
					if (currentUserRooms.contains(room)) {
						fetchMessages(room)
						subscribeToRoom(room)
					} else {
						joinRoom(room)
					}
					
				}
			}
			
		//}
	
	}
	
	private fun joinRoom(room: Room) {
		
		val joinRoomResult = SlackCloneApp.currentUser.joinRoom(room).wait()
		
		when(joinRoomResult){
			
			is Result.Success -> {
				fetchMessages(room)
				subscribeToRoom(room)
			}
			
			is Result.Failure -> {
				showError("Could not join Room: "+room.name)
			}
			
		}
		
	}
	
	private fun subscribeToRoom(room: Room) {
		currentRoom = room
		supportActionBar!!.title = room.name
		SlackCloneApp.currentUser.subscribeToRoom(
			roomId = room.id,
			messageLimit = MESSAGE_LIMIT // Optional, 10 by default
		) { event ->
			when (event) {
				is RoomSubscriptionEvent.NewMessage -> {
					progressBarChat.visibility = View.GONE
					chatAdapter.addItem(event.message)
					hideError()
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
		
		var messagesResult:Result<List<Message>,Error>? = null
		
			messagesResult = SlackCloneApp.currentUser.fetchMessages(
				room.id,
				direction = Direction.OLDER_FIRST, // Optional, OLDER_FIRST by default
				limit = MESSAGE_LIMIT // Optional, 10 by default
			).wait()
		
		
		Log.d("Special",messagesResult.toString())
		
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
				Log.d("SpecialFailure", (messagesResult as Result.Failure<List<Message>, Error>).error.reason)
				
				Log.d("SlackClone", (messagesResult as Result.Failure<List<Message>, Error>).error.reason)
				showError((messagesResult as Result.Failure<List<Message>, Error>).error.reason)
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
		jsonObject.put("email",profile.email)
		jsonObject.put("name",profile.name)
		jsonObject.put("imageURL",profile.pictureURL)
		
		val body = RequestBody.create(
			MediaType.parse("application/json; charset=utf-8"),
			jsonObject.toString()
		)

		val authHeader = "Bearer $accessToken"

		slackCloneAPI.createUser(authHeader,body).enqueue(object:Callback<ResponseBody>{
			override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
				showError("Could not connect to ChatKit")
			}
			
			override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
				Log.d("SlackClone",response.errorBody().toString())
				Log.d("SlackClone",response.headers().toString())
				Log.d("SlackClone",response.code().toString())
				val res = response.body()!!.string()
				val jObject = JSONObject(res!!)
				try {
					if (jObject.getString("status") == "400") {
						Log.d("SlackClone", "User exists")
						connectChatKit()
					} else {
					}
				} catch (e:Exception){
					e.printStackTrace()
					// user created successfully
					Log.d("SlackClone","Catch Exception")
					connectChatKit()
				}
				
			}
			
		})
	}
	
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.menu_main, menu)
		return true
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_logout -> {
				logout()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}
	
	private fun logout() {
		val intent = Intent(this, LoginActivity::class.java)
		intent.putExtra("clear_credentials", true)
		startActivity(intent)
		finish()
	}
	
	private fun hideKeyboard() {
    val inputMethodManager = (getSystemService(Activity.INPUT_METHOD_SERVICE)) as InputMethodManager
    var view = currentFocus
    if (view == null) {
        view =  View(this)
		}
		inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
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
		
		val usersRoom = SlackCloneApp.currentUser.rooms
		var toJoinRoom:Room? =null
		
		for (room in usersRoom){
			if (room.name==privateRoomName){
				toJoinRoom = room
			}
		}
		
		drawer_layout.closeDrawers()
		progressBarChat.visibility = View.VISIBLE
		chatAdapter.clear()
		
		if (toJoinRoom!=null){
			subscribeToRoom(toJoinRoom)
			fetchMessages(toJoinRoom)
		} else {
			val createRoomResult = SlackCloneApp.currentUser.createRoom(privateRoomName,true,memberList).wait()
			
			when(createRoomResult){
				
				is Result.Success -> {
					Log.d("SlackCloneUserClicked", (createRoomResult as Result.Success<Room, Error>).value.name)
					currentRoom = (createRoomResult as Result.Success<Room, Error>).value
					subscribeToRoom((createRoomResult as Result.Success<Room, Error>).value)
					fetchMessages((createRoomResult as Result.Success<Room, Error>).value)
				}
				is Result.Failure -> {
					Log.d("SlackCloneUserClicked", (createRoomResult as Result.Failure<Room, Error>).error.reason)
					showError((createRoomResult as Result.Failure<Room, Error>).error.reason)
				}
				
			}
		}
		
	}
	
	override fun onRoomClicked(item: Room) {
		drawer_layout.closeDrawers()
		chatAdapter.clear()
		subscribeToRoom(item)
		fetchMessages(item)
	}
	
}
