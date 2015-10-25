package com.example.sammy.followtheleader;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

public class NewEvent extends AppCompatActivity {

    ArrayList<String> parseUsers = null;
    ArrayList<String> playerArray = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_event);
        parseUsers = new ArrayList<String>();
        playerArray = new ArrayList<String>();
        parseUsers = pullData();
        ListView userList = (ListView) findViewById(R.id.userList);
        final ListView playersList = (ListView) findViewById(R.id.playerList);
        populateUserList(parseUsers, userList);


        userList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                view.animate().setDuration(2000).alpha(0)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                populatePlayersList(item, playersList);
                                view.setAlpha(1);
                            }
                        });
            }

        });

        playersList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                view.animate().setDuration(2000).alpha(0)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                removePlayer(item, playersList);
                                view.setAlpha(1);
                            }
                        });
            }

        });

        Button startEvent = (Button) findViewById(R.id.startButton);
        startEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int eventID = getEventType();
                if(eventID != 0)
                    procedeTomap(playerArray, eventID);
            }
        });
    }

    private int getEventType () {
        RadioButton eventType1 = (RadioButton) findViewById(R.id.toDest);
        RadioButton eventType2 = (RadioButton) findViewById(R.id.followMe);

        if(eventType1.isChecked()){
            return 1;
        }
        else if(eventType2.isChecked()){
            return 2;
        }
        else {
            int duration = Toast.LENGTH_SHORT;
            Context context = getApplicationContext();
            CharSequence text = "Please Choose a Type of Event";

            Toast toast = Toast.makeText(context, text, duration);
            RadioButton r =(RadioButton) findViewById(R.id.toDest);
            r.requestFocus();
            toast.show();
            return 0;
        }

    }
    public void procedeTomap (ArrayList<String> playerList,int eventID) {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("peoplePlaying", playerList);
        intent.putExtra("eventType", eventID);
        startActivity(intent);

    }

    private void removePlayer(String item, ListView playerList){
        playerArray.remove(item);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, playerArray);
        playerList.setAdapter(arrayAdapter);
    }

    private void populatePlayersList(String item, ListView playerList){
        playerArray.add(item);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, playerArray);
        playerList.setAdapter(arrayAdapter);
    }

    private void populateUserList(ArrayList<String> userArray, ListView userList){
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, userArray);
        userList.setAdapter(arrayAdapter);
    }


    public ArrayList<String> pullData(){

        final ArrayList<String> userName = new ArrayList<String>();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Userinfo");
//        query.whereEqualTo("password", password);
        query.findInBackground(new FindCallback<ParseObject>() {

            public void done(List<ParseObject> list, com.parse.ParseException e) {
                for (ParseObject parseObject : list) {
                    userName.add(parseObject.get("user_email").toString());
                }
            }
        });
        return userName;
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_new_event, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
////        if (id == R.id.action_settings) {
////            return true;
////        }
//
//        return super.onOptionsItemSelected(item);
//    }
}
