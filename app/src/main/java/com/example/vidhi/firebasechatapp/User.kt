package com.example.vidhi.firebasechatapp

import android.net.Uri

/**
 * Created by vidhi on 6/7/2017.
 */
data class User(var name: String?="" , var email : String?="" , var photoUrl : String?="" , var userKey : String? = "",
                var online : String? = "", var lastSeen: Long=-1)