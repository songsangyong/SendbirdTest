package com.example.sendbirdtest

import android.app.Application
import android.widget.Toast
import com.sendbird.android.SendBird
import com.sendbird.android.SendBirdException
import com.sendbird.android.handlers.InitResultHandler

class App : Application() {
    val TAG = App::class.java.simpleName
    val APP_ID: String = "0E00F9ED-8CCF-4588-BACD-C4AEDD221BF7"
    var initSucceed: Boolean = false
    lateinit var mCallback:(Boolean)->Unit
    override fun onCreate() {
        super.onCreate()
        println("TEST")
        SendBird.init(APP_ID,applicationContext, true, object:InitResultHandler{
            override fun onInitFailed(e: SendBirdException) {
                mCallback.invoke(false)
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
            }

            override fun onInitSucceed() {
                mCallback.invoke(true)
            }

            override fun onMigrationStarted() {}

        })
    }

    fun initCallback(callback: ((Boolean)->Unit)){
        mCallback = callback
    }
}