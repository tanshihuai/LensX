package com.example.terraforming
//TODO: Make image load faster by either changing picasso or resizing image
//TODO: make guide on first load only
//TODO: Make image zoomable, savable to photo gallery

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
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
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.Exception


class MainActivity : AppCompatActivity() {

    private var bottomSheetDialog: BottomSheetDialog? = null
    private lateinit var bottomSheetView: View
    private lateinit var guideStart: View
    private lateinit var guideEnd: View

    private lateinit var btnShutter: AppCompatButton
    private lateinit var btnLocation: AppCompatButton
    private lateinit var btnFilters: AppCompatButton
    private lateinit var animLocation: LottieAnimationView
    private lateinit var animShutter: LottieAnimationView


    private lateinit var ivPicture: ImageView
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var tvPrompt: TextView


    private lateinit var cardYarn: MaterialCardView
    private lateinit var cardStarry: MaterialCardView
    private lateinit var cardDiorama: MaterialCardView
    private lateinit var cardWatercolour: MaterialCardView
    private lateinit var cardMaple: MaterialCardView
    private lateinit var cardCartoon: MaterialCardView
    private lateinit var cardClay: MaterialCardView
    private lateinit var cardArtbook: MaterialCardView


    private val TAG = "My debug"
    private var locationPermissionGranted = false
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

    private val googleMapsAPIKey = BuildConfig.googleMapsAPIKey
    private val openWeatherAPIKey = BuildConfig.openWeatherAPIKey
    private val openAIAPIKey = BuildConfig.openAIAPIKey
    private val openAI = OpenAI(openAIAPIKey)

    private var name = ""
    private var time = ""
    private var weather = "clear skies"
    private var typeOfPlace = ""
    private var getLocationFlag = false
    private var getWeatherFlag = false
    private var selectedStyle: MaterialCardView? = null
    private val placeArray = arrayListOf<Place>()
    private var placeNames = arrayListOf<String>()
    private var locationButtonPressed = false
    private var onInitWeatherFlag = false
    private var onInitLocationFlag = false
    private var firstTime = false



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnShutter = findViewById(R.id.btnShutter)
        btnLocation = findViewById(R.id.btnLocation)
        btnFilters = findViewById(R.id.btnFilters)
        ivPicture = findViewById(R.id.ivPicture)
        tvPrompt = findViewById(R.id.tvPrompt)
        animShutter = findViewById(R.id.animShutter)
        animLocation = findViewById(R.id.animLocation)

        Places.initializeWithNewPlacesApiEnabled(applicationContext, googleMapsAPIKey)
        placesClient = Places.createClient(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_horizontal_scroll, null)


        initFiltersDialog()
        startLoading(animLocation,btnLocation)
        startLoading(animShutter, btnShutter)
        InitPlace()
        getLongLat(::InitWeather)


        btnShutter.setOnClickListener {
            btnLocation.isEnabled = false
            startLoading(animShutter, btnShutter)
            tvPrompt.text = ""
            Log.i(TAG, "Terraforming. Please wait.")
            getTime()

            // if location already selected via btnLocation, skip setPlaceArray in btnShutter
            if (locationButtonPressed) {
                getLocationFlag = true
                getLongLat(::getWeather)
            }
            // if location not yet selected via btnLocation, run setPlaceArray in btnShutter and select place[0]
            else {
                getLongLat(::getWeather)
                setPlaceArray(::generate)
            }
        }

        btnLocation.setOnClickListener {
            btnShutter.isEnabled = false
            startLoading(animLocation, btnLocation)
            setPlaceArray(::generate, ::showLocationDialog)
        }

