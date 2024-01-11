package com.example.terraforming


import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
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
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomsheet.BottomSheetDialog
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
    private lateinit var btnLocation: AppCompatButton
    private lateinit var btnFilters: AppCompatButton
    private lateinit var ivPicture: ImageView
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var tvPrompt: TextView
    private lateinit var anim: LottieAnimationView

    private lateinit var cardYarn: MaterialCardView
    private lateinit var cardStarry: MaterialCardView
    private lateinit var cardDiorama: MaterialCardView
    private lateinit var cardWatercolour: MaterialCardView
    private lateinit var cardTapestry: MaterialCardView
    private lateinit var cardCartoon: MaterialCardView
    private lateinit var cardClay: MaterialCardView
    private lateinit var cardArtbook: MaterialCardView

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
    private var typeOfPlace = ""
    private var getLocationFlag = false
    private var getWeatherFlag = false
    private var selectedStyle: MaterialCardView? = null

    private var bottomSheetDialog: BottomSheetDialog? = null
    private lateinit var bottomSheetView: View

    private val placeArray = arrayListOf<Place>()
    private var locationButton = false


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnShutter = findViewById(R.id.btnShutter)
        btnLocation = findViewById(R.id.btnLocation)
        btnFilters = findViewById(R.id.btnFilters)
        ivPicture = findViewById(R.id.ivPicture)
        tvPrompt = findViewById(R.id.tvPrompt)
        anim = findViewById(R.id.anim)

        Places.initializeWithNewPlacesApiEnabled (applicationContext, "AIzaSyCciR3XilwS3krTEQDeqVYYiLE8zzc8x90")
        placesClient = Places.createClient(this)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_horizontal_scroll, null)

        getLocation(::generate)

        btnShutter.setOnClickListener {
            resetVariables()
            btnShutter.setVisibility(View.INVISIBLE)
            anim.setVisibility(View.VISIBLE)
            anim.playAnimation()
            // TODO: at generate(), make loading button disappear, make shutter button appear
            Log.i(TAG, "Terraforming. Please wait.")
            getTime()
            Log.i(TAG, "Calling getLongLat()...")
            getLongLat(::getWeather)
            Log.i(TAG, "calling getLocation()...")
            getLocation(::generate)
        }

        btnLocation.setOnClickListener {
            showBottomSheetDialog()
        }

        btnFilters.setOnClickListener{
            showOrInitBottomSheetHorizontal()
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
            tvPrompt.text = tvPrompt.text.toString() + "Error at getLongLat(), no location permission."
            getLocationPermission()
        }
    }

    private fun getLocation(generate: () -> Unit) {
        Log.i(TAG, "getLocation() called.")


        // Use fields to define the data types to return.
        val placeFields: List<Place.Field> = listOf(Place.Field.NAME, Place.Field.TYPES)

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
                    var count = 0
                    // todo: handle response array being empty
                    for (placeLikelihood: PlaceLikelihood in response?.placeLikelihoods ?: emptyList()) {
                        if (count >5){
                            break
                        }
                        Log.i(TAG, "Place '${placeLikelihood.place.name}' has likelihood: ${placeLikelihood.likelihood}")
                        placeArray.add(placeLikelihood.place)
                        count ++
                    }
                    for (place: Place in placeArray){
                        Log.i(TAG, "Name is ${place.name}, type is ${place.getPlaceTypes().get(0)}")
                    }
                    getLocationFlag = true
                    if (getLocationFlag && getWeatherFlag) {
                        Log.i(TAG, "Calling generate() from getLocation()...")
                        generate()
                    }
                } else {
                    // call failed error
                    val exception = task.exception
                    if (exception is ApiException) {
                        Log.i(TAG, "Place not found: ${exception.statusCode}")
                        tvPrompt.text = tvPrompt.text.toString() + "Place not found: ${exception.statusCode}"
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
        var question = "A photo taken at $name, a $typeOfPlace, weather being $weather, at $time."

        when (selectedStyle){
            cardYarn -> question = "2d scene of $name (a $typeOfPlace) at $time, weather being $weather, entirely crafted from yarn. If $name features mountains, beaches, urban settings, or forests, they are rendered with yarn in colors and textures that mimic the real environment. For mountains, use shades of gray yarn, silver yarn and white yarn; for beaches, the sands are to be shades of yellow yarn and sea shells adorning the shoreline and blue and white yarn for the waves; for urban scenes, use green yellow white black and brown yarn; and for forests, vibrant green and brown yarns. If $name includes water bodies like rivers, lakes, or seas, they are shown with flowing yarn in hues of blue and white, simulating the movement of water. The sky overhead, whether clear, cloudy, or starry, is depicted with yarn in colors that suit the time of day or weather conditions at $name. Flora and fauna, if present in $name, are carefully created with yarn, showcasing their distinct shapes, colors, and textures true to the location's ecology. Any notable man-made features or structures at $name are also woven into the scene with yarn, reflecting their architectural or structural details. This yarn-crafted interpretation of $name brings a unique, tactile dimension to the scene, blending artistry with the essence of the place."
            cardStarry -> question = "$name, a $typeOfPlace, at $time, weather being $weather, in the style of Vincent Van Gogh."
            cardDiorama -> question = "$name, a $typeOfPlace, at $time, weather being $weather as a diorama."
            cardWatercolour -> question = "$name, a $typeOfPlace, at $time, weather being $weather in a delicate watercolor painting."
            cardTapestry -> question = "$name, a $typeOfPlace, at $time, weather being $weather in a medieval textured woven weave tapestry."
            cardCartoon -> question = "$name, a $typeOfPlace, at $time, weather being $weather in a bright and bold cartoon style."
            cardClay -> question = "$name, a $typeOfPlace, at $time, weather being $weather, weather being $weather, in a clay animation style, with a handcrafted, sculpted look."
            cardArtbook -> question = "$name (a $typeOfPlace) at $time, weather being $weather, as a digital fantasy painting, characterized by its vibrant color palettes and clear definition. It has realistic textures and exaggerated features, often seen in high-quality concept art for video games and animated films, as well as light, shadow, and texture that gives the scene a lively quality. It must look like a hand illustrated digital drawing, with few imperfections. Leans just a touch cartoony."
        }

        Log.i(TAG, "Terraforming prompt: $question")
        tvPrompt.text = tvPrompt.text.toString() + question
        CoroutineScope(Dispatchers.IO).launch {
            val images = openAI.imageURL( // or openAI.imageJSON
                creation = ImageCreation(
                    prompt = question,
                    model = ModelId("dall-e-3"),
                    n = 1,
                    size = ImageSize("1024x1024")
                )
            )
            withContext(Dispatchers.Main) {
                Log.i(TAG, images[0].url)
                val url = images[0].url
                Picasso.get().load(url).into(ivPicture)
                anim.setVisibility(View.INVISIBLE)
                anim.cancelAnimation()
                btnShutter.setVisibility(View.VISIBLE)
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
        typeOfPlace = ""
        tvPrompt.text = ""
    }

    private fun selectStyle(selectedCard: MaterialCardView){
        if (selectedStyle == selectedCard){
            selectedCard.setChecked(false)
            selectedStyle = null
        }
        else{
            selectedStyle?.setChecked(false)
            selectedStyle = selectedCard
            selectedCard.setChecked(true)
        }
    }

    private fun showBottomSheetDialog(){
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_layout, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        val listView = bottomSheetView.findViewById<ListView>(R.id.listView)
        var placeNames = arrayListOf<String>()

        for (place: Place in placeArray){
            placeNames.add(place.name)
        }


        val adapter = ArrayAdapter(this, R.layout.list_item_layout, R.id.textViewItem, placeNames)
        listView.adapter = adapter
        bottomSheetDialog.show()

        listView.setOnItemClickListener { _, _, position, _ ->
            // Handle item click
            name = placeArray[position].name
            typeOfPlace = placeArray[position].getPlaceTypes()?.get(0).toString()
            Log.i(TAG, "$name has been clicked")
            bottomSheetDialog.dismiss()
        }
    }

    private fun showOrInitBottomSheetHorizontal() {
        if (bottomSheetDialog == null){
            Log.i(TAG, "Initializing bottom_sheet_horizontal_scroll")
            bottomSheetDialog = BottomSheetDialog(this)
            bottomSheetDialog!!.setContentView(bottomSheetView)
            cardYarn = bottomSheetView.findViewById(R.id.cardYarn)
            cardStarry = bottomSheetView.findViewById(R.id.cardStarry)
            cardDiorama = bottomSheetView.findViewById(R.id.cardDiorama)
            cardWatercolour = bottomSheetView.findViewById(R.id.cardWatercolour)
            cardTapestry = bottomSheetView.findViewById(R.id.cardTapestry)
            cardCartoon = bottomSheetView.findViewById(R.id.cardCartoon)
            cardClay = bottomSheetView.findViewById(R.id.cardClay)
            cardArtbook = bottomSheetView.findViewById(R.id.cardArtbook)

            cardYarn.setOnClickListener{ selectStyle(cardYarn) }
            cardStarry.setOnClickListener{ selectStyle(cardStarry) }
            cardDiorama.setOnClickListener{ selectStyle(cardDiorama) }
            cardWatercolour.setOnClickListener{ selectStyle(cardWatercolour) }
            cardTapestry.setOnClickListener{ selectStyle(cardTapestry) }
            cardCartoon.setOnClickListener{ selectStyle(cardCartoon) }
            cardClay.setOnClickListener{ selectStyle(cardClay) }
            cardArtbook.setOnClickListener{ selectStyle(cardArtbook) }
        }
        bottomSheetDialog!!.show()
    }
}


