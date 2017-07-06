package com.example.vidhi.firebasechatapp

import android.content.Intent
import android.support.v4.app.TaskStackBuilder
import android.support.v7.app.NotificationCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.FirebaseMessagingService
import android.app.PendingIntent
import android.content.Context.NOTIFICATION_SERVICE
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.provider.Settings
import android.util.Log
import android.graphics.BitmapFactory
import android.webkit.URLUtil
import com.livinglifetechway.k4kotlin.orZero
import java.net.HttpURLConnection
import java.net.URL


/**
 * Created by vidhi on 7/4/2017.
 */
class MessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        val TAG = "Message"
        val title = remoteMessage?.data?.get("title")
        val body = remoteMessage?.data?.get("body")
        val myUrl = remoteMessage?.data?.get("icon")
        val mId = remoteMessage?.data?.get("id")?.hashCode()
        var imgBitmap: Bitmap? = null
        Log.d(TAG , "onMessageReceived: $title")
        Log.d(TAG , "onMessageReceived: $body")

        fun getBitmapFromUrl(url: String?): Bitmap? {
            var bitmap: Bitmap? = null
            val url = URL(url)
            val connection = url.openConnection() as HttpURLConnection
            connection.setDoInput(true)
            connection.connect()
            val input = connection.getInputStream()
            bitmap = BitmapFactory.decodeStream(input)
            return bitmap
        }

        try {
            imgBitmap = getBitmapFromUrl(myUrl)
        } catch (e: Exception) {
            imgBitmap = BitmapFactory.decodeResource(applicationContext.getResources() ,
                    R.drawable.ic_perm_identity_black_48dp);
        }


        val mBuilder = NotificationCompat.Builder(this@MessagingService)
                .setSmallIcon(R.drawable.cast_ic_notification_small_icon)
                .setLargeIcon(imgBitmap)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setContentText(body)
        val intent = Intent(this@MessagingService , HomeActivity::class.java)

        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addParentStack(HomeActivity::class.java)
        stackBuilder.addNextIntent(intent)
        val resultPendingIntent = stackBuilder.getPendingIntent(
                0 ,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        mBuilder.setContentIntent(resultPendingIntent);
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(mId.orZero() as Int , mBuilder.build())
    }
}