        btnFilters.setOnClickListener {
            bottomSheetDialog!!.show()
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
            tvPrompt.text =
                tvPrompt.text.toString() + "Error at getLongLat(), please enable location permission and restart the app."
            getLocationPermission()
            // re-enables btn location
            btnLocation.isEnabled = true
        }
    }

    private fun setPlaceArray(generate: () -> Unit) {
        Log.i(TAG, "setPlaceArray() called.")
        placeArray.clear()
        placeNames.clear()

        // Use fields to define the data types to return.
        val placeFields: List<Place.Field> = listOf(Place.Field.NAME, Place.Field.TYPES)

        // Use the builder to create a FindCurrentPlaceRequest.
        val request: FindCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)

        // Call findCurrentPlace and handle the response (first check that the user has granted permission).
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val placeResponse = placesClient.findCurrentPlace(request)
            placeResponse.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val response = task.result
                    var count = 0
                    // todo: handle response array being empty
                    for (placeLikelihood: PlaceLikelihood in response?.placeLikelihoods
                        ?: emptyList()) {
                        if (count > 7) {
                            break
                        }
                        placeArray.add(placeLikelihood.place)
                        placeNames.add(placeLikelihood.place.name)
                        count++
                    }
                    for (place: Place in placeArray) {
                        Log.i(TAG, "Place: ${place.name}, Type: ${place.placeTypes?.get(0)}")
                    }
                    getLocationFlag = true
                    name = placeArray[0].name
                    typeOfPlace = placeArray[0].placeTypes?.get(0).toString()
                    if (getLocationFlag && getWeatherFlag) {
                        Log.i(TAG, "Calling generate() from setPlaceArray()...")
                        generate()
                    }
                } else {
                    // call failed error
                    val exception = task.exception
                    if (exception is ApiException) {
                        Log.i(TAG, "Place not found: ${exception.statusCode}")
                        tvPrompt.text =
                            tvPrompt.text.toString() + "Place not found: ${exception.statusCode}"
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

    private fun setPlaceArray(generate: () -> Unit, locationCallback: () -> Unit) {
        Log.i(TAG, "setPlaceArray() called.")
        placeArray.clear()
        placeNames.clear()

        // Use fields to define the data types to return.
        val placeFields: List<Place.Field> = listOf(Place.Field.NAME, Place.Field.TYPES)

        // Use the builder to create a FindCurrentPlaceRequest.
        val request: FindCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)

        // Call findCurrentPlace and handle the response (first check that the user has granted permission).
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val placeResponse = placesClient.findCurrentPlace(request)
            placeResponse.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val response = task.result
                    var count = 0
                    // todo: handle response array being empty
                    for (placeLikelihood: PlaceLikelihood in response?.placeLikelihoods
                        ?: emptyList()) {
                        if (count > 7) {
                            break
                        }
                        placeArray.add(placeLikelihood.place)
                        placeNames.add(placeLikelihood.place.name)
                        count++
                    }
                    for (place: Place in placeArray) {
                        Log.i(TAG, "Place: ${place.name}, Type: ${place.placeTypes?.get(0)}")
                    }
                    getLocationFlag = true
                    name = placeArray[0].name
                    typeOfPlace = placeArray[0].placeTypes?.get(0).toString()
                    if (getLocationFlag && getWeatherFlag) {
                        Log.i(TAG, "Calling generate() from setPlaceArray()...")
                        generate()
                    }
                    locationCallback()
                } else {
                    // call failed error
                    val exception = task.exception
                    if (exception is ApiException) {
                        Log.i(TAG, "Place not found: ${exception.statusCode}")
                        tvPrompt.text =
                            tvPrompt.text.toString() + "Place not found: ${exception.statusCode}"
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

    private fun InitPlace() {
        getTime()
        Log.i(TAG, "setPlaceArray() called.")
        placeArray.clear()
        placeNames.clear()

        // Use fields to define the data types to return.
        val placeFields: List<Place.Field> = listOf(Place.Field.NAME, Place.Field.TYPES)

        // Use the builder to create a FindCurrentPlaceRequest.
        val request: FindCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)

        // Call findCurrentPlace and handle the response (first check that the user has granted permission).
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val placeResponse = placesClient.findCurrentPlace(request)
            placeResponse.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val response = task.result
                    var count = 0
                    // todo: handle response array being empty
                    for (placeLikelihood: PlaceLikelihood in response?.placeLikelihoods
                        ?: emptyList()) {
                        if (count > 7) {
                            break
                        }
                        placeArray.add(placeLikelihood.place)
                        placeNames.add(placeLikelihood.place.name)
                        count++
                    }
                    for (place: Place in placeArray) {
                        Log.i(TAG, "Place: ${place.name}, Type: ${place.placeTypes?.get(0)}")
                    }
                    getLocationFlag = true
                    name = placeArray[0].name
                    typeOfPlace = placeArray[0].placeTypes?.get(0).toString()
                    onInitLocationFlag = true
                    if (onInitWeatherFlag && onInitLocationFlag){
                        updateTvPrompt()
                        endLoading(animLocation, btnLocation)
                        endLoading(animShutter, btnShutter)
                        if (firstTime){
                            initiateGuide()
                        }
                    }
                } else {
                    // call failed error
                    val exception = task.exception
                    if (exception is ApiException) {
                        Log.i(TAG, "Place not found: ${exception.statusCode}")
                        tvPrompt.text =
                            tvPrompt.text.toString() + "Place not found: ${exception.statusCode}"
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

    private fun InitWeather(lat: Double, long: Double) {
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
                    onInitWeatherFlag = true
                    if (onInitWeatherFlag && onInitLocationFlag){
                        updateTvPrompt()
                        endLoading(animLocation, btnLocation)
                        endLoading(animShutter, btnShutter)
                        if (firstTime){
                            initiateGuide()
                        }

                    }
                }
            } catch (e: Exception) {
                Log.i(TAG, "exception caught, ${e.localizedMessage}")
            }
        }
    }

    private fun generate() {
        Log.i(TAG, "generate() called.")
        var question = "A photo taken at $name, a $typeOfPlace, weather being $weather, at $time."

        when (selectedStyle) {
            cardYarn -> question =
                "2d scene of $name (a $typeOfPlace) at $time, weather being $weather, entirely crafted from yarn and balls of yarn."

            cardStarry -> question =
                "$name, a $typeOfPlace, at $time, weather being $weather, in the style of Vincent Van Gogh."

            cardDiorama -> question =
                "$name, a $typeOfPlace, at $time, weather being $weather as a diorama."

            cardWatercolour -> question =
                "$name, a $typeOfPlace, at $time, weather being $weather in a delicate watercolor painting."

            cardMaple -> question =
                "$name, a $typeOfPlace from the perspective of a player playing in the style of Maplestory, in full color, at $time, weather being $weather"

            cardCartoon -> question =
                "$name, a $typeOfPlace, at $time, weather being $weather in a bright and bold cartoon style."

            cardClay -> question =
                "$name, a $typeOfPlace, at $time, weather being $weather, weather being $weather, in a clay animation style, with a handcrafted, sculpted look."

            cardArtbook -> question =
                "$name (a $typeOfPlace) at $time, weather being $weather, as a digital fantasy painting, characterized by its vibrant color palettes and clear definition. " +
                        "It has realistic textures and exaggerated features, often seen in high-quality concept art for video games and animated films, " +
                        "as well as light, shadow, and texture that gives the scene a lively quality. It must look like a hand illustrated digital drawing, with few imperfections. Leans just a touch cartoon-y."
        }

        Log.i(TAG, "Terraforming prompt: $question")
        tvPrompt.text = tvPrompt.text.toString() + "A photo taken at $name, a $typeOfPlace, weather being $weather, at $time."
        CoroutineScope(Dispatchers.IO).launch {
            try{
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

                    Picasso.get()
                        .load(url)
                        .centerCrop()
                        .resize(ivPicture.getMeasuredWidth(),ivPicture.getMeasuredHeight())
                        .into(ivPicture);

                    endLoading(animShutter, btnShutter)
                    resetVariables()
                    // re-enables location button after generate() is called:
                    // Either generate() call is successful and location button needs to be re-enabled or
                    // generate() call is unsuccessful but caught and location button needs to be re-enabled
                    btnLocation.isEnabled = true
                }
            }
            catch (e: Exception){
                tvPrompt.text = "Image generation failed: $e. Please try again."
                Log.i(TAG, "Image generation failed at generate(). Error is $e")
                withContext(Dispatchers.Main) {
                    // re-enables location button after generate() is called:
                    // Either generate() call is successful and location button needs to be re-enabled or
                    // generate() call is unsuccessful but caught and location button needs to be re-enabled
                    endLoading(animShutter, btnShutter)
                    resetVariables()
                    btnLocation.isEnabled = true
                }
            }
        }


        if (firstTime){
            endGuide()
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
        time = ""
        weather = "clear skies"
        getLocationFlag = false
        getWeatherFlag = false
    }

    private fun selectStyle(selectedCard: MaterialCardView) {
        if (selectedStyle == selectedCard) {
            selectedCard.isChecked = false
            selectedStyle = null
        } else {
            selectedStyle?.isChecked = false
            selectedStyle = selectedCard
            selectedCard.isChecked = true
        }
        bottomSheetDialog?.dismiss()
    }

    private fun showLocationDialog() {
        endLoading(animLocation, btnLocation)
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_layout, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        val listView = bottomSheetView.findViewById<ListView>(R.id.listView)
        val adapter = ArrayAdapter(this, R.layout.list_item_layout, R.id.textViewItem, placeNames)
        listView.adapter = adapter
        bottomSheetDialog.show()
        // re-enables shutter button after locations are showed:
        // Either user dismisses dialog and shutter button needs to be re-enabled or user taps on a location and shutter button needs to be re-enabled
        btnShutter.isEnabled = true
        Log.i(TAG, "btn shutter is enabled")

        listView.setOnItemClickListener { _, _, position, _ ->
            // Handle item click
            name = placeArray[position].name
            typeOfPlace = placeArray[position].placeTypes?.get(0).toString()
            Log.i(TAG, "$name has been selected as the location")
            updateTvPrompt()
            locationButtonPressed = true

            // continues on with the guide
            if (firstTime) {
                Log.i(TAG, "btn shutter is enabled")
                btnFiltersGuide()
            }
            bottomSheetDialog.dismiss()
        }
    }

    private fun initFiltersDialog() {
        Log.i(TAG, "Initializing bottom_sheet_horizontal_scroll")
        bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog!!.setContentView(bottomSheetView)
        cardYarn = bottomSheetView.findViewById(R.id.cardYarn)
        cardStarry = bottomSheetView.findViewById(R.id.cardStarry)
        cardDiorama = bottomSheetView.findViewById(R.id.cardDiorama)
        cardWatercolour = bottomSheetView.findViewById(R.id.cardWatercolour)
        cardMaple = bottomSheetView.findViewById(R.id.cardMaple)
        cardCartoon = bottomSheetView.findViewById(R.id.cardCartoon)
        cardClay = bottomSheetView.findViewById(R.id.cardClay)
        cardArtbook = bottomSheetView.findViewById(R.id.cardArtbook)

        cardYarn.setOnClickListener { selectStyle(cardYarn) }
        cardStarry.setOnClickListener { selectStyle(cardStarry) }
        cardDiorama.setOnClickListener { selectStyle(cardDiorama) }
        cardWatercolour.setOnClickListener { selectStyle(cardWatercolour) }
        cardMaple.setOnClickListener { selectStyle(cardMaple) }
        cardCartoon.setOnClickListener { selectStyle(cardCartoon) }
        cardClay.setOnClickListener { selectStyle(cardClay) }
        cardArtbook.setOnClickListener { selectStyle(cardArtbook) }
        bottomSheetDialog!!.setOnDismissListener {
            if (firstTime) {
                btnShutterGuide()
            }
        }
    }

    private fun startLoading(loadingAnimation: LottieAnimationView, button: AppCompatButton) {
        button.visibility = View.INVISIBLE
        loadingAnimation.visibility = View.VISIBLE
        loadingAnimation.playAnimation()
    }

    private fun endLoading(loadingAnimation: LottieAnimationView, button: AppCompatButton) {
        loadingAnimation.cancelAnimation()
        loadingAnimation.visibility = View.INVISIBLE
        button.visibility = View.VISIBLE
    }

    private fun btnLocationGuide() {
        MaterialTapTargetPrompt.Builder(this).apply {
            setTarget(findViewById(R.id.btnLocation))
            setPrimaryText("Tap here to select your location.")
            setSecondaryText("Select a location around you that you wish to generate a picture of.")
            setAutoDismiss(false)
            setFocalRadius(R.dimen.focal_radius_side)
        }.show()!!
    }

    private fun btnFiltersGuide() {
        MaterialTapTargetPrompt.Builder(this).apply {
            setTarget(findViewById(R.id.btnFilters))
            setPrimaryText("Tap here to select a filter.")
            setSecondaryText("Select a filter you want your picture to have. You can choose to not apply any filter.")
            setAutoDismiss(false)
            setFocalRadius(R.dimen.focal_radius_side)
        }.show()
    }

    private fun btnShutterGuide() {
        MaterialTapTargetPrompt.Builder(this).apply {
            setTarget(findViewById(R.id.btnShutter))
            setPrimaryText("Tap here to generate a photo.")
            setSecondaryText("This generates an image based on the current location and the filter you have chosen.")
            setAutoDismiss(false)
            setFocalRadius(R.dimen.focal_radius_shutter)
        }.show()
    }

    private fun initiateGuide() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_intro)
        guideStart = dialog.findViewById(R.id.guideStart)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)
        guideStart.setOnClickListener {
            dialog.dismiss()
            btnLocationGuide()
        }
        dialog.show()

    }

    private fun endGuide(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_end)
        guideEnd = dialog.findViewById(R.id.guideEnd)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)
        guideEnd.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun updateTvPrompt(){
        tvPrompt.text = "A photo taken at $name, a $typeOfPlace, at $time, weather being $weather."
    }

}