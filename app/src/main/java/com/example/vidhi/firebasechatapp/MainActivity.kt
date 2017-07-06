package com.example.vidhi.firebasechatapp

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.ResultCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import java.util.*

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)


        ////.....generate device token
        val tkn = FirebaseInstanceId.getInstance().getToken()
        Log.d("###Token Generated", "Token [" + tkn + "]")

        val userToken = FirebaseDatabase.getInstance().getReference("User-Token")
        userToken.run { child(auth.currentUser?.uid).child(tkn).setValue(true) }

        if (auth.currentUser != null) {
            // already signed in
            val intent = Intent(this@MainActivity , HomeActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // not signed in
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setProviders(Arrays.asList<AuthUI.IdpConfig>(AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build()))
                            .setLogo(R.mipmap.ic_launcher_round)
                            .build() ,
                    RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int , resultCode: Int , data: Intent) {
        super.onActivityResult(requestCode , resultCode , data)
        if (resultCode == ResultCodes.OK) {

            var email: String? = auth.currentUser!!.email
            var name: String? = auth.currentUser!!.displayName
            var id: String? = auth.currentUser!!.uid
            var photoUrl : String? = auth.currentUser!!.photoUrl.toString()

            val user = User(name , email , photoUrl)
            val dbRefUser = FirebaseDatabase.getInstance().getReference("User")
            dbRefUser.run { child(id).setValue(user) }

            finish()
        }
    }

    companion object {

        private val RC_SIGN_IN = 123
    }
}
