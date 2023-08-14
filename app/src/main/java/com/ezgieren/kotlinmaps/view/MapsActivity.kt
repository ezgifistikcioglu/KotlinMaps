package com.ezgieren.kotlinmaps.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.ezgieren.kotlinmaps.R

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.ezgieren.kotlinmaps.databinding.ActivityMapsBinding
import com.ezgieren.kotlinmaps.model.Place
import com.ezgieren.kotlinmaps.roomdb.PlaceDao
import com.ezgieren.kotlinmaps.roomdb.PlaceDatabase
import com.google.android.gms.maps.*
import com.google.android.gms.maps.MapsInitializer.Renderer
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMapsSdkInitializedCallback,
    GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: PlaceDatabase
    private lateinit var placeDao: PlaceDao
    private var trackBoolean: Boolean? = null
    private var selectedLatLng: Double? = null
    private var selectedLatLong: Double? = null
    val compositeDisposable = CompositeDisposable()
    var placeFromMain : Place? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(applicationContext, Renderer.LATEST, this)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        registerLauncher()

        sharedPreferences = this.getSharedPreferences("com.ezgieren.kotlinmaps", MODE_PRIVATE)
        trackBoolean = false

        selectedLatLng = 0.0
        selectedLatLong = 0.0

        database =
            Room.databaseBuilder(applicationContext, PlaceDatabase::class.java, "Places").build()

        placeDao = database.placeDao()
        binding.idSaveButton.isEnabled = false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        //To provide the connection between Map and Listener
        mMap.setOnMapLongClickListener(this)

        val intent = intent
        val info = intent.getStringExtra("info")
        if (info == "new"){
            binding.idSaveButton.visibility = View.VISIBLE
            binding.idDeleteButton.visibility = View.GONE

            //Casting
            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
            locationListener = LocationListener { location ->
                trackBoolean = sharedPreferences.getBoolean("trackBoolean", false)
                if (!trackBoolean!!) {
                    // Last location
                    val userLocation = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                    sharedPreferences.edit().putBoolean("trackBoolean", true).apply()
                }
            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                ) {
                    Snackbar.make(binding.root,
                        "Permission needed for location",
                        Snackbar.LENGTH_INDEFINITE).setAction("Give Permission") {
                        //request Permission
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }.show()
                } else {
                    //request Permission
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            } else {
                // permission granted
                lastLocationWithPermission()
            }
        }else{
            // If this application contains markers, etc. left over from before, we must clean it.
            mMap.clear()
            placeFromMain = intent.getSerializableExtra("selectedPlace") as? Place

            placeFromMain?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                mMap.addMarker(MarkerOptions().position(latLng).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15f))

                binding.idETPlaceText.setText(it.name)
                binding.idSaveButton.visibility = View.GONE
                binding.idDeleteButton.visibility = View.VISIBLE
            }
        }
    }

    private fun registerLauncher() {
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (result) {
                    // permission granted
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    ) {
                        lastLocationWithPermission()
                    }
                } else {
                    // permission denied
                    Toast.makeText(this@MapsActivity, "Permission needed!", Toast.LENGTH_LONG)
                        .show()
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun lastLocationWithPermission() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
            0,
            0f,
            locationListener)
        val lastLocation =
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (lastLocation != null) {
            val lastUserLocation =
                LatLng(lastLocation.latitude, lastLocation.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,
                15f))
        }
        mMap.isMyLocationEnabled = true
    }

    override fun onMapLongClick(p0: LatLng) {
        // get last clicked marker
        mMap.clear()

        mMap.addMarker(MarkerOptions().position(p0))
        selectedLatLng = p0.latitude
        selectedLatLong = p0.longitude
        binding.idSaveButton.isEnabled = true
    }

    override fun onMapsSdkInitialized(renderer: Renderer) {
        when (renderer) {
            Renderer.LATEST -> Log.d("MapsDemo", "The latest version of the renderer is used.")
            Renderer.LEGACY -> Log.d("MapsDemo", "The legacy version of the renderer is used.")
        }
    }

    fun save(view: View) {
        if (selectedLatLng != null && selectedLatLong != null) {
            val place =
                Place(binding.idETPlaceText.text.toString(), selectedLatLng!!, selectedLatLong!!)
            compositeDisposable.add(
                placeDao.insert(place)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )
        }
    }

    private fun handleResponse() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun delete(view: View) {
        placeFromMain?.let {
            compositeDisposable.add(
                placeDao.delete(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}