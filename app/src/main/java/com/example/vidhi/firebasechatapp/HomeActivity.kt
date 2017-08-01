package com.example.vidhi.firebasechatapp

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.widget.Toast
import com.bumptech.glide.Glide
import com.makeramen.roundedimageview.RoundedImageView

//public inline fun <T , R> with2(receiver: T , block: T.() -> R): R = receiver.block()
class HomeActivity : AppCompatActivity() {

    var name: String? = null
    var photoUrl: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)


        val dbRefUser = FirebaseDatabase.getInstance().getReference("User")
        val auth = FirebaseAuth.getInstance()
        val dbRefUserThread = FirebaseDatabase.getInstance().getReference("User-Thread").child(auth.currentUser?.uid)

        val mRecyclerView = findViewById(R.id.rec_frd_list) as RecyclerView
        val layoutManager = LinearLayoutManager(this)
        mRecyclerView.layoutManager = layoutManager


        val btnFab = findViewById(R.id.fab) as FloatingActionButton
        btnFab.setOnClickListener {
            val intent = Intent(this@HomeActivity , UserActivity::class.java)
            startActivity(intent)
        }

        val mAdapter = object : FirebaseRecyclerAdapter<Boolean , MyHolder>(Boolean::class.java ,
                R.layout.list_user , MyHolder::class.java , dbRefUserThread) {
            public override fun populateViewHolder(myholder: MyHolder , thread: Boolean , position: Int) {
                val currentThread = getRef(myholder.adapterPosition).key

                //.........display threads on home activity
//                myholder.tvUser.setText(currentThread)

                //.....display name on home activity

                val dbRefThreadUser = FirebaseDatabase.getInstance().getReference("Thread-Member").child(currentThread)
                dbRefThreadUser.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError?) {

                    }

                    override fun onDataChange(p0: DataSnapshot?) {
                        for (childSnapshot in p0?.children!!) {
                            val usersId = childSnapshot?.key
                            if (!usersId!!.equals(auth.currentUser?.uid)) {
                                val dbRefSecUser = FirebaseDatabase.getInstance().getReference("User").child(usersId)
                                dbRefSecUser.addValueEventListener(object : ValueEventListener {
                                    override fun onCancelled(p0: DatabaseError?) {

                                    }

                                    override fun onDataChange(p0: DataSnapshot?) {
                                        val userName: User? = p0?.getValue(User::class.java)
                                        name = userName?.name
                                        photoUrl = userName?.photoUrl
                                        if (userName?.online.equals("true")) {
                                            myholder.ivOnline.visibility = View.VISIBLE
                                        } else {
                                            myholder.ivOnline.visibility = View.GONE
                                        }
                                        val imageView = myholder.ivUser
                                        Glide.with(applicationContext)
                                                .load(photoUrl)
                                                .placeholder(R.drawable.ic_perm_identity_black_48dp)
                                                .into(imageView)
                                        println(photoUrl)
                                        println(name)
                                        myholder.tvUser.setText(name)

                                        ////....get notification user id set
                                        var userIdSet: Set<String?>? = null
                                        val sharedPref: SharedPreferences = PreferenceManager
                                                .getDefaultSharedPreferences(applicationContext)
                                        userIdSet = sharedPref.getStringSet("userIdSet" ,
                                                userIdSet)

                                        if (userIdSet!=null) {
                                            if (userIdSet.contains(usersId)) {
                                                myholder.ivNewMsg.visibility = View.VISIBLE
//
                                            }
                                        }



                                        myholder.tvUser.setOnClickListener {

                                            val intent = Intent(this@HomeActivity , ChatActivity::class.java)
                                            intent.putExtra("threadId" , currentThread)
                                            intent.putExtra("uName" , myholder.tvUser.text)
                                            intent.putExtra("photoUrl" , userName?.photoUrl)
                                            intent.putExtra("userId" , usersId)
                                            startActivity(intent)
                                        }
                                    }

                                })

                            }
                        }


                    }

                })

                myholder.tvUser.setOnLongClickListener({
                    val subdialog: AlertDialog.Builder = AlertDialog.Builder(this@HomeActivity)
                    subdialog.setTitle("Delete chat with " + name + " ?")
                            .setPositiveButton("Yes" , { update: DialogInterface? , which: Int ->

                                val dlt = FirebaseDatabase.getInstance()
                                        .getReference("User-Thread")
                                        .child(auth.currentUser?.uid)
                                        .child(currentThread).removeValue()
                                println(dlt)
                            })
                            .setNegativeButton("No" , { updatee: DialogInterface? , which: Int -> })

                    val display = subdialog
                    display.create()
                    display.show()
                    false
                })


            }
        }
        mRecyclerView.adapter = mAdapter

    }

    class MyHolder(v: View) : RecyclerView.ViewHolder(v) {

        internal var tvUser: TextView
        internal var ivUser: RoundedImageView
        internal var ivOnline: ImageView
        internal var ivNewMsg: ImageView

        init {
            tvUser = v.findViewById(R.id.tv_user) as TextView
            ivUser = v.findViewById(R.id.iv_user) as RoundedImageView
            ivOnline = v.findViewById(R.id.iv_online) as ImageView
            ivNewMsg = v.findViewById(R.id.iv_new_msg) as ImageView
        }

    }
}


