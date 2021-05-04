package com.wly.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.facebook.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.facebook.appevents.AppEventsLogger
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.auth.FacebookAuthProvider


class MainActivity : AppCompatActivity(), View.OnClickListener{

    private lateinit var auth: FirebaseAuth
    private lateinit var callbackManager: CallbackManager

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var signButton: Button
    private var signInMode: Boolean? = null
    private lateinit var switchText: TextView
    private lateinit var background: ConstraintLayout
    private lateinit var buttonFacebookLogin : LoginButton

    private var TAG = "FacebookAuthentication"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);

        auth = FirebaseAuth.getInstance()
        callbackManager = CallbackManager.Factory.create()

        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        signButton = findViewById(R.id.signButton)
        signInMode = true
        switchText = findViewById(R.id.switchText)
        background = findViewById(R.id.background)
        buttonFacebookLogin = findViewById(R.id.buttonFacebookLogin)

        this.switchText.setOnClickListener(this)
        this.background.setOnClickListener(this)

        buttonFacebookLogin.setReadPermissions("email", "public_profile")
        buttonFacebookLogin.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Log.d(TAG, "facebook:onSuccess:$loginResult")
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                Log.d(TAG, "facebook:onCancel")
            }

            override fun onError(error: FacebookException) {
                Log.d(TAG, "facebook:onError", error)
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Pass the activity result back to the Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d(TAG, "handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser

                    // add users in the database
                    val userRef = FirebaseDatabase.getInstance().reference.child("users").child(
                        user.uid
                    )
                    userRef.child("email").setValue(user.email)

                    // set the user as self following
                    userRef.child("following").push().setValue(user.uid)

                    showUserList()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                }
            }
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
                        val userRef = FirebaseDatabase.getInstance().reference.child("users").child(
                            user.uid
                        )
                        userRef.child("email").setValue(email.text.toString())

                        // set the user as self following
                        userRef.child("following").push().setValue(user.uid)

                        showUserList()
                    } else {
                        Log.w("Fail", "createUserWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
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
                        Toast.makeText(
                            baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
    }

    fun showUserList() {
        val intent = Intent(applicationContext, UserListActivity::class.java)
        startActivity(intent)
    }

}