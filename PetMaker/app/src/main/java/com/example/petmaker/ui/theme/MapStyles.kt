package com.example.petmaker.ui.theme

object MapStyles {
    val DAY = """
        [
          {
            "featureType": "landscape",
            "elementType": "geometry",
            "stylers": [
              { "color": "#a8e09f" }
            ]
          },
          {
            "featureType": "water",
            "elementType": "geometry",
            "stylers": [
              { "color": "#48cae4" }
            ]
          },
          {
            "featureType": "road",
            "elementType": "geometry.fill",
            "stylers": [
              { "color": "#fffcf2" }
            ]
          },
          {
            "featureType": "road",
            "elementType": "geometry.stroke",
            "stylers": [
              { "color": "#fca311" }
            ]
          },
          {
            "featureType": "poi",
            "stylers": [
              { "visibility": "off" }
            ]
          },
          {
            "featureType": "transit",
            "stylers": [
              { "visibility": "off" }
            ]
          }
        ]
    """.trimIndent()

    val NIGHT = """
        [
          {
            "elementType": "geometry",
            "stylers": [
              { "color": "#181a26" }
            ]
          },
          {
            "elementType": "labels.text.fill",
            "stylers": [
              { "color": "#7b2cbf" }
            ]
          },
          {
            "featureType": "landscape",
            "elementType": "geometry",
            "stylers": [
              { "color": "#132a13" }
            ]
          },
          {
            "featureType": "road",
            "elementType": "geometry.fill",
            "stylers": [
              { "color": "#1f2235" }
            ]
          },
          {
            "featureType": "road",
            "elementType": "geometry.stroke",
            "stylers": [
              { "color": "#9d4edd" }
            ]
          },
          {
            "featureType": "water",
            "elementType": "geometry",
            "stylers": [
              { "color": "#03045e" }
            ]
          },
          {
            "featureType": "poi",
            "stylers": [
              { "visibility": "off" }
            ]
          },
          {
            "featureType": "transit",
            "stylers": [
              { "visibility": "off" }
            ]
          }
        ]
    """.trimIndent()

    val RAINY = """
        [
          {
            "featureType": "landscape",
            "elementType": "geometry",
            "stylers": [
              { "color": "#4a5568" }
            ]
          },
          {
            "featureType": "water",
            "elementType": "geometry",
            "stylers": [
              { "color": "#1d4ed8" }
            ]
          },
          {
            "featureType": "road",
            "elementType": "geometry.fill",
            "stylers": [
              { "color": "#9ca3af" }
            ]
          },
          {
            "featureType": "road",
            "elementType": "geometry.stroke",
            "stylers": [
              { "color": "#4b5563" }
            ]
          },
          {
            "featureType": "poi",
            "stylers": [
              { "visibility": "off" }
            ]
          },
          {
            "featureType": "transit",
            "stylers": [
              { "visibility": "off" }
            ]
          }
        ]
    """.trimIndent()

    val SNOWY = """
        [
          {
            "featureType": "landscape",
            "elementType": "geometry",
            "stylers": [
              { "color": "#e2e8f0" }
            ]
          },
          {
            "featureType": "water",
            "elementType": "geometry",
            "stylers": [
              { "color": "#90e0ef" }
            ]
          },
          {
            "featureType": "road",
            "elementType": "geometry.fill",
            "stylers": [
              { "color": "#ffffff" }
            ]
          },
          {
            "featureType": "road",
            "elementType": "geometry.stroke",
            "stylers": [
              { "color": "#cbd5e1" }
            ]
          },
          {
            "featureType": "poi",
            "stylers": [
              { "visibility": "off" }
            ]
          },
          {
            "featureType": "transit",
            "stylers": [
              { "visibility": "off" }
            ]
          }
        ]
    """.trimIndent()

    val CLOUDY = """
        [
          {
            "featureType": "landscape",
            "elementType": "geometry",
            "stylers": [
              { "color": "#cfd8dc" }
            ]
          },
          {
            "featureType": "water",
            "elementType": "geometry",
            "stylers": [
              { "color": "#90a4ae" }
            ]
          },
          {
            "featureType": "road",
            "elementType": "geometry.fill",
            "stylers": [
              { "color": "#eceff1" }
            ]
          },
          {
            "featureType": "road",
            "elementType": "geometry.stroke",
            "stylers": [
              { "color": "#b0bec5" }
            ]
          },
          {
            "featureType": "poi",
            "stylers": [
              { "visibility": "off" }
            ]
          },
          {
            "featureType": "transit",
            "stylers": [
              { "visibility": "off" }
            ]
          }
        ]
    """.trimIndent()
}
