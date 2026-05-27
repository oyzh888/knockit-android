package com.knockit.app.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android equivalent of the iOS CoreLocation one-shot location request.
 * Uses FusedLocationProviderClient for battery-efficient GPS.
 *
 * Required permissions (must be granted before calling [requestLocation]):
 *   ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION
 */
object LocationService {

    /**
     * Request the current device location.
     *
     * @param context Any context; no Activity required.
     * @return The best available [Location].
     * @throws LocationException if permissions are missing or the request fails.
     */
    @Throws(LocationException::class)
    suspend fun requestLocation(context: Context): Location {
        // Check permissions
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            throw LocationException("Location permission not granted")
        }

        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()

        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { cts.cancel() }

            val priority = if (fineGranted) Priority.PRIORITY_HIGH_ACCURACY
            else Priority.PRIORITY_BALANCED_POWER_ACCURACY

            val request = CurrentLocationRequest.Builder()
                .setPriority(priority)
                .setMaxUpdateAgeMillis(30_000L)   // accept a cached fix up to 30 s old
                .build()

            client.getCurrentLocation(request, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        if (cont.isActive) cont.resume(location)
                    } else {
                        // FusedLocationProvider returned null — fall back to last known
                        client.lastLocation
                            .addOnSuccessListener { last ->
                                if (last != null) {
                                    if (cont.isActive) cont.resume(last)
                                } else {
                                    if (cont.isActive)
                                        cont.resumeWithException(
                                            LocationException("Location unavailable — GPS may be disabled")
                                        )
                                }
                            }
                            .addOnFailureListener { e ->
                                if (cont.isActive)
                                    cont.resumeWithException(LocationException("Location failed: ${e.message}", e))
                            }
                    }
                }
                .addOnFailureListener { e ->
                    if (cont.isActive)
                        cont.resumeWithException(LocationException("Location request failed: ${e.message}", e))
                }
        }
    }
}

class LocationException(message: String, cause: Throwable? = null) : Exception(message, cause)
