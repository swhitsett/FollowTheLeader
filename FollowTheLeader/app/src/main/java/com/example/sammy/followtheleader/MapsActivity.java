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
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

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

        //isUserLoggedIn();
        //Enabling Parse API
//        Parse.enableLocalDatastore(this);
//        Parse.initialize(this, "kg6d6QP0IQPIRALoiioW22RgHkzk8586Xvgwdyjh", "L9szZ1U1rxVW07SVW7Wucg3ek9u4DRE46PryrJfg");

        // Enable Local Datastore ----- grab from server eventualy
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

    //creation of adding user account-----------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.new_event, menu);
        return super.onCreateOptionsMenu(menu);
    }
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
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        Log.i(TAG, "Service Connected");
        startLocationUpdates();
        location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else {
            handleNewLocation(location);
        }
    }

    protected void startLocationUpdates(){
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    private void handleNewLocation(Location location) {

        Log.i(TAG, "----------------------" + location.getLatitude());
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();

        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        if(markerCreated) {
            marker.remove();
        }

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

        //pull parse data
        ParseQuery<ParseObject> query = ParseQuery.getQuery("UserPositions");
        query.whereEqualTo("sessionID", "4");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, com.parse.ParseException e) {
                for (ParseObject parseObject : list) {

                    Double Long = Double.parseDouble(parseObject.get("Longitude").toString());
                    Double Lat = Double.parseDouble(parseObject.get("Latitude").toString());

                    LatLng playerLocations = new LatLng(Lat, Long);
                    arrayPoints.add(playerLocations);
                }
            }
        });

        mMap.addPolyline(polylineOptions);

        ParseObject uPositions = new ParseObject("UserPositions");
        uPositions.put("Latitude", currentLatitude);
        uPositions.put("Longitude", currentLongitude);
        uPositions.put("userID", 4);
        uPositions.put("sessionID", 4);
        uPositions.put("Bearing", location.getBearing());
        uPositions.saveInBackground();

        if(!markerCreated) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            markerCreated = true;
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