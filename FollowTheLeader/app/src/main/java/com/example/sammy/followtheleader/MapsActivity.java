package com.example.sammy.followtheleader;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.parse.Parse;
import com.parse.ParseObject;

import java.util.ArrayList;

//public class MapsActivity extends ActionBarActivity implements OnMapReadyCallback {
public class MapsActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks
        ,GoogleApiClient.OnConnectionFailedListener
        ,GoogleMap.OnMapClickListener
        ,GoogleMap.OnMapLongClickListener
        ,LocationListener{

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    public static final String TAG = MapsActivity.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private Location location;
    private Marker marker;
    private boolean markerCreated = false;
    private Polyline polyline;
    private ArrayList<LatLng> arrayPoints = null;
    PolylineOptions polylineOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Enabling Parse API
//        Parse.enableLocalDatastore(this);
//        Parse.initialize(this, "kg6d6QP0IQPIRALoiioW22RgHkzk8586Xvgwdyjh", "L9szZ1U1rxVW07SVW7Wucg3ek9u4DRE46PryrJfg");
//
//        //testing Parse
//        ParseObject testObject = new ParseObject("TestObject");
//        testObject.put("foo", "Tyson Henery");
//        testObject.saveInBackground();

        // Enable Local Datastore.
        arrayPoints = new ArrayList<LatLng>();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        buildGoogleApiClient();
        createLocationRequest();
        mMap.setOnMapLongClickListener(this);
    }
    //---------------------------------------------------------------
    public void createLocationRequest(){
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
    }
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        mGoogleApiClient.connect();
    }
    //    @Override
//    protected void onPause() {
//
//        super.onPause();
//        if (mGoogleApiClient.isConnected()) {
//            mGoogleApiClient.disconnect();
//        }
//    }

    //creation of adding user account-----------------------
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu items for use in the action bar
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.login_activity, menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle presses on the action bar items
//        switch (item.getItemId()) {
//            case R.id.register:
//                registerUser();
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }
//    public void registerUser () {
//        Intent intent = new Intent(this, LoginActivity.class);
//        startActivity(intent);
//    }
    //------------------------------------------------------
    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                //setUpMap();
            }
        }
    }

//    private void setUpMap() {
//        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
//    }

    @Override
    public void onConnected(Bundle bundle) {

        Log.i(TAG, "Service Connected");
        startLocationUpdates();
        location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
//        if (location == null) {
//            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
//        }
//        else {
            handleNewLocation(location);
//        };


    }

    protected void startLocationUpdates(){
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    private void handleNewLocation(Location location) {
        Log.i(TAG, "----------------------" + location.getLatitude());
        Log.d(TAG, location.toString());
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();

        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        if(!markerCreated){
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

            marker = mMap.addMarker(options);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

//            Polyline line = mMap.addPolyline(new PolylineOptions()
//                    .add(new LatLng(39.744752, -122.015956), new LatLng(39.44752, -122.2), new LatLng(39.54752, -122.5), new LatLng(36.44752,-132.2))
//                    .width(5)
//                    .color(Color.RED));
            markerCreated = true;
        }
        else{

            marker.remove();

            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            marker = mMap.addMarker(options);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

            polylineOptions = new PolylineOptions();
            arrayPoints.add(latLng);
            polylineOptions.color(Color.RED);
            polylineOptions.width(8);
            polylineOptions.addAll(arrayPoints);

            mMap.addPolyline(polylineOptions);
//            Polyline line = mMap.addPolyline(new PolylineOptions()
//                    .add()
//                    .width(5)
//                    .color(Color.RED));

//            PolylineOptions rectOptions = new PolylineOptions()
//                    .add(latLng)
//                    .color(Color.RED)
//                    .width(6);
//            polyline = mMap.addPolyline(rectOptions);

//            Parse.enableLocalDatastore(this);
//            Parse.initialize(this, "kg6d6QP0IQPIRALoiioW22RgHkzk8586Xvgwdyjh", "L9szZ1U1rxVW07SVW7Wucg3ek9u4DRE46PryrJfg");
//            ParseObject uPositions = new ParseObject("UserPositions");
//            uPositions.put("Latitude", currentLatitude);
//            uPositions.put("Longitude", currentLongitude);
//            uPositions.put("userID", 4);
//            uPositions.put("Bearing", location.getBearing());
//            uPositions.saveInBackground();
//            //mMap.addAll();
        }
    }
    @Override
    public void onMapLongClick(LatLng point) {
        MarkerOptions marker = new MarkerOptions();
        marker.position(point);
        mMap.addMarker(marker);
    }
    @Override public void onMapClick(LatLng point) {
        marker.remove();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Reconnection Needed");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        handleNewLocation(location);
    }

}