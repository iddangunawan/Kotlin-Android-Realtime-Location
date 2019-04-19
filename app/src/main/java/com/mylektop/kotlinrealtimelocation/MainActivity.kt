package com.mylektop.kotlinrealtimelocation

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.mylektop.kotlinrealtimelocation.model.User
import com.mylektop.kotlinrealtimelocation.utils.Common
import io.paperdb.Paper
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val MY_REQUEST_CODE = 2323 // Any number you want
    }

    lateinit var userInformation: DatabaseReference
    lateinit var provider: List<AuthUI.IdpConfig>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init
        Paper.init(this)

        // Init Firebase
        userInformation = FirebaseDatabase.getInstance().getReference(Common.USER_INFORMATION)

        // Init provider
        provider = Arrays.asList<AuthUI.IdpConfig>(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Request Permission
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    showSignInOptions()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) {

                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    Toast.makeText(this@MainActivity, "You must accept this permission", Toast.LENGTH_SHORT).show()
                }
            }).check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MY_REQUEST_CODE) {
            val firebaseUser = FirebaseAuth.getInstance().currentUser

            // Check if user exists on database
            userInformation.orderByKey()
                .equalTo(firebaseUser!!.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(this@MainActivity, databaseError.message, Toast.LENGTH_SHORT).show()
                    }

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.value == null) {
                            // User not exists
                            if (!dataSnapshot.child(firebaseUser.uid).exists()) {
                                Common.loggedUser = User(firebaseUser.uid, firebaseUser.email!!)

                                // Add user to database
                                userInformation.child(Common.loggedUser.uid!!)
                                    .setValue(Common.loggedUser)
                            }
                        } else {
                            // User available
                            Common.loggedUser = dataSnapshot.child(firebaseUser.uid)
                                .getValue(User::class.java)!!
                        }

                        // Save UID to storage to update location from killed mode
                        Paper.book().write(Common.USER_UID_SAVE_KEY, Common.loggedUser.uid)
                        updateToken(firebaseUser)
                        setupUI()
                    }
                })
        }
    }

    private fun showSignInOptions() {
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(provider)
                .build(), MY_REQUEST_CODE
        )
    }

    private fun updateToken(firebaseUser: FirebaseUser?) {
        val tokens = FirebaseDatabase.getInstance()
            .getReference(Common.TOKENS)

        // Get Token
        FirebaseInstanceId.getInstance().instanceId
            .addOnSuccessListener { instanceIdResult ->
                tokens.child(firebaseUser!!.uid)
                    .setValue(instanceIdResult.token)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupUI() {
        // After all done ! Navigate Home
        startActivity(Intent(this@MainActivity, HomeActivity::class.java))
        finish()
    }
}
