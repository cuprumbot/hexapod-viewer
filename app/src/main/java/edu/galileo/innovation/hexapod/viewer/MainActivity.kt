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
        val cameraState = FirebaseDatabase.getInstance().reference.child("camera")
        val moveState = FirebaseDatabase.getInstance().reference.child("movement")

        /*
        val annotationsListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val keys = dataSnapshot.value
                Log.d(TAG, "=====> " + keys + "<=====")

                /*
                    TO DO
                    THIS GETS THE ANNOTATIONS, BUT SEVERAL SECONDS LATER
                    SAVE THE LATEST ANNOTATIONS SOMEWHERE ELSE AND FETCH THAT DATA
                 */
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        }
        */

        val imagesListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                /* Image */
                val imageUrl = dataSnapshot.child("url").value.toString()
                val imageRef = firebaseStorage.getReferenceFromUrl(imageUrl)

                Log.d(TAG, imageUrl)
                GlideApp.with(applicationContext)
                        .load(imageRef)
                        .into(imageView)

                /* Annotations */
                /*
                val name = dataSnapshot.child("name").value.toString()
                val annotations = FirebaseDatabase.getInstance().reference.child("logs").child(name).child("annotations")

                annotations.addListenerForSingleValueEvent(annotationsListener)

                Log.i(TAG, "Annotations:")
                Log.i(TAG, annotations.toString())
                /*
                    TO DO
                    THIS DOESN'T GET THE ANNOTATIONS, JUST THE URL
                    TO GET THE ANNOTATIONS, THE LISTENER IS USED
                 */
                */
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "imagesListener.onCancelled() >>>", databaseError.toException())
            }
        }
        newest.addValueEventListener(imagesListener)

        val buttonStart = findViewById<Button>(R.id.buttonStart)
        val buttonStop = findViewById<Button>(R.id.buttonStop)

        val buttonForward = findViewById<Button>(R.id.buttonForward)
        val buttonLeft = findViewById<Button>(R.id.buttonLeft)
        val buttonRight = findViewById<Button>(R.id.buttonRight)
        val buttonStand = findViewById<Button>(R.id.buttonStand)
        val buttonStore = findViewById<Button>(R.id.buttonStore)

        buttonStart.setOnClickListener{
            val update = HashMap<String,Any>()
            update["state"] = "started"
            cameraState.updateChildren(update)
            Toast.makeText(this@MainActivity, "Taking photos!", Toast.LENGTH_SHORT).show()
        }

        buttonStop.setOnClickListener {
            val update = HashMap<String,Any>()
            update["state"] = "stopped"
            cameraState.updateChildren(update)
            Toast.makeText(this@MainActivity, "Not taking photos anymore.", Toast.LENGTH_SHORT).show()
        }

        buttonForward.setOnClickListener{
            val update = HashMap<String,Any>()
            update["state"] = "forward"
            moveState.updateChildren(update)
        }

        buttonLeft.setOnClickListener{
            val update = HashMap<String,Any>()
            update["state"] = "left"
            moveState.updateChildren(update)
        }

        buttonRight.setOnClickListener{
            val update = HashMap<String,Any>()
            update["state"] = "right"
            moveState.updateChildren(update)
        }

        buttonStand.setOnClickListener{
            val update = HashMap<String,Any>()
            update["state"] = "stand"
            moveState.updateChildren(update)
        }

        buttonStore.setOnClickListener{
            val update = HashMap<String,Any>()
            update["state"] = "store"
            moveState.updateChildren(update)
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
