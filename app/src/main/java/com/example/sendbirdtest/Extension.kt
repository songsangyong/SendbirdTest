package com.example.sendbirdtest

import java.text.SimpleDateFormat
import java.util.Date

object Extension {
    fun Long.convertLoginToTime():String{
        val date = Date(this)
        val format = SimpleDateFormat("HH:mm")
        return format.format(date)
    }
}