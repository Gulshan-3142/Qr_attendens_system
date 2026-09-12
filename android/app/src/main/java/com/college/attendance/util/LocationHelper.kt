//package com.college.attendance.util
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.location.Location
//import android.os.Build
//import com.google.android.gms.location.FusedLocationProviderClient
//import com.google.android.gms.location.LocationServices
//import com.google.android.gms.location.Priority
//import com.google.android.gms.tasks.CancellationTokenSource
//import kotlinx.coroutines.suspendCancellableCoroutine
//import kotlin.coroutines.resume
//
//data class DeviceLocationResult(
//    val latitude: Double,
//    val longitude: Double,
//    val accuracy: Float,
//    val isMockLocation: Boolean,
//    val timestamp: Long
//)
//
//class LocationHelper(private val context: Context) {
//
//    private val fusedLocationClient: FusedLocationProviderClient =
//        LocationServices.getFusedLocationProviderClient(context)
//
//    @SuppressLint("MissingPermission")
//    suspend fun getCurrentLocation(): Result<DeviceLocationResult> =
//        suspendCancellableCoroutine { continuation ->
//            val cts = CancellationTokenSource()
//
//            continuation.invokeOnCancellation {
//                cts.cancel()
//            }
//
//            fusedLocationClient.getCurrentLocation(
//                Priority.PRIORITY_HIGH_ACCURACY,
//                cts.token
//            ).addOnSuccessListener { location: Location? ->
//                if (location != null) {
//                    val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                        location.isMock
//                    } else {
//                        @Suppress("DEPRECATION")
//                        location.isFromMockProvider
//                    }
//
//                    continuation.resume(
//                        Result.success(
//                            DeviceLocationResult(
//                                latitude = location.latitude,
//                                longitude = location.longitude,
//                                accuracy = location.accuracy,
//                                isMockLocation = isMock,
//                                timestamp = location.time
//                            )
//                        )
//                    )
//                } else {
//                    continuation.resume(
//                        Result.failure(Exception("Unable to acquire GPS fix. Please ensure Location Services are turned on."))
//                    )
//                }
//            }.addOnFailureListener { exception ->
//                continuation.resume(Result.failure(exception))
//            }
//        }
//}
package com.college.attendance.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class DeviceLocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val isMockLocation: Boolean,
    val timestamp: Long
)

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<DeviceLocationResult> =
        suspendCancellableCoroutine { continuation ->

            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1000L
            )
                .setMinUpdateIntervalMillis(500L)
                .setWaitForAccurateLocation(true)
                .setMaxUpdates(5)
                .build()

            val callback = object : LocationCallback() {

                override fun onLocationResult(
                    result: LocationResult
                ) {
                    val locations = result.locations

                    if (locations.isEmpty()) {
                        return
                    }

                    // Best location = smallest accuracy value
                    val location = locations.minByOrNull {
                        it.accuracy
                    } ?: return

                    val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        location.isMock
                    } else {
                        @Suppress("DEPRECATION")
                        location.isFromMockProvider
                    }

                    fusedLocationClient.removeLocationUpdates(this)

                    if (continuation.isActive) {
                        continuation.resume(
                            Result.success(
                                DeviceLocationResult(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    accuracy = location.accuracy,
                                    isMockLocation = isMock,
                                    timestamp = location.time
                                )
                            )
                        )
                    }
                }
            }

            continuation.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(callback)
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                context.mainLooper
            ).addOnFailureListener { exception ->

                fusedLocationClient.removeLocationUpdates(callback)

                if (continuation.isActive) {
                    continuation.resume(
                        Result.failure(exception)
                    )
                }
            }
        }
}