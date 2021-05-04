package com.wly.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*


class CreatePostActivity : AppCompatActivity() {

    var content: EditText? = null
    var postButton: Button? = null
    var image: ImageView? = null
    var imageName = UUID.randomUUID().toString() + ".jpg"  //????
    var imageUrl: String = ""
    var uploadImage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        content = findViewById(R.id.content);
        postButton = findViewById(R.id.postButton);
        image = findViewById(R.id.imageView);
    }

    private fun getPhoto() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 1)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getPhoto()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        var bitmap: Bitmap? = null
        // show the image
        val selectedImage = data!!.data
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedImage)
                image!!.setImageBitmap(bitmap)
                uploadImage = true
            } catch (e: IOException) {
                e.printStackTrace()
            }

            // Get the data from an ImageView as bytes
            val baos = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            // store image in storage
            var mountainsRef: StorageReference = FirebaseStorage.getInstance().reference
                .child("images")
                .child(imageName)
            var uploadTask = mountainsRef.putBytes(data)

            uploadTask.addOnFailureListener(OnFailureListener {
                // Handle unsuccessful uploads
                Toast.makeText(this, "UploadFailed", Toast.LENGTH_SHORT).show()
            }).addOnSuccessListener(OnSuccessListener<UploadTask.TaskSnapshot> { taskSnapshot ->
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                Toast.makeText(this, "UploadSuccess", Toast.LENGTH_SHORT).show()
            })

            val urlTask = uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                mountainsRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    imageUrl = task.result.toString()
                } else {
                    Log.i("Fail", "Fail get url")
                }
            }
        } else {
            Log.i("Fail", "Can not show image")
        }
    }

    fun getShowPhoto(view: View) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        } else {
            getPhoto()
        }
    }

    fun post(view: View?) {
        if (!uploadImage || content?.text.toString().equals("")) {
            Toast.makeText(this, "Message or image is empty!", Toast.LENGTH_SHORT).show()
        } else {
            // store data in database
            val key = FirebaseAuth.getInstance().currentUser!!.uid
            val postMap: Map<String, String> = mapOf(
                "imageName" to imageName,
                "imageURL" to imageUrl,
                "message" to content?.text.toString()
            )

            // put the post the posts/
            FirebaseDatabase.getInstance().reference.child("posts").child("$key").push().setValue(
                postMap
            )


            startActivity(Intent(this, UserListActivity::class.java))
        }
    }

//    fun cancel(view: View?) {
//        // TODO
//        // add an alert window
//        startActivity(Intent(applicationContext, UserListActivity::class.java))
//    }

}

