package com.wly.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class ContactsActivity : AppCompatActivity() {

    var listView: ListView? = null
    var emails: ArrayList<String>? = ArrayList()
    var keys: ArrayList<String>? = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        title = "Friends"

        listView = findViewById(R.id.listView)
        listView?.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE

        var arrayAdapter: ArrayAdapter<*> = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_checked,
            emails!!
        )
        listView?.adapter = arrayAdapter

        val key = FirebaseAuth.getInstance().currentUser!!.uid
        var usersRef = Firebase.database.reference.child("users")
        var followingRef = usersRef.child("$key").child("following")

        // retrieve data from database
        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val userKey = snapshot.key
                Log.i("user", userKey.toString())

                emails?.add(snapshot.child("email").value as String)
                keys?.add(userKey.toString())

                // update checked button
                updateFollowing(userKey!!, followingRef, (keys?.size?.minus(1)))

                arrayAdapter.notifyDataSetChanged()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildRemoved(snapshot: DataSnapshot) {}

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {}
        }

        usersRef.addChildEventListener(childEventListener)

        // create isFollowing lists according to if the item is checked
        listView?.onItemClickListener = AdapterView.OnItemClickListener { arrayView, view, position, id ->
            val checkedTextView = view as CheckedTextView
            var friendKey = keys!![position]
            if (checkedTextView.isChecked) {
                Log.i("Info", "Checked!")
                val followingId = followingRef.push().key
                followingRef.child(followingId!!).setValue("$friendKey")
            } else {
                Log.i("Info", "NOT Checked!")
                delete(friendKey, followingRef)
            }
        }
        
    }

    private fun updateFollowing(str: String, ref: DatabaseReference, pos: Int?) {
        val q = ref.orderByValue().equalTo(str)
        q.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    if (pos != null) {
                        listView?.setItemChecked(pos, true)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TAG", "onCancelled", error.toException())
            }

        })
    }

    private fun delete(str: String, ref: DatabaseReference) {
        val q = ref.orderByValue().equalTo(str)
        q.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (data in dataSnapshot.children) {
                    data.ref.removeValue()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "onCancelled", databaseError.toException())
            }
        })
    }
}