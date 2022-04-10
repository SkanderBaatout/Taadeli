package com.skander.taadeli

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.skander.taadeli.models.DriverInfoModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import kotlinx.android.synthetic.main.activity_splash_screen.*
import java.util.*
import java.util.concurrent.TimeUnit

class SplashActivity : AppCompatActivity() {

    companion object {
        private val LOGIN_REQUESt_CODE = 4545

    }

    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener

    private lateinit var database: FirebaseDatabase
    private lateinit var driverInfoRef:DatabaseReference
    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    override fun onStop() {
        if (firebaseAuth != null && listener != null)
            firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }

    private fun delaySplashScreen() {
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                firebaseAuth.addAuthStateListener(listener)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        init()
    }

    private fun init() {
        database= FirebaseDatabase.getInstance()
        driverInfoRef=database.getReference(Common.DRIVER_INFO_REFERENCE)

        providers = Arrays.asList(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),

            )
        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { myFirebaseAuth ->
            val user = myFirebaseAuth.currentUser
            if (user != null) {
                checkUserFromFirebase()
            } else {
                showLoginLayout()
            }
        }
    }

    private fun checkUserFromFirebase() {
        driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()   ){
                 //   Toast.makeText(this@SplashActivity, "User already registered!!", Toast.LENGTH_SHORT).show()
                    val model =snapshot.getValue(DriverInfoModel::class.java)
                    goToHomeActivity(model)
                }else{
                    showRegisterLayout()
                }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SplashActivity, error.message, Toast.LENGTH_SHORT).show()
                }

            })
    }

    private fun goToHomeActivity(model: DriverInfoModel?) {
        Common.currentUser=model
        startActivity(Intent(this@SplashActivity,DriverHomeActivity::class.java))
        finish()
        }

    private fun showRegisterLayout() {
        val builder = AlertDialog.Builder(this,R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register,null)

        val ediFirstName=itemView.findViewById(R.id.edit_first_name) as TextInputEditText
        val editLastName=itemView.findViewById(R.id.edit_last_name) as TextInputEditText
        val editPhoneNumber=itemView.findViewById(R.id.edit_phone_number) as TextInputEditText

        val btnContinue=itemView.findViewById(R.id.btn_register) as Button

        //setData
        if (FirebaseAuth.getInstance().currentUser!!.phoneNumber !=null &&
                !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber))
                     editPhoneNumber.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)

       //view
        builder.setView(    itemView)
        val dialog=builder.create()
        dialog.show()

            //Event
        btnContinue.setOnClickListener {
            when {
                TextUtils.isDigitsOnly(ediFirstName.text.toString()) -> {
                    Toast.makeText(this,"Please enter first name.",Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                TextUtils.isDigitsOnly(editLastName.text.toString()) -> {
                    Toast.makeText(this,"Please enter last name.",Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                TextUtils.isDigitsOnly(editPhoneNumber.text.toString()) -> {
                    Toast.makeText(this,"Please enter phone number.",Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                else -> {
                    val model =DriverInfoModel()
                    model.first_name=ediFirstName.text.toString()
                    model.last_name=editLastName.text.toString()
                    model.phone_number=editPhoneNumber.text.toString()

                    model.rating=0.0
                    driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                        .setValue(model)
                        .addOnFailureListener{
                                e->
                            Toast.makeText(this,""+e.message ,Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            progress_bar.visibility=View.GONE
                        }
                        .addOnSuccessListener {
                            Toast.makeText(this,"Registered successfully!",Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            goToHomeActivity(model)
                            progress_bar.visibility=View.GONE
                        }
                }
            }


        }
    }

    private fun showLoginLayout() {
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .setGoogleButtonId(R.id.btn_google_sign_in)
            .build()
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build(), LOGIN_REQUESt_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Activity.RESULT_OK) {


            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
            } else {
                Toast.makeText(this, "" + response!!.error!!.message, Toast.LENGTH_SHORT).show()
            }

        }
    }
}