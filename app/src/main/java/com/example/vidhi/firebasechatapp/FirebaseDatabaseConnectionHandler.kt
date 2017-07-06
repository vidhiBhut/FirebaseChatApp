package com.example.vidhi.firebasechatapp

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * Created by vidhi on 6/27/2017.
 */
class FirebaseDatabaseConnectionHandler : Application.ActivityLifecycleCallbacks {

    var count: Int = 0
    val delayedTimeMillis: Long = 10000
    val mHandler: Handler = Handler()
    val auth = FirebaseAuth.getInstance()
    val dbRefUser = FirebaseDatabase.getInstance().getReference("User")
    override fun onActivityPaused(activity: Activity?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onActivityResumed(activity: Activity?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onActivityStarted(activity: Activity?) {
        count++
        if (count > 0) {
            FirebaseDatabase.getInstance().goOnline()

            if (auth.currentUser?.uid != null) {
                dbRefUser.run { child(auth.currentUser?.uid).child("online").setValue("true") }
                FirebaseDatabase.getInstance().getReference("User").child(auth.currentUser?.uid).child("lastSeen").removeValue()

            }
        }

    }

    override fun onActivityDestroyed(activity: Activity?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onActivitySaveInstanceState(activity: Activity? , outState: Bundle?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onActivityStopped(activity: Activity?) {
        count--
        if (count == 0) {

            mHandler.postDelayed({
                if (count == 0) {
                    FirebaseDatabase.getInstance().goOffline()
                }
            } , delayedTimeMillis)
            FirebaseDatabase.getInstance().getReference("User").child(auth.currentUser?.uid).child("online").removeValue()
            val cTime: Long = System.currentTimeMillis()
            if (auth.currentUser?.uid != null){
                dbRefUser.run { child(auth.currentUser?.uid).child("lastSeen").setValue(cTime) }
            }

        }
    }

    override fun onActivityCreated(activity: Activity? , savedInstanceState: Bundle?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}