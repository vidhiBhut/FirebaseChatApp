package com.example.vidhi.firebasechatapp

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.makeramen.roundedimageview.RoundedImageView


class UserActivity : AppCompatActivity() {

    var imgUrl : String? = null
    var mRecycler : RecyclerView? = null
    val listUser: MutableList<String?> = mutableListOf()
    val listUserImgUrl : MutableList<String?> = mutableListOf()
    val listSearchUser: MutableList<User> = mutableListOf()
    val userObject : MutableList<User> = mutableListOf()
    val keyList : MutableList<String> = mutableListOf()
    val searchKeyList : MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        ///....recycler view for search function
        val recUser = findViewById(R.id.rec_user_list) as RecyclerView
//        val imgSearch = findViewById(R.id.iv_search) as ImageView

        mRecycler = findViewById(R.id.rec_user) as RecyclerView
        mRecycler?.layoutManager = LinearLayoutManager(this)

        val toolbar = findViewById(R.id.toolbar) as android.support.v7.widget.Toolbar
        setSupportActionBar(toolbar)

        setSupportActionBar(toolbar)
        val ab = supportActionBar
        ab!!.setHomeAsUpIndicator(R.drawable.ic_navigate_before_white_48dp)
        ab.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        val layoutManager = LinearLayoutManager(this)
        recUser.layoutManager = layoutManager

        val dbRefUser = FirebaseDatabase.getInstance().getReference("User")
        val auth = FirebaseAuth.getInstance()

        dbRefUser.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {

            }

            override fun onDataChange(p0: DataSnapshot?) {
                for (i in p0!!.children) {
                    val user = i.getValue(User::class.java)
                    val uName = user?.name
                    val imgUrl = user?.photoUrl
                    val email = user?.email
                    val key : String = i.key
                    println("userActivity.....key..........."+key)
                    val newUser = User(uName,email,imgUrl,key)
                    userObject.add(newUser)



                }
                println("................" + listUser)
                mRecycler?.adapter = SearchAdapter(this@UserActivity,userObject)
//                mRecycler?.adapter?.setHasStableIds(true)
            }

        })

        val serachView = findViewById(R.id.search) as android.support.v7.widget.SearchView
        serachView.setOnQueryTextListener(object : android.support.v7.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {

                return true


            }
            override fun onQueryTextChange(p0: String?): Boolean {
                mRecycler?.visibility=View.VISIBLE
                recUser.visibility = View.INVISIBLE
                listSearchUser.clear()
                for (i in userObject) {
                    if (i.name!!.toLowerCase().contains(p0.toString().toLowerCase())) {
                        listSearchUser.add(i)
                    }
                }
                mRecycler?.adapter = SearchAdapter(this@UserActivity,listSearchUser)
//                mRecycler?.adapter?.setHasStableIds(true)
                return true
            }

        })



//        val dbRefUser = FirebaseDatabase.getInstance().getReference("User")




//        imgSearch.setOnClickListener {
//            val intent = Intent(this@UserActivity , SearchActivity::class.java)
//            startActivity(intent)
//        }
        var mAdapter = object : FirebaseRecyclerAdapter<User , UserActivity.MyHolder>(User::class.java ,
                R.layout.list_user_new , MyHolder::class.java , dbRefUser) {
            public override fun populateViewHolder(myholder: UserActivity.MyHolder , user: User , position: Int) {

                if (auth.currentUser!!.uid != getRef(myholder.adapterPosition).key) {
                    myholder.tvUser.setText(user.name)

                    imgUrl = user.photoUrl
                    val imageView = myholder.ivUser

                        println(user.name)
                        Glide.with(imageView.getContext())
                                .load(imgUrl)
                                .placeholder(R.drawable.ic_perm_identity_black_48dp)
                                .into(imageView)

                } else {
                    myholder.tvUser.visibility = View.GONE
                    myholder.ivUser.visibility= View.GONE
                }

                myholder.tvUser.setOnClickListener {
                    val dbRefThreadMember = FirebaseDatabase.getInstance().getReference("Thread-Member")
                    val idList = listOf<String?>(getRef(myholder.adapterPosition).key , auth.currentUser?.uid)
                    val newList1 = idList.sortedBy { it.toString() }
                    val threadId = newList1[0] + "-" + newList1[1]


                    dbRefThreadMember.run { child(threadId).child(auth.currentUser?.uid).setValue(true) }
                    dbRefThreadMember.run { child(threadId).child(getRef(myholder.adapterPosition).key).setValue(true) }

                    val dbRefUserThread = FirebaseDatabase.getInstance().getReference("User-Thread")
                    dbRefUserThread.run { child(auth.currentUser?.uid).child(threadId).setValue(true) }
                    dbRefUserThread.run { child(getRef(myholder.adapterPosition).key).child(threadId).setValue(true) }

                    val intent = Intent(this@UserActivity , ChatActivity::class.java)
                    intent.putExtra("threadId" , threadId)
                    intent.putExtra("uName" , myholder.tvUser.text)
                    intent.putExtra("photoUrl" , user.photoUrl)
                    intent.putExtra("userId",getRef(myholder.adapterPosition).key)
                    startActivity(intent)
                    finish()
                }
            }
        }
        recUser.adapter = mAdapter

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    class MyHolder(v: View) : RecyclerView.ViewHolder(v) {

        internal var tvUser: TextView
        internal var ivUser: RoundedImageView

        init {
            tvUser = v.findViewById(R.id.tv_user_name) as TextView
            ivUser = v.findViewById(R.id.iv_user) as RoundedImageView
        }

    }
}
