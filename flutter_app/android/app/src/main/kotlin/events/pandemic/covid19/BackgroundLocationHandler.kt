package events.pandemic.covid19

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.*
import events.pandemic.covid19.utils.SessionManager
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import java.util.concurrent.TimeUnit

class BackgroundLocationHandler(private var engine: FlutterEngine, private var context: Context, private var channel: MethodChannel) : MethodChannel.MethodCallHandler {
    private var operation: Operation? = null
    private var sessionManager: SessionManager

    companion object {
        private const val CHANNEL_NAME = "events.pandemic.covid19/background_location"

        // There is a 23 character limit for LOG TAG.
        private const val LOG_TAG = "covid19.BgHandler"
        private const val WORKER_TAG = "covid19.BgHandler.Worker"

        // This is just a random number.
        private const val PERMISSION_REQUEST_CODE = 0xb352a

        @JvmStatic
        fun registerWith(engine: FlutterEngine, context: Context) {
            val channel = MethodChannel(
                    engine.dartExecutor.binaryMessenger,
                    CHANNEL_NAME
            )
            channel.setMethodCallHandler(BackgroundLocationHandler(engine, context, channel))
        }
    }

    init {
        this.sessionManager = SessionManager(context)
    }

    private fun buildNotification(): Notification? {
        val builder = NotificationCompat.Builder(context, CHANNEL_NAME)
                .setContentTitle("Background service is running")
                .setOngoing(true)
                .setSound(null)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                /* .setSmallIcon(R.drawable.navigation_empty_icon) */
                .setWhen(System.currentTimeMillis())
        return builder.build()
    }

    private fun setNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // TODO: make this a proper variable
            val name = "covid19"
            val mChannel = NotificationChannel(CHANNEL_NAME, name, NotificationManager.IMPORTANCE_LOW)
            mChannel.setSound(null, null)
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    private fun checkPermissions() {
        val permissionAccessCoarseLocationApproved = ActivityCompat
                .checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (permissionAccessCoarseLocationApproved) {
            /*
            val backgroundLocationPermissionApproved = ActivityCompat
                    .checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

            if (backgroundLocationPermissionApproved) {
                // App can access location both in the foreground and in the background.
                // Start your service that doesn't have a foreground service type
                // defined.
                // return true
            } else {
                // App can only access location in the foreground. Display a dialog
                // warning the user that your app must have all-the-time access to
                // location in order to function properly. Then, request background
                // location.
                ActivityCompat.requestPermissions(context as Activity,
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        PERMISSION_REQUEST_CODE
                )
            }
             */
        } else {
            // App doesn't have access to the device's location at all. Make full request
            // for permission.
            ActivityCompat.requestPermissions(context as Activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION
                            /* Manifest.permission.ACCESS_BACKGROUND_LOCATION */),
                    PERMISSION_REQUEST_CODE
            )
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    private fun startListening() {

        checkPermissions()

        if (getStatus())
            return

        setNotification()

        // Periodic work has a minimum interval of 15 minutes.
        val workRequest =
                PeriodicWorkRequestBuilder<BackgroundLocationWorker>(
                        PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                        .addTag(WORKER_TAG).build()
        // TODO: change "REPLACE" to "KEEP" in production
        operation = WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORKER_TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest)
        val uuid = workRequest.stringId
        sessionManager.setStringData("worker_id", uuid)
    }

    private fun stopListening() {
        if (!getStatus()) return

        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(WORKER_TAG)
        workManager.pruneWork()

        sessionManager.setStringData("worker_id", "")
    }

    private fun getStatus(): Boolean {
        val worker_id = sessionManager.getStringData("worker_id")
        return (worker_id != null) && worker_id.isNotEmpty()

//        var statuses = WorkManager.getInstance(context).getWorkInfosByTag(WORKER_TAG).get()
//        if (statuses == null)
//            return false
//        for (status in statuses) {
//            if (status.state == WorkInfo.State.RUNNING || status.state == WorkInfo.State.ENQUEUED) {
//                return true
//            }
//        }
//        return false
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        // Note that, this method is executed in the "main thread".
        when (call.method) {
            "start" -> {
                Log.d(LOG_TAG, "starting!")

                // TODO: check permission
                startListening()
            }

            "stop" -> {
                Log.d(LOG_TAG, "stopped!")

                stopListening()
            }

            "status" -> {
                Log.d(LOG_TAG, "get status")

                result.success(getStatus())
            }

            else -> {
                result.notImplemented()
            }
        }
    }
}