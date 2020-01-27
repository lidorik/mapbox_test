package com.e.nv3

import android.graphics.Bitmap
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.utils.BitmapUtils
import com.mapbox.services.android.navigation.ui.v5.NavigationContract
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher.startNavigation
import com.mapbox.services.android.navigation.ui.v5.NavigationView
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera
import com.mapbox.services.android.navigation.ui.v5.listeners.BannerInstructionsListener
import com.mapbox.services.android.navigation.ui.v5.listeners.InstructionListListener
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.Exception

class MainActivity : AppCompatActivity(), OnNavigationReadyCallback, Callback<DirectionsResponse>,
    NavigationListener, ProgressChangeListener, InstructionListListener,
    BannerInstructionsListener, LocationEngineCallback<LocationEngineResult>, PermissionsListener {

    private val DEFAULT_INTERVAL_IN_MILLISECONDS : Long = 500L
    private val DEFAULT_MAX_WAIT_TIME : Long = (DEFAULT_INTERVAL_IN_MILLISECONDS * 5)

    private val MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoiZ2VzdHVkaW8iLCJhIjoiY2s1cGI4eGJ6MWR5dTNtcndsMTltM3Y3OCJ9.lZKerrcB1t7RbPkZLwntXQ"

    //    private val ORIGIN: Point = Point.fromLngLat(44.799622, 41.696440)
    private val DESTINATION: Point = Point.fromLngLat(44.802852, 41.696728)
    private val INITIAL_ZOOM : Double = 16.0

    private lateinit var locationEngine : LocationEngine

    private var currentPos : LocationEngineResult? = null

    private var navigationView: NavigationView? = null
    private lateinit var map : NavigationMapboxMap
    private lateinit var map2 : MapboxMap

    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private var navInitialized : Boolean = false

    private lateinit var symbolManager : SymbolManager

    private var ID_ICON_AIRPORT : String = "airport"

    private lateinit var mapView : MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        Mapbox.getInstance(this, MAPBOX_ACCESS_TOKEN)
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        navigationView = navigationXmlView


        createMapBox(savedInstanceState)
        navigationView?.onCreate(savedInstanceState)


    }

    override fun onNavigationReady(isRunning: Boolean) {
        Log.d("xxx","onNavigationReady")
        fetchRoute()

//        locationEngine = LocationEngineProvider.getBestLocationEngine(this)
//        navigation.locationEngine = locationEngine
    }

    private fun fetchRoute() {
        Log.d("xxx","fetchRoute")

        // Mapboxmap init
        map = navigationView!!.retrieveNavigationMapboxMap()!!
        map!!.updateLocationLayerRenderMode(RenderMode.NORMAL)
//        var icon : Sprite = spriteFactory.fromResource(R.drawable.ic_directions_boat_black_18dp)

//        // Add symbol at specified lat/lon
//        var symbol = symbolManager.create(
//            SymbolOptions()
////        .withLatLng(new LatLng(60.169091, 24.939876))
////        .withIconImage(BitmapUtils.getBitmapFromDrawable(resources.getDrawable(R.drawable.icon_red))!!)
//                .withIconSize(2.0f)
//                .withIconColor("#cc0000")
//        )

//        bindText2Symbol()
        map.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
        map.updateLocationVisibilityTo(true)

        NavigationRoute.builder(this)
            .accessToken(MAPBOX_ACCESS_TOKEN)
            .origin(Point.fromLngLat(currentPos!!.lastLocation!!.longitude, currentPos!!.lastLocation!!.latitude))
            .destination(DESTINATION)
//            .alternatives(true)
            .build()
            .getRoute( this )

    }

    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onResponse(
        call: Call<DirectionsResponse>,
        response: Response<DirectionsResponse>
    ) {
        Log.d("xxx","fetchRoute onResponse")
        var directionsRoute : DirectionsRoute = response.body()?.routes()?.get(0) ?: return
        Log.d("xxx"," FOUND!! fetchRoute onResponse")
//        map.startCamera(directionsRoute)
        startNavigation(directionsRoute)
    }

    private fun startNavigation(directionsRoute: DirectionsRoute) {
        Log.d("xxx","StartNavigation")

        val options = NavigationViewOptions.builder()
//            .locationEngine(locationEngine)
//            .navigationListener(this)
            .directionsRoute(directionsRoute)
            .shouldSimulateRoute(true)
            .progressChangeListener(this)
            .instructionListListener(this)
//            .speechAnnouncementListener(this)
            .bannerInstructionsListener(this)
//        setBottomSheetCallback(options)
//        setupNightModeFab()
        navigationView!!.startNavigation(options.build())
    }

    fun initLocationEngine(){
        Log.d("xxx","Location Engine Started")
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        var request : LocationEngineRequest = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build()

        locationEngine!!.requestLocationUpdates(request, this, getMainLooper());
        locationEngine!!.getLastLocation(this);
    }

    @SuppressWarnings("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            initLocationEngine()

            Log.d("xxx","Permission granted")

            // Get an instance of the component
            val locationComponent = map2?.locationComponent

            // Activate with a built LocationComponentActivationOptions object
            locationComponent?.activateLocationComponent(LocationComponentActivationOptions.builder(this, loadedMapStyle).build())

            // Enable to make component visible
            locationComponent?.isLocationComponentEnabled = true

            // Set the component's camera mode
            locationComponent?.cameraMode = CameraMode.TRACKING_COMPASS

            // Set the component's render mode
            locationComponent?.renderMode = RenderMode.COMPASS

        } else {

            permissionsManager = PermissionsManager(this)

            permissionsManager?.requestLocationPermissions(this)

        }
    }

    fun navigationInitialize()
    {
        val initialPosition : CameraPosition = CameraPosition.Builder()
            .target(LatLng(currentPos!!.lastLocation!!.latitude, currentPos!!.lastLocation!!.longitude))
            .zoom(INITIAL_ZOOM)
            .build()
        navigationView?.initialize(this, initialPosition)

    }

    override fun onNavigationFinished() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onNavigationRunning() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCancelNavigation() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProgressChange(location: Location?, routeProgress: RouteProgress?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onInstructionListVisibilityChanged(visible: Boolean) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun willDisplay(instructions: BannerInstructions?): BannerInstructions? {
        return instructions
    }

    override fun onFailure(exception: Exception) {
        currentPos = null
    }

    override fun onSuccess(result: LocationEngineResult?) {
        if( result != null ){
            Log.d("xxx","Got new location coords")
            currentPos = result
            if(!navInitialized)
            {
                navInitialized = true
                setCameraToMap()
                navigationInitialize()
            }
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPermissionResult(granted: Boolean) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        if (granted) {
            Log.d("MapBox", "call enableLocationComponent");
//            enableLocationComponent()
//            enableLocation()
        } else {
            Toast.makeText(this, "საჭიროა GPS აქტივაცია", Toast.LENGTH_LONG).show()
//            enableLocationComponent()
            finish()
        }
    }

    private fun bindText2Symbol(style : Style) {
        style.addImage(
            ID_ICON_AIRPORT,
            BitmapUtils.getBitmapFromDrawable(resources.getDrawable(R.drawable.icon_red))!!,
            true
        )
    }

    fun setCameraToMap()
    {
        val initialPosition = CameraPosition.Builder()
            .target(LatLng(currentPos!!.lastLocation!!.latitude, currentPos!!.lastLocation!!.longitude))
            .zoom(INITIAL_ZOOM)
            .build()

        navigationView!!.initialize(this, initialPosition)
    }


    fun createMapBox(savedInstanceState : Bundle?)
    {
//        val navigation = MapboxNavigation(this, mapbox_access_token)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { mapboxMap ->
            Log.d("MapBox","mapboxMap ->")
            map2 = mapboxMap
            mapboxMap.setStyle(Style.MAPBOX_STREETS) {
                // Map is set up and the style has loaded. Now you can add data or make other map adjustments.
//                addAirplaneImageToStyle(it)
                enableLocationComponent(map2.style!!)
                symbolManager = SymbolManager(mapView, map2, it)
                symbolManager.iconAllowOverlap = true
                symbolManager.iconIgnorePlacement = true
            }

        }

        Log.d("MapBox","createMapBox")
    }


}

