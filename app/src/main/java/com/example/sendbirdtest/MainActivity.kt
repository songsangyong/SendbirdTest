package com.example.sendbirdtest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import com.example.sendbirdtest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    val mBinding by lazy { ActivityMainBinding.inflate(LayoutInflater.from(this)) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        val app = application as App
        app.initCallback { isSuccess ->
            if(isSuccess){

            }
        }

    }
}