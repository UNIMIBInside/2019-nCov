package events.pandemic.covid19

import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.util.*

class BackgroundLocationWorker(appContext: Context, workerParams: WorkerParameters) :
        Worker(appContext, workerParams) {
    private var locationCallback: LocationCallback? = null
    private var records = ArrayList<Location>()

    companion object {
        // 5 minutes
        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 5 * 60 * 1000
        private const val LOG_TAG = "BgWorker"
    }

    override fun doWork(): Result {
        Log.d(LOG_TAG, "Hello from Worker!")
/*
        if (locationCallback == null) {
            Log.d(LOG_TAG, "initialize locationCallback")
            var locationRequest = LocationRequest()
            locationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS * 3
            locationRequest.fastestInterval = UPDATE_INTERVAL_IN_MILLISECONDS
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

            locationCallback = object: LocationCallback() {
                override fun onLocationResult(result: LocationResult?) {
                    result ?: return
                    for (location in result.locations) {
                        // TODO: do something
                        val timestamp = System.currentTimeMillis()
                        val date_str = Date(timestamp).toString()
                        Log.d(LOG_TAG, "${date_str} ${location.latitude} ${location.longitude}")
                    }
                }
            }

            Looper.myLooper() ?: Looper.prepare()
            LocationServices.getFusedLocationProviderClient(applicationContext)
                    .requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        }
 */

        LocationServices.getFusedLocationProviderClient(applicationContext)
                .lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                records.add(location)
                Log.d(LOG_TAG, "${records.size} ${location.latitude} ${location.longitude}")
            }
        }

        return Result.success();
    }


}