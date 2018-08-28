package edu.galileo.innovation.hexapod.viewer

// Android
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.widget.ImageView
import android.util.Log
import android.widget.Button
import android.widget.Toast
import android.view.View
// Firebase
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
// Network
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
// Files
import java.io.FileOutputStream
import java.io.File

const val TAG = "MainActivity"
const val USING_FIREBASE = true

class MainActivity : Activity() {
    private lateinit var imageView: ImageView

    private lateinit var firebaseStorage: FirebaseStorage

    // Streaming
    private lateinit var socket: DatagramSocket
    private var address = InetAddress.getByName("10.10.10.1")
    private var port = 4445

    // Streaming handler
    private lateinit var handlerThread: HandlerThread
    private lateinit var looper: Looper
    private lateinit var handler: Handler

    private var firstPicture = true

    // Buttons
    private lateinit var buttonStart: Button
    private lateinit var buttonStop: Button
    private lateinit var buttonForward: Button
    private lateinit var buttonLeft: Button
    private lateinit var buttonRight: Button
    private lateinit var buttonStand: Button
    private lateinit var buttonStore: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        imageView = findViewById(R.id.imageView)

        firebaseStorage = FirebaseStorage.getInstance()

        val newest = FirebaseDatabase.getInstance().reference.child("newest")
        val cameraState = FirebaseDatabase.getInstance().reference.child("camera")
        val moveState = FirebaseDatabase.getInstance().reference.child("movement")

        // Streaming handler
        handlerThread = HandlerThread("Streaming Handler")
        handlerThread.start()
        looper = handlerThread.looper
        handler = Handler(looper)

        val imagesListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                /* Image */
                val imageUrl = dataSnapshot.child("url").value.toString()
                val imageRef = firebaseStorage.getReferenceFromUrl(imageUrl)

                Log.d(TAG, imageUrl)

