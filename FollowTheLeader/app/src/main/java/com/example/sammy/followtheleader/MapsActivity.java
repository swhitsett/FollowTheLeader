package com.example.sammy.followtheleader;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
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
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private ArrayList<String> currentPlayerNames = null;
    private ArrayList<String> userAtPoint = null;
    private ArrayList<LatLng> markerLocation = null;
    private ArrayList<LatLng> playerPollyLine = null;
    private int eventType;
    private String user1;
    private String sessionID;
    private boolean gameStarted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        isUserLoggedIn();

//        ParseInstallation.getCurrentInstallation().saveInBackground();

        currentPlayerNames = new ArrayList<String>();
        userAtPoint = new ArrayList<String>();
        markerLocation = new ArrayList<LatLng>();
        playerPollyLine = new ArrayList<LatLng>();
        //Grab data from other activitys

//        currentPlayerNames = intent.getStringArrayListExtra("peoplePlaying");
//        eventType = intent.getIntExtra("eventType", 0);
//        sessionID = intent.getStringExtra("sessionID");

        String jsonData = intent.getStringExtra("com.parse.Data");

        if(jsonData != null) {
            JSONObject json;
            try {
                json = new JSONObject(jsonData);
                sessionID = json.getString("gameID");
                gameStarted = json.getBoolean("gameStarted");
                eventType = json.getInt("eventType");
                getArrayList(json.getJSONArray("currentPlayers"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            currentPlayerNames = intent.getStringArrayListExtra("peoplePlaying");
            eventType = intent.getIntExtra("eventType", 0);
            sessionID = intent.getStringExtra("sessionID");
            gameStarted = intent.getBooleanExtra("gameStarted", false);
        }
//        gameStarted = intent.getBooleanExtra("gameStarted", false);
        arrayPoints = new ArrayList<LatLng>();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        buildGoogleApiClient();
        createLocationRequest();
//        initparseInstliation();
        mMap.setOnMapLongClickListener(this);


    }
    //---------------------------------------------------------------
    private void getArrayList(JSONArray mrArray){
        for (int i=0;i<mrArray.length();i++){
//                mrArray.getString(i).toString();
            try {
                currentPlayerNames.add(mrArray.get(i).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    public void isUserLoggedIn(){
        SharedPreferences settings = getSharedPreferences("Prefs_File", 0);
        boolean loggedin = settings.getBoolean("loggedIn", false);
        String uname = settings.getString("userName", "blank");
//        setSilent(silent);

        if(!loggedin){
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        else{
            user1 = uname;
        }
    }
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
        inflater.inflate(R.menu.menu_new_event, menu);
        return super.onCreateOptionsMenu(menu);
    }
//
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.register:
                newEvent();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void newEvent () {
        Intent intent = new Intent(this, NewEvent.class);
        intent.putExtra("user1", user1);
        startActivity(intent);
    }
    //------------------------------------------------------
    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }
    @Override
    protected void onStop(){
        super.onStop();

        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences("Prefs_File", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("userName", user1);
        editor.putBoolean("loggedIn", true);
        editor.commit();

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
        if(gameStarted) {
            Log.i(TAG, "----------------------" + location.getLatitude());
            double currentLatitude = location.getLatitude();
            double currentLongitude = location.getLongitude();

            LatLng latLng = new LatLng(currentLatitude, currentLongitude);

            if(markerCreated) {
                arrayPoints.clear();
                userAtPoint.clear();
            }

            saveNewLocation(currentLatitude, currentLongitude, location.getBearing());
            pullUserPositions(latLng);

            if (!markerCreated) {
                currentPlayerNames.add(user1);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            }
        }
    }
//    private void addPollyLinesToMap(){
//        polylineOptions = null;
//        polylineOptions = new PolylineOptions();
//        polylineOptions.color(Color.RED);
//        polylineOptions.width(8);
//        polylineOptions.addAll(arrayPoints);
//        mMap.addPolyline(polylineOptions);
//    }
    private void drawPlayerPolyline(){
        polylineOptions = null;
        polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.RED);
        polylineOptions.width(8);
        polylineOptions.addAll(playerPollyLine);
        mMap.addPolyline(polylineOptions);
        playerPollyLine.clear();
    }
    private void saveNewLocation(double currentLatitude, double currentLongitude, double bearing){
        ParseObject uPositions = new ParseObject("UserPositions");
        uPositions.put("Latitude", currentLatitude);
        uPositions.put("Longitude", currentLongitude);
        uPositions.put("userID", user1);
        uPositions.put("sessionID", sessionID);
        uPositions.put("Bearing", bearing);
        uPositions.saveInBackground();
    }
    private void placeUserMarkers (LatLng latLng) {
        markerLocation.clear();
        boolean userPointFound = false;
//        ArrayList<LatLng> markerLocation = new ArrayList<LatLng>();
        for(int j=currentPlayerNames.size()-1; j>=0; j--) {
            for (int i = userAtPoint.size()-1; i>=0  ; i--) {
                if (userAtPoint.get(i).equals(currentPlayerNames.get(j))) {
                    playerPollyLine.add(arrayPoints.get(i));
                    if(!userPointFound) {
                        if (markerLocation.isEmpty()) {
                            markerLocation.add(arrayPoints.get(i));
                            userPointFound = true;
//                            break;
                        } else {
                            markerLocation.add(j, arrayPoints.get(i));
                            userPointFound = true;
//                            break;
                        }
                    }
                }
            }
            drawPlayerPolyline();
            userPointFound = false;
        }
        for (int i = 0; i < markerLocation.size(); i++) {
            MarkerOptions options = new MarkerOptions()
                    .position(markerLocation.get(i))
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            marker = mMap.addMarker(options);
            markerCreated = true;
//            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }
    private void pullUserPositions(final LatLng latLng) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("UserPositions");
        query.whereEqualTo("sessionID", sessionID);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, com.parse.ParseException e) {
                for (ParseObject parseObject : list) {

                    Double Long = Double.parseDouble(parseObject.get("Longitude").toString());
                    Double Lat = Double.parseDouble(parseObject.get("Latitude").toString());
                    String user = parseObject.getString("userID").toString();
//                    String userasdf = parseObject.getString("sessionID").toString();

                    LatLng playerLocations = new LatLng(Lat, Long);
                    arrayPoints.add(playerLocations);
                    userAtPoint.add(user);
                }
                mMap.clear();
                placeUserMarkers(latLng);
//                addPollyLinesToMap();
            }
        });

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