package events.pandemic.covid19

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices

class BackgroundLocationWorker(appContext: Context, workerParams: WorkerParameters) :
        Worker(appContext, workerParams) {
    // private var registeredEventSink = HashMap<String, EventChannel.EventSink>()

    companion object {
        // private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 5 * 60 * 1000
        private const val LOG_TAG = "BgWorker"
        // private const val CHANNEL_NAME = "events.pandemic.covid19/background_location"
    }

    private fun invokeCallback(location: Location) {
        // val locationAndTimestamp = LocationAndTimestamp(location)
        val locationAndTimestamp = HashMap<String, Double>()
        locationAndTimestamp["latitude"] = location.latitude
        locationAndTimestamp["longitude"] = location.longitude
        locationAndTimestamp["altitude"] = location.altitude
        locationAndTimestamp["accuracy"] = location.accuracy.toDouble()
        locationAndTimestamp["speed"] = location.speed.toDouble()
        locationAndTimestamp["time"] = System.currentTimeMillis().toDouble()

        // "?." means that call the member function only if the reference is not null.
        BackgroundLocationHandler.instance.getRegisteredEventSink()?.success(locationAndTimestamp)
    }

    override fun doWork(): Result {
        Log.d(LOG_TAG, "Hello from Worker!")

        LocationServices.getFusedLocationProviderClient(applicationContext)
                .lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                Log.d(LOG_TAG, "${location.latitude} ${location.longitude}")

                // records.add(location)
                invokeCallback(location)
            }
        }

        return Result.success();
    }
}

class LocationAndTimestamp {
    private var latitude: Double
    private var longitude: Double
    private var altitude: Double
    private var accuracy: Float
    private var speed: Float
    private var time: Long

    constructor(location: Location) {
        this.latitude = location.latitude
        this.longitude = location.longitude
        this.altitude = location.altitude
        this.accuracy = location.accuracy
        this.speed = location.speed
        this.time = System.currentTimeMillis()
    }
}
