package com.wly.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class MainActivity : AppCompatActivity(), View.OnClickListener{

    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var signButton: Button
    private var signInMode: Boolean? = null
    private lateinit var switchText: TextView
    private lateinit var background: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        signButton = findViewById(R.id.signButton)
        signInMode = true
        switchText = findViewById(R.id.switchText)
        background = findViewById(R.id.background)

        this.switchText.setOnClickListener(this)
        this.background.setOnClickListener(this)

    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if(currentUser != null){
            showUserList()
        }
    }

    override fun onClick(view: View) {
        if (view.id === switchText.id) {
            if (signInMode == true) {
                signInMode = false
                signButton.text = "Register"
                switchText.text = "Already Have an account?"
            } else {
                signInMode = true
                signButton.text = "Sign In"
                switchText.text = "Create an account?"
            }
        } else if (view.id === background.id) {
            val inputMethodManager: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
    }

    fun sign(view: View) {
        if (email.text.toString().isEmpty() || password.text.toString().isEmpty()) {
            Toast.makeText(this, "A username and password are required", Toast.LENGTH_SHORT).show()
            // add password length check
        } else if (signInMode == true) {
            signIn()
        } else {
            signUp()
        }
    }

    private fun signUp() {
        auth.createUserWithEmailAndPassword(email.text.toString(), password.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("Success", "createUserWithEmail:success")
                        val user = auth.currentUser
                        FirebaseDatabase.getInstance().reference.child("users").child(user.uid)
                            .child("email").setValue(email.text.toString())
                        showUserList()
                    } else {
                        Log.w("Fail", "createUserWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun signIn() {
        auth.signInWithEmailAndPassword(email.text.toString(), password.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("Success", "signInWithEmail:success")
                        showUserList()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("Fail", "signInWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                    }
                }
    }

    fun showUserList() {
        val intent = Intent(applicationContext, UserListActivity::class.java)
        startActivity(intent)
    }

}