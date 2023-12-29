package com.example.terraforming

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private lateinit var btnAsk: Button
    private lateinit var ivPicture: ImageView

    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val TAG = "My debug"

    private var locationPermissionGranted = false
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private var name = ""
    private var time = ""
    val openAI = OpenAI(
        token = "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"
    )

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnAsk = findViewById(R.id.btnAsk)
        ivPicture = findViewById(R.id.ivPicture)



        btnAsk.setOnClickListener {

            Log.i(TAG, "Terraforming. Please wait.")

            getLocation(::onSuccess)

            // CoroutineScope tied to the lifecycle of the activity with Main dispatcher for UI updates

        }
    }

    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    private fun getLocation(function1: (String) -> Unit){
        Log.i(TAG,"Getting location...")
        Places.initialize(applicationContext, "AIzaSyCciR3XilwS3krTEQDeqVYYiLE8zzc8x90")
        placesClient = Places.createClient(this)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)


        // Use fields to define the data types to return.
        val placeFields: List<Place.Field> = listOf(Place.Field.NAME)

        // Use the builder to create a FindCurrentPlaceRequest.
        val request: FindCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)

        // Call findCurrentPlace and handle the response (first check that the user has granted permission).
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {

            val placeResponse = placesClient.findCurrentPlace(request)
            placeResponse.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val response = task.result
                    val firstPlaceLikelihood = response.placeLikelihoods.firstOrNull()
                    if (firstPlaceLikelihood != null){
                        Log.i(TAG, "firstPlaceLikelihood is not null")
                        name = firstPlaceLikelihood.place.name
                        Log.i(TAG, "Location name is ${name}")
                        function1(name)
                    }
                    else{
                        Log.i(TAG, "firstPlaceLikelihood is null")
                    }

                } else {
                    val exception = task.exception
                    if (exception is ApiException) {
                        Log.e(TAG, "Place not found: ${exception.statusCode}")
                    }
                }
            }
        } else {
            // A local method to request required permissions;
            getLocationPermission()
        }
    }

    private fun onSuccess(placeName: String){
        getTime()
        val question = "dalle3: ${name} at $time, rainy weather"
        Log.i(TAG, "Terraforming prompt: ${question}")
        CoroutineScope(Dispatchers.Main).launch {
            // Use withContext to switch to IO dispatcher for network call
            val images = openAI.imageURL( // or openAI.imageJSON
                creation = ImageCreation(
                    prompt = question,
                    model = ModelId("dall-e-2"),
                    n = 1,
                    size = ImageSize.is1024x1024
                )
            )
            Log.i("Mine", images[0].url)
            var url = images[0].url
            Picasso.get().load(url).into(ivPicture)
        }
    }

    private fun getTime(){
        Log.i(TAG,"Getting time...")
        val currentTime = Date()
        val formatter = SimpleDateFormat("h a", Locale.getDefault())
        time = formatter.format(currentTime)
        Log.i(TAG,"Current time is " + time)
    }
}