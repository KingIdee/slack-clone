# Android Slack Clone
An Android app that mimics the popular work communication tool - Slack.

## Prerequisites

You need the following installed:

* [Android Studio](https://developer.android.com/studio/index)

You need an account with the following services:

* [Auth0](https://auth0.com)
* [Webtask](https://webtask.io/make)
* [ChatKit](https://dash.pusher.com/chatkit)


## Getting started

To start with, clone the repository and open the project in Android Studio.

### Setting up an Auth0 client app
* Create an account at [Auth0](https://auth0.com) and login to your dashboard.
* Create a new Android native client in Auth0.
* Copy out the client_id and domain_name details from your Auth0 app and replace it with the holders in the `strings.xml` file in the Android app. This file is located at - `/{NameOfProject}/app/src/main/res/values/strings.xml`.
* Next, configure your login page. Head over to the Hosted Pages section of your Dashboard. At the top of your the code editor, click the menu entitled Default Templates and pick Lock (Passwordless) from the list.
* Add a callback to your Auth0 application. It should be something like this: `demo://AUTH_DOMAIN_NAME/android/com.example.androidslackclone/callback`


### Setting up the backend

In this sample, the backend is built with Node.js and it is hosted on [WebTask](https://webtask.io). Create a new account on Webtask, and a new server instance, say - `slack-clone`. Paste the code from the `server.js` file into the new Webtask instance created. 

Your Webtask account has a unique `BASE_URL` while instances you create are just appended as endpoints to it. Copy the `BASE_URL` of your Webtask app (which is something like this: - `https://wt-25e341bb2fca3ab10c862fb71cda965c-0.sandbox.auth0-extend.com/`) to the `BASE_URL` variable in the `SlackCloneApp` Kotlin class in your Android application.

### Setting up a ChatKit app
* Create a [ChatKit](https://dash.pusher.com/chatkit) account and login to your dashboard.
* Create a new ChatKit instance.
* Copy the instance locator value to the `INSTANCE_LOCATOR` variable in the `SlackCloneApp` Kotlin class.
* Replace the `CHATKIT_INSTANCE_LOCATOR` holder in your server instance with the instance locator value too.
* Copy the secret key value from your ChatKit instance to the `CHATKIT_SECRET_KEY` holder in your server instance.
* Create a new user named `admin` and create a new room named `general`, this is to make things easier on the client end.

