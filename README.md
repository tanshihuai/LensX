# LensX
LensX is a camera with no lens. Instead, it uses AI to generate a "photo" of a specific place and moment using the phone's current location, time, weather, and surrounding environment.
<br>
<br>
<br>
<p align="center">
    <img src="https://github.com/tanshihuai/Terraforming/blob/master/app/src/main/res/raw/general_1.gif" width="40%" height="40%">
</p>

## How it works
LensX uses OpenAI's DALL.E 3 to generate an image via a text prompt. 
<br>
<br>
This text prompt is generated automatically by reverse geocaching your current location to identify your surroundings, and also incorporates the current time, date, weather, season, and lighting conditions (golden hour, overcast, etc). Additionally, LensX allows users to specify whether they are indoors or outdoors, and has a selection of filters that completely transforms the generated photo.
<br>
<br>
<br>
<p align="center">
    <img src="https://github.com/tanshihuai/Terraforming/blob/master/app/src/main/res/raw/presskit_2.png">
</p>
<br>
<br>
<br>
<br>

<p align="center">
    <img src="https://github.com/tanshihuai/Terraforming/blob/master/app/src/main/res/raw/presskit_1.png">
</p>


## Choosing a location
LensX takes the user's live location data to search for surrounding landmarks. Users are able to pinpoint and select their exact location to generate a photo that best captures the essence of a specific place and moment.
<br>
<br>
<br>

<p align="center">
    <img src="https://github.com/tanshihuai/Terraforming/blob/master/app/src/main/res/raw/locations_1.gif" width="40%" height="40%">
</p>

## Choosing a filter
LensX has a selection of advanced filters, allowing users to reimagine their whole photo, such as into a world crafted by yarn and wool, or as a little buildings and figurines in a diorama.
<br>
<br>
<br>

<p align="center">
    <img src="https://github.com/tanshihuai/Terraforming/blob/master/app/src/main/res/raw/filters_1.gif" width="40%" height="40%">
</p>

## Overview

LensX was created in an attempt to bridge reality and imagination, allowing users to capture the "vibe" of a specific time and place, rather than a photo. Every click of the shutter reveals something new.
Join us in reimagining photography, where the focus isn't on capturing what we see, but on experiencing a slice of the imaginary.



## Tech Stack
***Front-end & Back-end***
<br>
[Android Studio (Kotlin)](https://developer.android.com/studio)
<br>

***APIS***
<br>
Image generation: [DALL.E 3](https://platform.openai.com/docs/guides/images/introduction?context=node) <br>
Weather data: [OpenweatherMap](https://openweathermap.org/) <br>
Location data: [Google Places](https://developers.google.com/maps/documentation/places/web-service/overview) <br> 
Image display: [Picasso](https://square.github.io/picasso/)


## Authors
**Jack Tan** - [GitHub](https://github.com/tanshihuai)

## Acknowledgments
Special thanks to [Bjøern Karmann](https://bjoernkarmann.dk/), whose Paragraphica inspired LensX.
