package com.example.vidhi.firebasechatapp

/**
 * Created by vidhi on 6/6/2017.
 */

data class Chat(var msg: String = "" , var time: Long = -1 , var userId: String? = "" ,
                var imgPath: String? = "" , var audioPath: String? = "" , var contact: Contact? = null ,
                var location: Location? = null,
                var filePath : String? = "")

data class Contact(var name: String? = "" , var number: String? = "")
data class Location(var lat: Double? = null , var long: Double? = null)