                GlideApp.with(applicationContext)
                        .load(imageRef)
                        .into(imageView)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "imagesListener.onCancelled() >>>", databaseError.toException())
            }
        }
        if (USING_FIREBASE) {
            newest.addValueEventListener(imagesListener)
        }

        buttonStart = findViewById(R.id.buttonStart)
        buttonStop = findViewById(R.id.buttonStop)

        buttonForward = findViewById(R.id.buttonForward)
        buttonLeft = findViewById(R.id.buttonLeft)
        buttonRight = findViewById(R.id.buttonRight)
        buttonStand = findViewById(R.id.buttonStand)
        buttonStore = findViewById(R.id.buttonStore)

        buttonStart.setOnClickListener {
            val update = HashMap<String, Any>()
            update["state"] = "started"
            cameraState.updateChildren(update)
            //Toast.makeText(this@MainActivity, "Taking photos!", Toast.LENGTH_SHORT).show()
        }

        buttonStop.setOnClickListener {
            val update = HashMap<String, Any>()
            update["state"] = "stopped"
            cameraState.updateChildren(update)
            //Toast.makeText(this@MainActivity, "Not taking photos anymore.", Toast.LENGTH_SHORT).show()
        }

        buttonForward.setOnClickListener {
            val update = HashMap<String, Any>()
            update["state"] = "forward"
            moveState.updateChildren(update)
        }

        buttonLeft.setOnClickListener {
            val update = HashMap<String, Any>()
            update["state"] = "left"
            moveState.updateChildren(update)
        }

        buttonRight.setOnClickListener {
            val update = HashMap<String, Any>()
            update["state"] = "right"
            moveState.updateChildren(update)
        }

        buttonStand.setOnClickListener {
            val update = HashMap<String, Any>()
            update["state"] = "stand"
            moveState.updateChildren(update)
        }

        buttonStore.setOnClickListener {
            val update = HashMap<String, Any>()
            update["state"] = "store"
            moveState.updateChildren(update)
        }

        //val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        //StrictMode.setThreadPolicy(policy)

        val path = Environment.getExternalStorageDirectory()
        val myFile = File(path, "myFile.png")
        myFile.createNewFile()
        Log.w(TAG, "Path: " + myFile.absolutePath)





        /**
                anko doAsync and uiThread

                DIDN'T WORK
         **/

        doAsync {
            // First message
            socket = DatagramSocket()
            val message = "LOCAL"
            val buffer = message.toByteArray()
            val packet = DatagramPacket(buffer, buffer.size, address, port)
            socket.send(packet)
            Log.i(TAG, "First packet sent to robot!")

            // Buffer to receive
            var recBuffer = ByteArray(60000)
            var recPacket = DatagramPacket(recBuffer, recBuffer.size)

            while (true) {
                Log.v("loop", "Running...")

                // Receive a packet
                socket.receive(recPacket)
                address = recPacket.address
                port = recPacket.port

                /**
                        After this point ALMOST no uiThread action works
                        socket.receive() causes ALMOST everything to fail afterwards

                        Horrible workaround: Anko toast
                        Perform at least one toast and it magically works!
                 **/

                Log.i("PR", "Got a datagram!")
                Log.i("PR", "Length: ${recPacket.length} - Port: $port - Address: $address")

                hideLeft()
                uiThread {
                    toast("Starting...")
                }
                hideRight()

                val bmp = BitmapFactory.decodeByteArray(recPacket.data, 0, recPacket.length)

                if (firstPicture) {

                    /**
                            Horrible workaround: Anko toast
                            Perform one toast, after that any other uiThread action will work.

                            GET FEEDBACK ON THIS: Why does this works like that!?
                     **/
                    hideLeft()
                    uiThread {
                        toast("Starting...")
                    }
                    hideRight()

                    var out: FileOutputStream? = null
                    try {
                        out = FileOutputStream(myFile, false)
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.flush()

                        Log.i(TAG, "File successfully saved!")
                        firstPicture = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            out!!.close()
                            Log.i(TAG, "File successfully closed!")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                uiThread {
                    Log.i("anko uiThread", "Before setImageBitmap")
                    imageView.setImageBitmap(bmp)
                    Log.i("anko uiThread", "After setImageBitmap")

                    Log.d(TAG, "bmp height: ${bmp.height} - bmp width: ${bmp.width}")
                    Log.d(TAG, "imageView height: ${imageView.height} - imageView width: ${imageView.width} - imageView visibility: ${imageView.visibility}")
                }
            } // while(true)
        } // doAsync{}


        /**
                Thread

                DIDN'T WORK
         **/
        /*
        val packetListenerThread = Thread(Runnable {
            // First message
            socket = DatagramSocket()
            val message = "LOCAL"
            val buffer = message.toByteArray()
            val packet = DatagramPacket(buffer, buffer.size, address, port)
            socket.send(packet)
            Log.i(TAG, "First packet sent to robot!")

            // Buffer to receive
            var recBuffer = ByteArray(60000)
            var recPacket = DatagramPacket(recBuffer, recBuffer.size)

            while (true) {
                Log.v("loop", "Running...")

                // TEST: hide one of the buttons before receive()
                hideRight()

                // Receive a packet
                socket.receive(recPacket)
                address = recPacket.address
                port = recPacket.port

                /**
                        AFTER THIS POINT NO UI THREAD ACTION WORKS
                        socket.receive() CAUSES EVERYTHING TO FAIL AFTERWARDS
                 **/

                // TEST: hide one of the buttons after receive()
                hideLeft()

                Log.i("PR", "Got a datagram!")
                Log.i("PR", "Length: ${recPacket.length} - Port: $port - Address: $address")

                val bmp = BitmapFactory.decodeByteArray(recPacket.data, 0, recPacket.length)

                if (firstPicture) {
                    var out: FileOutputStream? = null
                    try {
                        out = FileOutputStream(myFile, false)
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.flush()

                        Log.i(TAG, "File successfully saved!")
                        firstPicture = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            out!!.close()
                            Log.i(TAG, "File successfully closed!")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                /*
                    Didn't work
                 */
                this@MainActivity.runOnUiThread {
                    Log.i("runOnUiThread", "Before setImageBitmap")
                    imageView.setImageBitmap(bmp)
                    Log.i("runOnUiThread", "After setImageBitmap")

                    Log.d(TAG, "bmp height: ${bmp.height} - bmp width: ${bmp.width}")
                    Log.d(TAG, "imageView height: ${imageView.height} - imageView width: ${imageView.width} - imageView visibility: ${imageView.visibility}")
                }

                /*
                    Didn't work
                 */
                /*
                runOnUiThread {
                    Log.i("runOnUiThread, "Before GlideApp.with()")
                    GlideApp.with(applicationContext).load(bmp).into(imageView)
                    Log.i("runOnUiThread", "After GlideApp.with()")

                    Log.d(TAG, "bmp height: ${bmp.height} - bmp width: ${bmp.width}")
                    Log.d(TAG, "imageView height: ${imageView.height} - imageView width: ${imageView.width} - imageView visibility: ${imageView.visibility}")
                }
                */

                /*
                    Didn't work
                 */
                /*
                imageView.post(Runnable {
                    Log.i("imageView post method, "Before setImageBitmap")
                    imageView.setImageBitmap(bmp)
                    Log.i("imageView post method", "After setImageBitmap")

                    Log.d(TAG, "bmp height: ${bmp.height} - bmp width: ${bmp.width}")
                    Log.d(TAG, "imageView height: ${imageView.height} - imageView width: ${imageView.width} - imageView visibility: ${imageView.visibility}")
                })
                */
            }
        })
        packetListenerThread.start()
        */
    }

    private fun hideLeft() {
        runOnUiThread {
            Log.i(TAG, "!!! antes de ocultar izquierdo")
            buttonLeft.visibility = View.INVISIBLE
            Log.i(TAG, "!!! despues de ocultar izquierdo")
        }
    }

    private fun hideRight() {
        runOnUiThread {
            Log.i(TAG, "!!! antes de ocultar derecho")
            buttonRight.visibility = View.INVISIBLE
            Log.i(TAG, "!!! despues de ocultar derecho")
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

