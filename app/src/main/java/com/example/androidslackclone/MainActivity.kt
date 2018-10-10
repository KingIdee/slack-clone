package com.example.androidslackclone

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.util.Log
import com.pusher.chatkit.CurrentUser
import com.pusher.platform.network.wait
import com.pusher.util.Result
import elements.Error
import android.widget.Toast
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.management.ManagementException
import com.auth0.android.result.UserProfile
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.BaseCallback
import com.auth0.android.management.UsersAPIClient




class MainActivity : AppCompatActivity() {
	
	
	private lateinit var authenticationAPIClient : AuthenticationAPIClient
	lateinit var usersClient: UsersAPIClient
	
	
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
		
		connectChatKit()
		
		
		
		
		
	}
	
	private fun connectChatKit() {
		SlackCloneApp.chatManager.connect().wait().let<Result<CurrentUser, Error>, Unit> { result ->
			when (result) { // Result<CurrentUser, Error>
				is Result.Success -> {
					Log.d("Authentication", result.value.toString())
					SlackCloneApp.currentUser = result.value
					loadRooms()
				}
				is Result.Failure -> Log.d("Authentication", result.error.toString())
			}
		}
	}
	
	private fun loadRooms() {
	
	}
	
	
	private fun getProfile(accessToken: String) {
		authenticationAPIClient.userInfo(accessToken)
				.start(object : BaseCallback<UserProfile, AuthenticationException> {
					override fun onSuccess(userinfo: UserProfile) {
						usersClient.getProfile(userinfo.id)
								.start(object : BaseCallback<UserProfile, ManagementException> {
									override fun onSuccess(profile: UserProfile) {
										SlackCloneApp.userEmail = profile.email
										/*userProfile = profile
										runOnUiThread { refreshScreenInformation() }*/
									}
									
									override fun onFailure(error: ManagementException) {
										runOnUiThread { Toast.makeText(this@MainActivity, "User Profile Request Failed", Toast.LENGTH_SHORT).show() }
									}
								})
					}
					
					override fun onFailure(error: AuthenticationException) {
						runOnUiThread { Toast.makeText(this@MainActivity, "User Info Request Failed", Toast.LENGTH_SHORT).show() }
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
	
}
