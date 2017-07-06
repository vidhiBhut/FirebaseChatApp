package com.example.vidhi.firebasechatapp

import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.ContactsContract
import android.provider.MediaStore
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.makeramen.roundedimageview.RoundedImageView
import com.ravikoradiya.zoomableimageview.ZoomableImageView
import org.jetbrains.anko.toast
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat

class ChatActivity : AppCompatActivity() {
    var idSev: String? = null
    var idLocal: String? = null
    var mAdapter: FirebaseRecyclerAdapter<Chat , RecyclerView.ViewHolder>? = null;
    var dbRefChat: DatabaseReference? = null
    var frameLayout: GridLayout? = null
    var threadId: String? = ""
    var imgUser: RoundedImageView? = null
    val auth = FirebaseAuth.getInstance()

    //a Uri object to store file path
    var filePath: Uri? = null


    //firebase storage reference
    var storageReference: StorageReference? = null

    ///..........variables for audio
    val currentFormat = 0
    val output_formats = intArrayOf(MediaRecorder.OutputFormat.MPEG_4 , MediaRecorder.OutputFormat.THREE_GPP)
    var recorder: MediaRecorder? = null
    var player: SimpleExoPlayer? = null
    val bandwidthMeter: DefaultBandwidthMeter = DefaultBandwidthMeter()
    var mFilename: String? = Environment.getExternalStorageDirectory().absolutePath


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        dbRefChat = FirebaseDatabase.getInstance().getReference("Chat")

        val currentId = FirebaseAuth.getInstance()
        idLocal = currentId.currentUser?.uid
        storageReference = FirebaseStorage.getInstance().getReference("Store")
        mFilename += "/recorded_audio.3gp"


        val etMsg = findViewById(R.id.add_task_box) as EditText
        val btnSend = findViewById(R.id.add_task_button) as ImageView
        imgUser = findViewById(R.id.iv_user) as RoundedImageView
        val nameUser = findViewById(R.id.tv_user_name) as TextView
        val btnattachImage = findViewById(R.id.btn_attach_image) as ImageView
        val tvOnline = findViewById(R.id.tv_online) as TextView
        val tvTyping = findViewById(R.id.tv_typing) as TextView
        val recycler = findViewById(R.id.task_list) as RecyclerView
        val ivDelete = findViewById(R.id.iv_delete) as ImageView
        val layoutManager = LinearLayoutManager(this)
        recycler.layoutManager = layoutManager


        ///..............attach layout
        ///
        frameLayout = findViewById(R.id.attach_layout) as GridLayout
        val linearLayout = findViewById(R.id.line2) as LinearLayout
        val btnDoc = findViewById(R.id.btn_doc) as ImageView
        val btnCamera = findViewById(R.id.btn_camera) as ImageView
        val btnImg = findViewById(R.id.btn_img) as ImageView
        val btnAudio = findViewById(R.id.btn_audio) as ImageView
        val btnLocation = findViewById(R.id.btn_location) as ImageView
        val btnContact = findViewById(R.id.btn_contact) as ImageView


        ///...setting attach layout visibility
        linearLayout.setOnClickListener {
            frameLayout?.visibility = View.GONE
            linearLayout.visibility = View.GONE
        }
        etMsg.setOnClickListener {
            frameLayout?.visibility = View.GONE
        }


        ////.......getting details for actionbar
        val intent: Intent = intent
        threadId = intent.getStringExtra("threadId")
        val userName = intent.getStringExtra("uName")
        val userPhoto = intent.getStringExtra("photoUrl")
        val userId = intent.getStringExtra("userId")
        println(userPhoto)

        ////..........setting toolbar details
        val toolbar = findViewById(R.id.toolbar) as android.support.v7.widget.Toolbar
        setSupportActionBar(toolbar)
        Glide.with(imgUser?.getContext())
                .load(userPhoto)
                .placeholder(R.drawable.ic_perm_identity_black_48dp)
                .into(imgUser)

        nameUser.setText(userName)
        println(userName)

        setSupportActionBar(toolbar)
        val ab = supportActionBar
        ab!!.setHomeAsUpIndicator(R.drawable.ic_navigate_before_white_48dp)
        ab.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        val dbRefOnline = FirebaseDatabase.getInstance().getReference("User").child(userId)
        dbRefOnline.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDataChange(data: DataSnapshot?) {
                val userData = data?.getValue(User::class.java)
                if (!userData?.online.isNullOrEmpty()) {
                    tvOnline.setText("online")
                } else if (userData?.lastSeen != null) {
                    val sfd: SimpleDateFormat = SimpleDateFormat("dd-MM-yy HH:mm")
                    var formatedTime = sfd.format(userData?.lastSeen)
                    tvOnline.setText(formatedTime)
                }
            }

        })

        ///.....set text watcher...
