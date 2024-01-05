package com.example.terraforming

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
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
import com.google.android.material.card.MaterialCardView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import org.json.JSONObject
import java.lang.Exception
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private lateinit var btnShutter: AppCompatButton
    private lateinit var ivPicture: ImageView
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var cardYarn: MaterialCardView
    private lateinit var cardStarry: MaterialCardView
    private lateinit var cardDiorama: MaterialCardView


    private val TAG = "My debug"
    private var locationPermissionGranted = false
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

    private val openWeatherAPIKey = "6f24d2191ecfc9eb76a31cc11c8c1355"
    private val openAI = OpenAI(
        token = "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"
    )

    private var name = ""
    private var time = ""
    private var weather = "clear skies"
    private var getLocationFlag = false
    private var getWeatherFlag = false
    private var selectedStyle: MaterialCardView? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnShutter = findViewById(R.id.btnShutter)
        ivPicture = findViewById(R.id.ivPicture)
        cardYarn = findViewById(R.id.cardYarn)
        cardStarry = findViewById(R.id.cardStarry)
        cardDiorama = findViewById(R.id.cardDiorama)

        Places.initialize(applicationContext, "AIzaSyCciR3XilwS3krTEQDeqVYYiLE8zzc8x90")
        placesClient = Places.createClient(this)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)


        btnShutter.setOnClickListener {
            resetVariables()
            // TODO: make shutter button disappear, make loading button appear
            // TODO: at generate(), make loading button disappear, make shutter button appear
            Log.i(TAG, "Terraforming. Please wait.")
            getTime()
            Log.i(TAG, "Calling getLongLat()...")
            getLongLat(::getWeather)
            Log.i(TAG, "calling getLocation()...")
            getLocation(::generate)
        }

        cardYarn.setOnClickListener{
            if (selectedStyle == cardYarn){
                cardYarn.setChecked(false)
                selectedStyle = null
            }
            else{
                selectedStyle?.setChecked(false)
                selectedStyle = cardYarn
                cardYarn.setChecked(true)
            }
        }

        cardStarry.setOnClickListener{
            if (selectedStyle == cardStarry){
                cardStarry.setChecked(false)
                selectedStyle = null
            }
            else{
                selectedStyle?.setChecked(false)
                selectedStyle = cardStarry
                cardStarry.setChecked(true)
            }
        }

        cardDiorama.setOnClickListener{
            if (selectedStyle == cardDiorama){
                cardDiorama.setChecked(false)
                selectedStyle = null
            }
            else{
                selectedStyle?.setChecked(false)
                selectedStyle = cardDiorama
                cardDiorama.setChecked(true)
            }
        }
    }

    private fun getLongLat(getWeather: (lat: Double, long: Double) -> Unit) {
        Log.i(TAG, "getLongLat() called.")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    // Got last known location. In some rare situations, this can be null.
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        Log.i(TAG, "Lat = $latitude, Long= $longitude")
                        Log.i(TAG, "Calling getWeather()...")
                        getWeather(latitude, longitude)
                    }
                }
        } else {
            Log.i(TAG, "Error at getLongLat(), no location permission.")
            getLocationPermission()
        }
    }

    private fun getLocation(generate: () -> Unit) {
        Log.i(TAG, "getLocation() called.")


        // Use fields to define the data types to return.
        val placeFields: List<Place.Field> = listOf(Place.Field.NAME)

        // Use the builder to create a FindCurrentPlaceRequest.
        val request: FindCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)

        // Call findCurrentPlace and handle the response (first check that the user has granted permission).
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {

            val placeResponse = placesClient.findCurrentPlace(request)
            placeResponse.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val response = task.result
                    val firstPlaceLikelihood = response.placeLikelihoods.firstOrNull()
                    if (firstPlaceLikelihood != null) {
                        name = firstPlaceLikelihood.place.name
                        Log.i(TAG, "Location name is $name")
                        getLocationFlag = true
                        if (getLocationFlag && getWeatherFlag) {
                            Log.i(TAG, "Calling generate() from getLocation()...")
                            generate()
                        }
                    } else {
                        // location is null error
                        Log.i(TAG, "firstPlaceLikelihood is null")
                        val toast = Toast.makeText(
                            this,
                            "We cannot determine your current location. Please try again in a different location.",
                            Toast.LENGTH_LONG
                        )
                        toast.show()
                    }
                } else {
                    // call failed error
                    val exception = task.exception
                    if (exception is ApiException) {
                        Log.i(TAG, "Place not found: ${exception.statusCode}")
                    }
                    val toast = Toast.makeText(
                        this,
                        "We cannot determine your current location. Please try again later.",
                        Toast.LENGTH_LONG
                    )
                    toast.show()
                }
            }
        } else {
            getLocationPermission()
        }
    }

    private fun getWeather(lat: Double, long: Double) {
        Log.i(TAG, "getWeather() called.")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    "https://api.openweathermap.org/data/3.0/onecall?lat=${lat}&lon=${long}&units=metric&exclude=minutely,hourly,daily,alerts&appid=$openWeatherAPIKey"
                val resultJson = URL(url).readText()
                val jsonObject = JSONObject(resultJson)
                val jsonCurrent = jsonObject.getJSONObject("current")
                val jsonWeather = jsonCurrent.getJSONArray("weather")
                val jsonWeather0 = jsonWeather.getJSONObject(0)
                weather = jsonWeather0.getString("description")

                withContext(Dispatchers.Main) {
                    Log.i(TAG, "Weather is: $weather")
                    getWeatherFlag = true
                    if (getLocationFlag && getWeatherFlag) {
                        Log.i(TAG, "Calling generate() from getWeather()...")
                        generate()
                    }
                }
            } catch (e: Exception) {
                Log.i(TAG, "exception caught, ${e.localizedMessage}")
                getWeatherFlag = true
                if (getLocationFlag && getWeatherFlag) {
                    Log.i(TAG, "Calling generate() from getWeather() with no weather...")
                    generate()
                }
            }
        }
    }

    private fun generate() {
        Log.i(TAG, "generate() called.")

        val question = "dalle3: $name at $time, weather is broken clouds"
        Log.i(TAG, "Terraforming prompt: $question")
        CoroutineScope(Dispatchers.IO).launch {
            val images = openAI.imageURL( // or openAI.imageJSON
                creation = ImageCreation(
                    prompt = question,
                    model = ModelId("dall-e-2"),
                    n = 1,
                    size = ImageSize.is1024x1024
                )
            )
            withContext(Dispatchers.Main) {
                Log.i("Mine", images[0].url)
                val url = images[0].url
                Picasso.get().load(url).into(ivPicture)
            }
        }
    }

    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    private fun getTime() {
        Log.i(TAG, "Getting time...")
        val currentTime = Date()
        val formatter = SimpleDateFormat("h a", Locale.getDefault())
        time = formatter.format(currentTime)
        Log.i(TAG, "Current time is $time")
    }

    private fun resetVariables() {
        name = ""
        time = ""
        weather = "clear skies"
        getLocationFlag = false
        getWeatherFlag = false
    }
}
