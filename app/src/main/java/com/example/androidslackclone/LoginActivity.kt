package com.example.androidslackclone

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.authentication.storage.SecureCredentialsManager
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.auth0.android.provider.AuthCallback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.auth0.android.authentication.storage.CredentialsManagerException
import com.auth0.android.callback.BaseCallback


class LoginActivity : AppCompatActivity() {
	
	lateinit var auth0: Auth0
	lateinit var credentialsManager: SecureCredentialsManager
	lateinit var newActivityIntent : Intent
	
	private val webCallback = object : AuthCallback {
		override fun onFailure(dialog: Dialog) {
			runOnUiThread { dialog.show() }
		}
		
		override fun onFailure(exception: AuthenticationException) {
			runOnUiThread { Toast.makeText(this@LoginActivity, "Log In - Error Occurred", Toast.LENGTH_SHORT).show() }
		}
		
		override fun onSuccess(credentials: Credentials) {
			runOnUiThread { Toast.makeText(this@LoginActivity, "Log In - Success", Toast.LENGTH_SHORT).show() }
			credentialsManager.saveCredentials(credentials)
			startNextActivity(credentials)
		}
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		//setContentView(R.layout.activity_login)
		
		newActivityIntent = Intent(this@LoginActivity, MainActivity::class.java)
		
		auth0 = Auth0(this)
		auth0.isLoggingEnabled = true
		auth0.isOIDCConformant = true
		credentialsManager = SecureCredentialsManager(this, AuthenticationAPIClient(auth0),
				SharedPreferencesStorage(this))
		
		if (intent.getBooleanExtra("clear_credentials", false)) {
			credentialsManager.clearCredentials()
		}
		
		
		if (!credentialsManager.hasValidCredentials()) {
			//Make user login
			WebAuthProvider.init(auth0)
					.withScheme("demo")
					.withAudience(String.format("https://%s/api/v2/", getString(R.string.com_auth0_domain)))
					.withScope("openid profile email offline_access read:current_user update:current_user_metadata")
					.start(this, webCallback)
		} else {
			credentialsManager.getCredentials(object : BaseCallback<Credentials, CredentialsManagerException> {
				override fun onSuccess(credentials: Credentials) {
					startNextActivity(credentials)
				}
				
				override fun onFailure(error: CredentialsManagerException) {
					//Authentication cancelled by the user. Exit the app
					finish()
				}
			})
		}
		
		
	}
	
	private fun startNextActivity(credentials: Credentials) {
		newActivityIntent.putExtra("access_token", credentials.accessToken)
		newActivityIntent.putExtra("id_token", credentials.idToken)
		startActivity(newActivityIntent)
		finish()
	}
	
}