//        etMsg.addTextWatcher { _,_,_,_ -> Log.d("TAG" , "onCreate: " + etMsg.text) }
        etMsg.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun beforeTextChanged(s: CharSequence? , start: Int , count: Int , after: Int) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onTextChanged(s: CharSequence? , start: Int , before: Int , count: Int) {
                if (count > 0) {
                    val dbRefThreadDetails = FirebaseDatabase.getInstance().getReference("Thread-Details")
                    dbRefThreadDetails.run { child(threadId).child("typing").child(auth.currentUser?.uid).setValue(true) }
                } else {
                    FirebaseDatabase.getInstance().getReference("Thread-Details")
                            .child(threadId)
                            .child("typing")
                            .child(auth.currentUser?.uid)
                            .removeValue()
                }
            }


        })

        ///......set typing

        val dbRefTyping = FirebaseDatabase.getInstance().getReference("Thread-Details").child(threadId).child("typing").child(userId)
        dbRefTyping.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {
//                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDataChange(p0: DataSnapshot?) {
                val newText: TextView = TextView(this@ChatActivity)
                val check = p0?.getValue(Boolean::class.java)
                println(".................check typing" + check)
                if (p0?.getValue(Boolean::class.java) == true) {
                    tvTyping.visibility = View.VISIBLE
                } else {
                    tvTyping.visibility = View.GONE
                }
            }
        })


        /////declare here bcoz of lateinit issue......
        val newRef = dbRefChat?.child(threadId)


        ////....delete whole chat
        ivDelete.setOnClickListener {
            val subdialog: AlertDialog.Builder = AlertDialog.Builder(this@ChatActivity)
            subdialog.setTitle("Are you sure to Delete chat ?")
                    .setPositiveButton("Yes" , { update: DialogInterface? , which: Int ->
                        val dlt = dbRefChat?.child(threadId)?.removeValue()
                        println(dlt)
                    })
                    .setNegativeButton("No" , { updatee: DialogInterface? , which: Int -> })

            val display = subdialog
            display.create()
            display.show()

        }

        ///adapter.......
        mAdapter = object : FirebaseRecyclerAdapter<Chat , RecyclerView.ViewHolder>(Chat::class.java ,
                R.layout.list_msg , RecyclerView.ViewHolder::class.java , newRef) {

            public override fun populateViewHolder(viewHolder: RecyclerView.ViewHolder , chat: Chat , position: Int) {

                ///...........msg is text
                if (!chat.msg.isNullOrEmpty()) {
                    var viewHolder1 = viewHolder as TextMsgHolder
                    viewHolder1.tvMsg.setText(chat.msg)
                    val sTime = chat.time
                    val sfd: SimpleDateFormat = SimpleDateFormat("dd-MM-yy HH:mm")
                    var formatedTime = sfd.format(sTime)
                    idSev = chat.userId
                    viewHolder1.tvTime.setText(formatedTime)

                    if (idSev.equals(idLocal)) {
                        viewHolder1.linearLayout.gravity = Gravity.END
                        viewHolder1.tvMsg.setBackgroundResource(R.drawable.drawable1)
                    } else {
                        viewHolder1.linearLayout.gravity = Gravity.START
                        viewHolder1.tvMsg.setBackgroundResource(R.drawable.drawable)
                    }

                    viewHolder1.tvMsg.setOnLongClickListener({
                        val subdialog: AlertDialog.Builder = AlertDialog.Builder(this@ChatActivity)
                        subdialog.setTitle("Are you sure to Delete chat ?")
                                .setPositiveButton("Yes" , { update: DialogInterface? , which: Int ->
                                    val dlt = dbRefChat?.child(threadId)?.child(getRef(viewHolder1.adapterPosition).key)?.removeValue()
                                    println(dlt)
                                })
                                .setNegativeButton("No" , { updatee: DialogInterface? , which: Int -> })

                        val display = subdialog
                        display.create()
                        display.show()
                        false
                    })

                }
                ///............msg is images
                else if (!chat.imgPath.isNullOrEmpty()) {
                    val viewHolder2 = viewHolder as ImageHolder

                    idSev = chat.userId
                    val imgSrc = chat.imgPath

                    Glide.with(viewHolder2.ivMsg.getContext())
                            .load(imgSrc)
                            .into(viewHolder2.ivMsg)
                    val sTime1 = chat.time
                    val sfd: SimpleDateFormat = SimpleDateFormat("dd-MM-yy HH:mm")
                    var formatedTime = sfd.format(sTime1)
                    viewHolder2.tvTime.setText(formatedTime)

                    if (idSev.equals(idLocal)) {
                        viewHolder2.line1.gravity = Gravity.END
                        viewHolder2.ivMsg.setBackgroundResource(R.drawable.drawable1)
                    } else {
                        viewHolder2.line1.gravity = Gravity.START
                        viewHolder2.ivMsg.setBackgroundResource(R.drawable.drawable)
                    }


                    viewHolder2.ivMsg.setOnLongClickListener({
                        val subdialog: AlertDialog.Builder = AlertDialog.Builder(this@ChatActivity)
                        subdialog.setTitle("Are yoy sure to Delete chat ?")
                                .setPositiveButton("Yes" , { update: DialogInterface? , which: Int ->
                                    val dlt = dbRefChat?.child(threadId)
                                            ?.child(getRef(viewHolder2.adapterPosition).key)
                                            ?.removeValue()
                                    storageReference!!.child(threadId!!)
                                            .child(getRef(viewHolder2.adapterPosition).key)
                                            .delete().addOnSuccessListener(object : OnSuccessListener<Void> {
                                        override fun onSuccess(p0: Void?) {
                                            Toast.makeText(applicationContext , "Message deleted" , Toast.LENGTH_SHORT).show()
                                        }

                                    })
                                    println(dlt)
                                })
                                .setNegativeButton("No" , { updatee: DialogInterface? , which: Int -> })

                        val display = subdialog
                        display.create()
                        display.show()
                        false
                    })

                }
                ///....msg is audio........
                else if (!chat.audioPath.isNullOrEmpty()) {
                    var audioHolder = viewHolder as AudioHolder


                    val sTime = chat.time
                    val sfd: SimpleDateFormat = SimpleDateFormat("dd-MM-yy HH:mm")
                    var formatedTime = sfd.format(sTime)
                    idSev = chat.userId
                    audioHolder.tvTime.setText(formatedTime)

                    fun buildHttpDataSourceFactor(bandwidthMeter: DefaultBandwidthMeter): DefaultHttpDataSourceFactory {
                        return DefaultHttpDataSourceFactory(Util.getUserAgent(this@ChatActivity ,
                                getString(R.string.app_name)) , bandwidthMeter)
                    }

                    var videoTrackSelectionFactory: TrackSelection.Factory = AdaptiveTrackSelection.Factory(bandwidthMeter)
                    player = ExoPlayerFactory.newSimpleInstance(this@ChatActivity ,
                            DefaultTrackSelector(videoTrackSelectionFactory) ,
                            DefaultLoadControl())

                    audioHolder.simpleExoPlayerView.player = player

                    var dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(this@ChatActivity ,
                            bandwidthMeter ,
                            buildHttpDataSourceFactor(bandwidthMeter))
                    var videoSource: MediaSource = ExtractorMediaSource(Uri.parse(chat.audioPath) , dataSourceFactory ,
                            DefaultExtractorsFactory() , Handler() , null)
                    player?.prepare(videoSource)

                    player?.addListener(object : ExoPlayer.EventListener {
                        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
//                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }

                        override fun onTracksChanged(trackGroups: TrackGroupArray? , trackSelections: TrackSelectionArray?) {
                        }

                        override fun onPlayerError(error: ExoPlaybackException?) {
//                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }

                        override fun onLoadingChanged(isLoading: Boolean) {
                        }

                        override fun onPositionDiscontinuity() {
//                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }

                        override fun onTimelineChanged(timeline: Timeline? , manifest: Any?) {
//                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }

                        override fun onPlayerStateChanged(playWhenReady: Boolean , playbackState: Int) {

                            println("..................onPlayerStateChanged + $playbackState");

                            var resumeWindow: Int = 0
                            var haveResumePosition: Boolean = resumeWindow != C.INDEX_UNSET;

                            if (playbackState == ExoPlayer.STATE_ENDED) {
                                if (haveResumePosition) {
                                    player?.seekTo(resumeWindow , 0L) //changed
                                    player?.playWhenReady = false
                                }
                            }
                        }


                    })

                    if (idSev.equals(idLocal)) {
                        audioHolder.audioLayout.gravity = Gravity.END
                        audioHolder.simpleExoPlayerView.setBackgroundResource(R.drawable.drawable1)

                    } else {
                        audioHolder.audioLayout.gravity = Gravity.START
                        audioHolder.simpleExoPlayerView.setBackgroundResource(R.drawable.drawable)

                    }

                    audioHolder.simpleExoPlayerView.setOnClickListener({
                        val subdialog: AlertDialog.Builder = AlertDialog.Builder(this@ChatActivity)
                        subdialog.setTitle("Are yoy sure to Delete chat ?")
                                .setPositiveButton("Yes" , { update: DialogInterface? , which: Int ->
                                    val dlt = dbRefChat?.child(threadId)
                                            ?.child(getRef(audioHolder.adapterPosition).key)
                                            ?.removeValue()
                                    println(dlt)

                                    storageReference!!.child(threadId!!)
                                            .child(getRef(audioHolder.adapterPosition).key)
                                            .delete().addOnSuccessListener(object : OnSuccessListener<Void> {
                                        override fun onSuccess(p0: Void?) {
                                            Toast.makeText(applicationContext , "Message deleted" , Toast.LENGTH_SHORT).show()
                                        }

                                    })
                                })
                                .setNegativeButton("No" , { updatee: DialogInterface? , which: Int -> })

                        val display = subdialog
                        display.create()
                        display.show()
                        false
                    })

                }

                ///......msg is contact
                else if (!chat.contact?.name.isNullOrEmpty()) {
                    var viewHolder4 = viewHolder as ContactHolder

                    viewHolder4.tvName?.setText(chat.contact?.name)
                    viewHolder4.tvNumber?.setText(chat.contact?.number)
                    val sTime = chat.time
                    val sfd: SimpleDateFormat = SimpleDateFormat("dd-MM-yy HH:mm")
                    var formatedTime = sfd.format(sTime)
                    idSev = chat.userId
                    viewHolder4.tvTime?.setText(formatedTime)

                    if (idSev.equals(idLocal)) {
                        viewHolder4.contactLayout?.gravity = Gravity.END
                        viewHolder4.colorLayout?.setBackgroundResource(R.drawable.drawable1)
                    } else {
                        viewHolder4.contactLayout?.gravity = Gravity.START
                        viewHolder4.colorLayout?.setBackgroundResource(R.drawable.drawable)
                    }

                    viewHolder4.contactLayout?.setOnLongClickListener({
                        val subdialog: AlertDialog.Builder = AlertDialog.Builder(this@ChatActivity)
                        subdialog.setTitle("Are yoy sure to Delete chat ?")
                                .setPositiveButton("Yes" , { update: DialogInterface? , which: Int ->
                                    val dlt = dbRefChat?.child(threadId)?.child(getRef(viewHolder4.adapterPosition).key)?.removeValue()
                                    println(dlt)
                                })
                                .setNegativeButton("No" , { updatee: DialogInterface? , which: Int -> })

                        val display = subdialog
                        display.create()
                        display.show()
                        false
                    })
                }

                /////......msg is file
                else if (!chat.filePath.isNullOrEmpty()) {

                    val fileHolder = viewHolder as FileHolder
                    val sTime = chat?.time
                    val sfd: SimpleDateFormat = SimpleDateFormat("dd-MM-yy HH:mm")
                    var formatedTime = sfd.format(sTime)
                    idSev = chat?.userId!!
                    fileHolder.tvTime.setText(formatedTime)

                    if (idSev.equals(idLocal)) {
                        fileHolder.linearFile.gravity = Gravity.END
                        fileHolder.fileLayout.setBackgroundResource(R.drawable.drawable1)
                    } else {
                        fileHolder.linearFile.gravity = Gravity.START
                        fileHolder.fileLayout.setBackgroundResource(R.drawable.drawable1)

                    }


                    fileHolder.ivDownload.setOnClickListener {
                        //                        storageRef.child("users/me/profile.png").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
//    @Override
//    public void onSuccess(Uri uri) {
//        // Got the download URL for 'users/me/profile.png'
//    }
//}).addOnFailureListener(new OnFailureListener() {
//    @Override
//    public void onFailure(@NonNull Exception exception) {
//        // Handle any errors
//    }
//});
//                        dbRefChat.child(threadId).
//
//                        List<FileDownloadTask> tasks = mStorageRef.getActiveDownloadTasks();
//                        if (tasks.size() > 0) {
//                            // Get the task monitoring the download
//                            FileDownloadTask task = tasks.get(0);
//
//                            // Add new listeners to the task using an Activity scope
//                            task.addOnSuccessListener(this, new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
//                                @Override
//                                public void onSuccess(FileDownloadTask.TaskSnapshot state) {
//                                    handleSuccess(state); //call a user defined function to handle the event.
//                                }
//                            });





                    }




                    fileHolder.fileLayout.setOnLongClickListener {

                        val subdialog: AlertDialog.Builder = AlertDialog.Builder(this@ChatActivity)
                        subdialog.setTitle("Are you want to sure delete?").setPositiveButton("Yes", { update: DialogInterface?, which: Int ->

                            dbRefChat?.child(threadId)
                                    ?.child(getRef(fileHolder.adapterPosition).key)
                                    ?.removeValue()
                            storageReference!!.child(threadId!!)
                                    .child(getRef(fileHolder.adapterPosition).key)
                                    .delete().addOnSuccessListener(object : OnSuccessListener<Void> {
                                override fun onSuccess(p0: Void?) {
                                    Toast.makeText(applicationContext, "File Succesfully deleted", Toast.LENGTH_SHORT).show()
                                }

                            })


                        }).setNegativeButton("No", { updatee: DialogInterface?, which: Int -> })

                        val display = subdialog
                        display.create()
                        display.show()
                        false


                    }
                }

                ///....msg is location
                else {
                    var viewHolder5 = viewHolder as LocationHolder


                    if (idSev.equals(idLocal)) {
                        viewHolder5.locationLayout?.gravity = Gravity.END
                        viewHolder5.ivLocation?.setBackgroundResource(R.drawable.drawable1)
                    } else {
                        viewHolder5.locationLayout?.gravity = Gravity.START
                        viewHolder5.ivLocation?.setBackgroundResource(R.drawable.drawable)
                    }

                    val lat = chat.location?.lat
                    val long = chat.location?.long
                    viewHolder5.ivLocation?.setOnClickListener {
                        val uri = "https://www.google.com/maps/search/?api=1&query=$lat,$long"
                        val intent = Intent(android.content.Intent.ACTION_VIEW , Uri.parse(uri))
                        intent.setClassName("com.google.android.apps.maps" , "com.google.android.maps.MapsActivity")
                        startActivity(intent)
                    }
                    val sTime = chat.time
                    val sfd: SimpleDateFormat = SimpleDateFormat("dd-MM-yy HH:mm")
                    var formatedTime = sfd.format(sTime)
                    idSev = chat.userId
                    viewHolder5.tvTime?.setText(formatedTime)


                    ////delete msg
                    viewHolder5.ivLocation?.setOnLongClickListener({
                        val subdialog: AlertDialog.Builder = AlertDialog.Builder(this@ChatActivity)
                        subdialog.setTitle("Are yoy sure to Delete chat ?")
                                .setPositiveButton("Yes" , { update: DialogInterface? , which: Int ->
                                    val dlt = dbRefChat?.child(threadId)?.child(getRef(viewHolder5.adapterPosition).key)?.removeValue()
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

            override fun onCreateViewHolder(parent: ViewGroup? , viewType: Int): RecyclerView.ViewHolder {

                if (viewType == 0) {
                    val userType1: View = LayoutInflater.from(parent?.getContext())
                            .inflate(R.layout.list_msg , parent , false);
                    return TextMsgHolder(userType1)

                } else if (viewType == 1) {
                    val userType2: View = LayoutInflater.from(parent?.getContext())
                            .inflate(R.layout.msg_img , parent , false);
                    return ImageHolder(userType2)
                } else if (viewType == 2) {
                    val userType3: View = LayoutInflater.from(parent?.getContext())
                            .inflate(R.layout.msg_audio , parent , false);
                    return AudioHolder(userType3)
                } else if (viewType == 3) {
                    val userType4: View = LayoutInflater.from(parent?.getContext())
                            .inflate(R.layout.msg_contact , parent , false);
                    return ContactHolder(userType4)
                } else if (viewType == 4) {
                    val userType5: View = LayoutInflater.from(parent?.getContext())
                            .inflate(R.layout.msg_file , parent , false);
                    return FileHolder(userType5)
                } else {
                    val userType6: View = LayoutInflater.from(parent?.getContext())
                            .inflate(R.layout.msg_location , parent , false);
                    return LocationHolder(userType6)
                }

            }

            override fun getItemViewType(position: Int): Int {
                val chat: Chat = getItem(position)
                if (!chat.msg.isNullOrEmpty()) {
                    return 0
                } else if (!chat.imgPath.isNullOrEmpty()) {
                    return 1
                } else if (!chat.audioPath.isNullOrEmpty()) {
                    return 2
                } else if (!chat.contact?.name.isNullOrEmpty()) {
                    return 3
                }
                else if(!chat.filePath.isNullOrEmpty()){
                    return 4
                }
                else {
                    return 5
                }
            }


        }///.........adapter

        recycler.adapter = mAdapter

        var adapterDataObserver: RecyclerView.AdapterDataObserver = object : RecyclerView.AdapterDataObserver() {

            override fun onItemRangeInserted(positionStart: Int , itemCount: Int) {
                super.onItemRangeInserted(positionStart , itemCount)
                recycler.scrollToPosition(positionStart)

            }
        }

        mAdapter?.registerAdapterDataObserver(adapterDataObserver)

        btnattachImage.setOnClickListener {
            if (frameLayout?.visibility == View.VISIBLE) {
                frameLayout?.visibility = View.GONE
            } else {
                frameLayout?.visibility = View.VISIBLE
            }
            linearLayout.visibility = View.VISIBLE

            ///........store image from phone storage
            btnImg.setOnClickListener {

                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent , "Select Picture") , 234)
            }

            //................store image from camera
            btnCamera.setOnClickListener {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent , 1)
            }

            //................store audio

            btnAudio.setOnTouchListener(object : View.OnTouchListener {

                override fun onTouch(v: View? , event: MotionEvent?): Boolean {
                    if (event?.action == MotionEvent.ACTION_DOWN) {
                        startRecording()
                        Toast.makeText(applicationContext , "Record Start" , Toast.LENGTH_SHORT).show()
                    } else if (event?.action == MotionEvent.ACTION_UP) {
                        stopRecording()
                        Toast.makeText(applicationContext , "Record Stop" , Toast.LENGTH_SHORT).show()

                        if (!mFilename.equals("")) {
                            var progressDialog = ProgressDialog(this@ChatActivity)
                            progressDialog.setTitle("Uploading")
                            progressDialog.show()
                            val key1 = dbRefChat?.push()?.key
                            val audioStorage = storageReference!!.child(threadId!!).child(key1!!)
                            audioStorage.putFile(Uri.fromFile(File(mFilename))).addOnProgressListener { taskSnapshot ->
                                //calculating progress percentage
                                val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount

                                //displaying percentage in progress dialog
                                progressDialog.setMessage("Uploading " + progress.toInt() + "%...")
                            }

                                    .addOnSuccessListener(object : OnSuccessListener<UploadTask.TaskSnapshot> {
                                        override fun onSuccess(p0: UploadTask.TaskSnapshot?) {

                                            /////store audio in storage
                                            val url: String? = p0?.downloadUrl.toString()
                                            val time: Long = System.currentTimeMillis()
                                            val audioMsg = Chat(time = time , audioPath = url , userId = idLocal)
                                            dbRefChat?.run { child(threadId).child(key1).setValue(audioMsg) }
                                            progressDialog.dismiss()

                                            println("................in success listener ....................")

                                        }
                                    })

                        }

                    }
                    return true
                }

            })


            /////......store contact
            btnContact.setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK , ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                startActivityForResult(intent , 5)
            }


            /////.......store location
            btnLocation.setOnClickListener {

                println("................in on location click...............")
                val mFusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@ChatActivity)
                mFusedLocationClient.lastLocation
                        .addOnSuccessListener(this) { location ->
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                Toast.makeText(applicationContext , "latitude" + location.latitude , Toast.LENGTH_SHORT).show()
                                Toast.makeText(applicationContext , "longitude" + location.longitude , Toast.LENGTH_SHORT).show()
                                val lat = location.latitude
                                val long = location.longitude

                                ////////////....store location in db
                                val key1 = dbRefChat?.push()?.key
                                val time: Long = System.currentTimeMillis()
                                val myLocation = com.example.vidhi.firebasechatapp.Location(lat = lat , long = long)
                                val locationMsg = Chat(time = time , location = myLocation , userId = idLocal)
                                dbRefChat?.run { child(threadId).child(key1).setValue(locationMsg) }
                            }
                        }

            }

            ///.......store file
            btnDoc.setOnClickListener {
                val intent = Intent()
                intent.action = Intent.ACTION_GET_CONTENT
                intent.type = "contact/*"
                startActivityForResult(intent, 4)
            }


        }


        //////.......send text msg
        btnSend.setOnClickListener {
            var msg: String = etMsg.text.toString()
            if (msg.equals("") || msg.equals(null)) {
                Toast.makeText(applicationContext , "Enter any message" , Toast.LENGTH_SHORT).show()
            } else {
                val id: String? = currentId.currentUser?.uid
                val key = dbRefChat?.push()?.key
                val ctime: Long = System.currentTimeMillis()
                val chat = Chat(msg = msg , time = ctime , userId = id)
                dbRefChat?.run { child(threadId).child(key).setValue(chat) }
                etMsg.text.clear()
            }
        }


    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        FirebaseDatabase.getInstance().getReference("Thread-Details").child(threadId).child("typing").child(auth.currentUser?.uid).removeValue()

    }


    //handling activity result
    override fun onActivityResult(requestCode: Int , resultCode: Int , data: Intent?) {
        super.onActivityResult(requestCode , resultCode , data)

        ///....storing camera image
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            val image = data.getExtras()?.get("data") as Bitmap
            val byte: ByteArrayOutputStream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG , 100 , byte)

            if (image != null) {
                //displaying a progress dialog while upload is going on
                var progressDialog = ProgressDialog(this)
                progressDialog.setTitle("Uploading")
                progressDialog.show()


                val key1 = dbRefChat?.push()?.key
                val riversRef = storageReference!!.child(threadId!!).child(key1!!)

                val baos: ByteArrayOutputStream = ByteArrayOutputStream()
                image.compress(Bitmap.CompressFormat.JPEG , 100 , baos)
                val bytes: ByteArray = baos.toByteArray()
                riversRef.putBytes(bytes).addOnProgressListener { taskSnapshot ->
                    //calculating progress percentage
                    val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount

                    //displaying percentage in progress dialog
                    progressDialog.setMessage("Uploading " + progress.toInt() + "%...")
                }

                        .addOnSuccessListener(object : OnSuccessListener<UploadTask.TaskSnapshot> {
                            override fun onSuccess(p0: UploadTask.TaskSnapshot?) {
                                val url: String? = p0?.downloadUrl.toString()
                                val time: Long = System.currentTimeMillis()
                                val imgMsg = Chat(time = time , imgPath = url , userId = idLocal)
                                dbRefChat?.run { child(threadId!!).child(key1).setValue(imgMsg) }
                                progressDialog.dismiss()

                            }
                        })

            }


        }


        ////............storing storage image
        else if (requestCode == 234 && resultCode == RESULT_OK && data != null && data.data != null) {
            filePath = data.data

            try {
                if (filePath != null) {
                    //displaying a progress dialog while upload is going on
                    var progressDialog = ProgressDialog(this)
                    progressDialog.setTitle("Uploading")
                    progressDialog.show()


                    val key1 = dbRefChat?.push()?.key
                    val riversRef = storageReference!!.child(threadId!!).child(key1!!)


                    riversRef.putFile(filePath!!).addOnProgressListener { taskSnapshot ->
                        //calculating progress percentage
                        val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount

                        //displaying percentage in progress dialog
                        progressDialog.setMessage("Uploading " + progress.toInt() + "%...")
                    }

                            .addOnSuccessListener(object : OnSuccessListener<UploadTask.TaskSnapshot> {
                                override fun onSuccess(p0: UploadTask.TaskSnapshot?) {
                                    val url: String? = p0?.downloadUrl.toString()
                                    val time: Long = System.currentTimeMillis()
                                    val imgMsg = Chat(time = time , imgPath = url , userId = idLocal)
                                    dbRefChat?.run { child(threadId).child(key1).setValue(imgMsg) }
                                    progressDialog.dismiss()

                                }
                            })

                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }


        ///.....storing contact number
        else if (requestCode == 5 && resultCode == Activity.RESULT_OK && data != null && data.data != null) {

            try {

                val contactUri: Uri? = data.data
                println(".............................." + contactUri)

                if (contactUri != null) {
                    //displaying a progress dialog while upload is going on
                    var progressDialog = ProgressDialog(this)
                    progressDialog.setTitle("Uploading")
//                    progressDialog.show()

//                        Uri contactData = data.getData();
                    val phone: Cursor = getContentResolver().query(contactUri , null , null , null , null);
                    if (phone.moveToFirst()) {
                        val contactNumberName: String? = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        val contactNumber: String? = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                        val key1 = dbRefChat?.push()?.key

                        val time: Long = System.currentTimeMillis()
                        val contact = Contact(contactNumberName , contactNumber)
                        val contactMsg = Chat(time = time , contact = contact , userId = idLocal)
                        dbRefChat?.run { child(threadId).child(key1).setValue(contactMsg) }
                        progressDialog.dismiss()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }


        }

        ///....storing file
        else if (requestCode == 4 && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            filePath = data.data

            val pd = ProgressDialog(this)
            val progress = 100.0
            pd.setMessage("Uploading....")

            pd.setMessage("Uploading " + progress.toInt() + "%...")

            if (filePath != null) {
                pd.show()

                val keyId = dbRefChat?.push()?.key
                val childRef = storageReference?.child(threadId!!)?.child(keyId!!)
                //uploading the image
                val uploadTask = childRef?.putFile(filePath!!)


                uploadTask?.addOnSuccessListener(object : OnSuccessListener<UploadTask.TaskSnapshot> {

                    override fun onSuccess(p0: UploadTask.TaskSnapshot?) {
//                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

                        pd.dismiss()

                        Toast.makeText(this@ChatActivity, "Upload successful", Toast.LENGTH_SHORT).show()

                        val pathNew: String? = p0!!.downloadUrl.toString()
                        val ctime: Long = System.currentTimeMillis()
                        val imgMsg = Chat(time = ctime, filePath = pathNew, userId = idLocal)
                        dbRefChat?.run { child(threadId).child(keyId).setValue(imgMsg) }


                    }


                })?.addOnFailureListener(OnFailureListener { e ->
                    pd.dismiss()

                    Toast.makeText(this@ChatActivity, "Upload Failed -> " + e, Toast.LENGTH_SHORT).show()
                    Log.d(".....", e.toString())
                })
            }


        }


    }


    ////holder for text msg
    class TextMsgHolder(v: View) : RecyclerView.ViewHolder(v) {

        internal var tvMsg: TextView
        internal var tvTime: TextView
        internal var linearLayout: LinearLayout

        init {
            tvMsg = v.findViewById(R.id.tv_msg) as TextView
            linearLayout = v.findViewById(R.id.linear_container) as LinearLayout
            tvTime = v.findViewById(R.id.tv_time) as TextView

        }

    }

    //.................holder for image msg
    class ImageHolder(v: View) : RecyclerView.ViewHolder(v) {

        public var ivMsg: ZoomableImageView
        internal var line1: LinearLayout
        internal var tvTime: TextView


        init {
            line1 = v.findViewById(R.id.line1) as LinearLayout
            ivMsg = v.findViewById(R.id.iv_zoom) as ZoomableImageView
            tvTime = v.findViewById(R.id.tv_time) as TextView


        }

    }

    ///////...........holder for audio msg
    class AudioHolder(v: View) : RecyclerView.ViewHolder(v) {
        internal var tvTime: TextView
        internal var audioLayout: LinearLayout
        internal var simpleExoPlayerView: SimpleExoPlayerView


        init {
            tvTime = v.findViewById(R.id.tv_time) as TextView
            audioLayout = v.findViewById(R.id.linear_audio) as LinearLayout
            simpleExoPlayerView = v.findViewById(R.id.main_exoplayer) as SimpleExoPlayerView


        }
    }

    //////......holder for contact msg
    class ContactHolder(v: View) : RecyclerView.ViewHolder(v) {
        internal var tvName: TextView?
        internal var tvNumber: TextView?
        internal var tvTime: TextView?
        internal var contactLayout: LinearLayout?
        internal var colorLayout: LinearLayout?


        init {
            tvName = v.findViewById(R.id.tv_name) as TextView?
            tvNumber = v.findViewById(R.id.tv_number) as TextView?
            tvTime = v.findViewById(R.id.tv_time) as TextView?
            contactLayout = v.findViewById(R.id.linear_contact) as LinearLayout?
            colorLayout = v.findViewById(R.id.line_contact_color) as LinearLayout?


        }
    }

    /////////......holder for location msg
    class LocationHolder(v: View) : RecyclerView.ViewHolder(v) {

        //        internal var mapView: MapView
        internal var tvTime: TextView?
        internal var ivLocation: ImageView?
        internal var locationLayout: LinearLayout?

        init {
            locationLayout = v.findViewById(R.id.line_location) as LinearLayout?
//            mapView = v.findViewById(R.id.map_view) as MapView
            tvTime = v.findViewById(R.id.tv_time) as TextView?
            ivLocation = v.findViewById(R.id.iv_location) as ImageView?
        }


    }

    /////........holder for file msg
    class FileHolder(v: View) : RecyclerView.ViewHolder(v) {
        internal var tvTime: TextView
        internal var ivFile : ImageView
        internal var ivDownload : ImageView
        internal var linearFile: LinearLayout
        internal var fileLayout : LinearLayout


        init {
            tvTime = v.findViewById(R.id.tv_time) as TextView
            ivFile = v.findViewById(R.id.iv_file) as ImageView
            ivDownload = v.findViewById(R.id.iv_download) as ImageView
            linearFile = v.findViewById(R.id.line_file) as LinearLayout
            fileLayout = v.findViewById(R.id.file_layout) as LinearLayout


        }
    }


    ////function for audio
    fun startRecording() {
        recorder = MediaRecorder()
        recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder?.setOutputFormat(output_formats[currentFormat])
        recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        recorder?.setOutputFile(mFilename)

        try {
            recorder?.prepare()
            recorder?.start()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stopRecording() {

        if (null != recorder) {
            try {
                recorder?.stop()
            } catch (stopException: RuntimeException) {
                //handle cleanup here
            }
//            recorder?.stop();
            recorder?.reset();
            recorder?.release();
            recorder = null;
        }


    }
}
