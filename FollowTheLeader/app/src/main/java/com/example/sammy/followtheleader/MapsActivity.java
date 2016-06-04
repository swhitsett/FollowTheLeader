package com.example.sammy.followtheleader;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
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
    private ArrayList<Double> userBearing = null;
    private int eventType;
    private String user1;
    private String sessionID;
    private String userPlace;
    private boolean gameStarted = false;
    private boolean initalZoom = true;
    private LatLng destinationLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        isUserLoggedIn();

        userPlace = "";
        currentPlayerNames = new ArrayList<String>();
        userAtPoint = new ArrayList<String>();
        markerLocation = new ArrayList<LatLng>();
        playerPollyLine = new ArrayList<LatLng>();
        userBearing = new ArrayList<Double>();
        arrayPoints = new ArrayList<LatLng>();

        String jsonData = intent.getStringExtra("com.parse.Data");

        if(jsonData != null) {
            JSONObject json;
            JSONArray players;
            try {
                json = new JSONObject(jsonData);
                sessionID = json.getString("gameID");
                gameStarted = json.getBoolean("gameStarted");
                eventType = json.getInt("eventType");
                destinationLocation = new LatLng(json.getDouble("Lat"), json.getDouble("Long"));
                players = json.getJSONArray("currentPlayers");
                getArrayList(players);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            eventType = intent.getIntExtra("eventType", 0);
            sessionID = intent.getStringExtra("sessionID");
            gameStarted = intent.getBooleanExtra("gameStarted", false);
            destinationLocation = intent.getParcelableExtra("destination");
            currentPlayerNames = intent.getStringArrayListExtra("peoplePlaying");
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        buildGoogleApiClient();
        createLocationRequest();
        mMap.setOnMapLongClickListener(this);

    }
    //Checks for Logged in User and JSON Parser for players+++++++++++++++++++++++++++++++++++++++++
    private void getArrayList(JSONArray mrArray){
        for (int i=0;i<mrArray.length();i++){
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

        if(!loggedin){
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        else{
            user1 = uname;
        }
    }
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    //Creation of Event and Sign Out actionbar items -----------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_new_event, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.register:
                newEvent();
                return true;
            case R.id.sign_out:
                SharedPreferences settings = getSharedPreferences("Prefs_File", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("loggedIn", false);
                editor.commit();
                isUserLoggedIn();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    public void newEvent () {
        Intent intent = new Intent(this, NewEvent.class);
        intent.putExtra("user1", user1);
        intent.putExtra("destination",destinationLocation);
        startActivity(intent);
    }
    //----------------------------------------------------------------------------------------------

    //Handlers for Leaving the Application/Returning++++++++++++++++++++++++++++++++++++++++++++++++
    @Override
    protected void onPause() {
        super.onPause();
        if(!gameStarted) {
            if (mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                mGoogleApiClient.disconnect();
            }
        }
    }
    @Override
    protected void onStop(){
        super.onStop();

        SharedPreferences settings = getSharedPreferences("Prefs_File", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("userName", user1);
        editor.putBoolean("loggedIn", true);
        editor.commit();

    }
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        mGoogleApiClient.connect();
    }
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    //Event handler for Location listener-----------------------------------------------------------
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

            TextView playerPlace = (TextView) findViewById(R.id.PlayerSpeed);
            String currentDistance = String.format("Speed \n%dmph", Math.round(location.getSpeed() * (float)2.236936 ));
            playerPlace.setText(currentDistance);

            saveNewLocation(currentLatitude, currentLongitude, location.getBearing());
            pullUserPositions(latLng);

            if (!markerCreated) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            }

            arrivedAtDestination();
        }
        else if(initalZoom){
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
            initalZoom = false;
        }
    }
    //----------------------------------------------------------------------------------------------

    //Saving and Pulling data from Parse -----------------------------------------------------------
    private void saveNewLocation(double currentLatitude, double currentLongitude, double bearing){
        ParseObject uPositions = new ParseObject("UserPositions");
        uPositions.put("Latitude", currentLatitude);
        uPositions.put("Longitude", currentLongitude);
        uPositions.put("userID", user1);
        uPositions.put("sessionID", sessionID);
        uPositions.put("Bearing", bearing);
        uPositions.saveInBackground();
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
                    Double bar = Double.parseDouble(parseObject.get("Bearing").toString());

                    LatLng playerLocations = new LatLng(Lat, Long);
                    userBearing.add(bar);
                    arrayPoints.add(playerLocations);
                    userAtPoint.add(user);
                }
                mMap.clear();
                placeUserMarkers(latLng);
            }
        });
    }
    //----------------------------------------------------------------------------------------------

    //Drawing map data to the screen++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    private void placeUserMarkers (LatLng latLng) {
        markerLocation.clear();
        boolean userPointFound = false;
        for(int j=currentPlayerNames.size()-1; j>=0; j--) {
            for (int i = userAtPoint.size()-1; i>=0  ; i--) {
                if (userAtPoint.get(i).equals(currentPlayerNames.get(j))) {
                    playerPollyLine.add(arrayPoints.get(i));
                    if(!userPointFound) {
                        if (markerLocation.isEmpty()) {
                            markerLocation.add(arrayPoints.get(i));
                            userPointFound = true;
                        } else {
                            markerLocation.add(j, arrayPoints.get(i));
                            userPointFound = true;
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
                    .title(userAtPoint.get(i))
                    .anchor(0.5f,0.5f)
                    .rotation(new Float(userBearing.get(i)))
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.arrow))
                    .flat(true);//BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            marker = mMap.addMarker(options);
            markerCreated = true;

            if((userAtPoint.get(i).equals(user1))) {
//                location.setLatitude(markerLocation.get(i).latitude);
//                location.setLongitude(markerLocation.get(i).longitude);
                Location destination = new Location("dest");
//                Location userpoint = new Location("user");
                destination.setLatitude(destinationLocation.latitude);
                destination.setLongitude(destinationLocation.longitude);
                location.setLatitude(markerLocation.get(i).latitude);
                location.setLongitude(markerLocation.get(i).longitude);
//                userpoint.setLatitude(markerLocation.get(i).latitude);  //individual persons location
//                userpoint.setLongitude(markerLocation.get(i).longitude); //individual persons location
                double k = location.distanceTo(destination);
                calcPositionToDestination(destination, k);
            }
//            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
        handleFinalDestination();
    }
    private void drawPlayerPolyline(){
        polylineOptions = null;
        polylineOptions = new PolylineOptions();
        polylineOptions. color(Color.RED);
        polylineOptions.width(8);
        polylineOptions.addAll(playerPollyLine);
        mMap.addPolyline(polylineOptions);
        playerPollyLine.clear();
    }
    private void handleFinalDestination(){

        mMap.addMarker(new MarkerOptions()
                .position(destinationLocation)
                .anchor(0.5f, 0.5f)
                .rotation(-30)
                .title("Meet You Here!!")
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.flag)));

        mMap.addCircle(new CircleOptions()
                .center(destinationLocation)
                .radius(20)
                .strokeColor(Color.RED)
                .fillColor(Color.GREEN));

    }
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    //Game Calculations (dist, speed, place) and mapview display------------------------------------
    private void arrivedAtDestination(){
        float[] results = new float[1];

        Location.distanceBetween(destinationLocation.latitude
                , destinationLocation.longitude
                , location.getLatitude()
                , location.getLongitude()
                , results);

        float distanceInMeters = results[0];
        boolean isWithin10m = distanceInMeters < 20;
        TextView playerPlace = (TextView) findViewById(R.id.PlayerDistance);
        String currentDistance = String.format("Distance\n%dft", new Integer(Math.round(distanceInMeters * (float)3.2808)));
        playerPlace.setText(currentDistance);

        //test toast
        if(isWithin10m) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage(String.format("You Have arrived %s\n at the destination", userPlace));
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

            String uniqueID = UUID.randomUUID().toString();
            ParseQuery pushQuery = ParseInstallation.getQuery();
            pushQuery. whereEqualTo("user", currentPlayerNames);
            pushQuery.whereNotEqualTo("user",user1);              //avoid sending to yourself

            JSONObject data = new JSONObject();
            try {
                data.put("alert", String.format("%s has arrived %s at the location",user1,userPlace));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            ParsePush push = new ParsePush();
            push.setQuery(pushQuery); // Set our Installation query
            push.setData(data);
            push.sendInBackground();
            gameStarted = false;

        }
    }
    private void calcPositionToDestination( Location destination, double userDistance){
        double otherPlayerDistance =0.0;
        Location loc = new Location("user");
        int currentPosition = 1;
        for(int i =0; i<markerLocation.size(); i++){
            loc.setLatitude(markerLocation.get(i).latitude);  //individual persons location
            loc.setLongitude(markerLocation.get(i).longitude); //individual persons location
            otherPlayerDistance = loc.distanceTo(destination);
            if(!(userAtPoint.get(i).equals(user1))) {
                if (userDistance > otherPlayerDistance) {
                    currentPosition = i + 1;
                }
                else{
                    currentPosition = 1;
                }
            }
        }
        TextView playerPlace = (TextView) findViewById(R.id.PlayerPosition);

        if(currentPosition == 1)
            userPlace = String.format("%dst", currentPosition);
        else if(currentPosition == 2)
            userPlace = String.format("%dnd", currentPosition);
        else
            userPlace = String.format("%dth", currentPosition);

        playerPlace.setText(userPlace);
    }
    //----------------------------------------------------------------------------------------------

    //Map listeners +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Override
    public void onMapLongClick(LatLng point) {
        if(!markerCreated) {
            mMap.clear();
            MarkerOptions marker = new MarkerOptions();
            marker.position(point);
            destinationLocation = point;
            mMap.addMarker(marker);
        }
    }
    @Override
    public void onMapClick(LatLng point) {
        marker.remove();
    }
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    // Google Connection Functions/Listeners++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    public void createLocationRequest(){
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(2 * 1000); // 1 second, in milliseconds
    }
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
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

        Log.i(TAG, "Derp Service Connected");
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
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

}