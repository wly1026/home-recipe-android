package com.wly.myapplication

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.net.HttpURLConnection
import java.net.URL


class UserListActivity : AppCompatActivity() {

    private val mAuth = FirebaseAuth.getInstance()
    private val followings = ArrayList<String>()
    private lateinit var linLayout : LinearLayout

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.home_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.logout) {
            mAuth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
        } else if (item.itemId == R.id.post) {
            startActivity(Intent(this, CreatePostActivity::class.java))
        } else if (item.itemId == R.id.contacts) {
            startActivity(Intent(this, ContactsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        linLayout = findViewById(R.id.linLayout)

        val currUserId = FirebaseAuth.getInstance().currentUser!!.uid
        val usersRef = Firebase.database.reference.child("users")
        val followingRef = usersRef.child("$currUserId").child("following")

        // retrieve all following's posts by create time
        followingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (following in dataSnapshot.children) {
                    val followId = following.value as String
                    var q = Firebase.database.reference.child("posts")
                        .child("$followId").orderByKey()
                    q.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for (data in dataSnapshot.children) {
                                val map = data.value as Map<String, String>
                                draw(map)
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("TAG", "onCancelled", databaseError.toException())
                        }
                    })

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("Fail", "loadPost:onCancelled", databaseError.toException())
            }
        })

    }

    private fun draw(map: Map<String, String>) {

        val imageView = ImageView(applicationContext)
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val task = ImageDownloader()
        val bitmap: Bitmap
        try {
            bitmap = task.execute(map["imageURL"]).get()!!
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        linLayout.addView(imageView)

        val textView = TextView(applicationContext)
        textView.text = map["message"] as String
        linLayout.addView(textView)

    }

    class ImageDownloader : AsyncTask<String?, Void?, Bitmap?>() {
        override fun doInBackground(vararg urls: String?): Bitmap? {
            return try {
                val url = URL(urls[0])
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()
                val `in` = connection.inputStream
                BitmapFactory.decodeStream(`in`)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                null
            }
        }
    }

}