package edu.galileo.innovation.hexapod.viewer

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.ImageView
import android.util.Log

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import android.widget.Button
import android.widget.Toast


class MainActivity : Activity() {
    private val TAG = "MainActivity"

    private lateinit var imageView: ImageView

    private lateinit var firebaseStorage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        imageView = findViewById(R.id.imageView)

        firebaseStorage = FirebaseStorage.getInstance()

        val newest = FirebaseDatabase.getInstance().reference.child("newest")
        val state = FirebaseDatabase.getInstance().reference.child("state")

        val imagesListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val imageUrl = dataSnapshot.child("url").value.toString()
                val imageRef = firebaseStorage.getReferenceFromUrl(imageUrl)

                Log.w(TAG, imageUrl)

                GlideApp.with(applicationContext)
                        .load(imageRef)
                        .into(imageView)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "imagesListener.onCancelled() >>>", databaseError.toException())
            }
        }
        newest.addValueEventListener(imagesListener)

        val buttonStart = findViewById<Button>(R.id.buttonStart)
        val buttonStop = findViewById<Button>(R.id.buttonStop)

        buttonStart.setOnClickListener{
            val update = HashMap<String,Any>()
            update["state"] = "started"
            state.updateChildren(update)
            Toast.makeText(this@MainActivity, "Taking photos!", Toast.LENGTH_SHORT).show()
        }

        buttonStop.setOnClickListener {
            val update = HashMap<String,Any>()
            update["state"] = "stopped"
            state.updateChildren(update)
            Toast.makeText(this@MainActivity, "Not taking photos anymore.", Toast.LENGTH_SHORT).show()
        }
    }

    /*
        Check this out in case of memory problems, specially after closing the app.
     */
    /*
    override fun onDestroy() {
        super.onDestroy()
        GlideApp.with(applicationContext).pauseRequests()
    }
    */
}
