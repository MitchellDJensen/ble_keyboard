package com.example.chill_watch;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class HomeActivity extends AppCompatActivity {

    ImageButton setup_button;
    Button clear_button;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String company_name = "Test";
    String account_username = "Test User";
    int krabbyPatties = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_main);

        Intent intent = getIntent();
        String accountUsername = intent.getStringExtra("accountUsername");
        String companyName = intent.getStringExtra("companyName");
        krabbyPatties = intent.getIntExtra("krabbyPatties", 0);

        if (accountUsername != null && !accountUsername.isEmpty()) {
            account_username = accountUsername;
        }
        if (companyName != null && !companyName.isEmpty()) {
            company_name = companyName;
        }

        ListView listView = (ListView) findViewById(R.id.list_inputs);
        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(adapter);

        TextView status_message = findViewById(R.id.status);
        TextView skin_temp = findViewById(R.id.temperatureValue);
        FloatingActionButton syntheticButton = findViewById(R.id.addSyntheticButton);

        //final DocumentReference docRef = db.collection("Users").document(account_username);
        final DocumentReference docRef = db.collection("TestData").document(String.valueOf(krabbyPatties));
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            /*
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {

            }

             */

            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("New Data", "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d("New Data", "Current data: " + snapshot.getData());
                    adapter.add(snapshot.get("Time") + ", Skin: "+ snapshot.get("SkinTemp") + ", Ambient: " + snapshot.get("AmbientTemp"));

                    String code = snapshot.get("Code").toString();
                    // int code = (int) codeStr;
                    if (code == "0") {
                        status_message.setText("All Clear!");
                        status_message.setTextColor(Color.GREEN);
                    } else if (code == "1") {
                        status_message.setText("They Ain't Wearin It");
                        status_message.setTextColor(Color.BLUE);
                    } else if (code == "2") {
                        status_message.setText("HAZARDOUS SKIN TEMP");
                        status_message.setTextColor(Color.RED);
                    } else if (code == "3") {
                        status_message.setText("Caution Hot Weather");
                        status_message.setTextColor(Color.YELLOW);
                    } else if (code == "4") {
                        status_message.setText("HAZARDOUS WEATHER");
                        status_message.setTextColor(Color.RED);
                    }

                    String skinTemp = snapshot.get("SkinTemp").toString();
                    float tempValue = Float.parseFloat(skinTemp);
                    int showTemp = Math.round(tempValue);
                    if (showTemp != 0) {
                        skin_temp.setText(showTemp + "\u00B0C");
                    }

                    adapter.notifyDataSetChanged();
                } else {
                    Log.d("New Data", "Current data: null");
                }
            }
        });

        /*
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                //Post post = dataSnapshot.getValue(Post.class);
                // ..
                adapter.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                    Log.d("New Data", ds.getKey() + "," + ds.getChildrenCount() + "," + ds.child("time").getValue() + ", " +  ds.child("value").getValue());
                    for (DataSnapshot ds1: ds.getChildren()) {
                        Log.d("New Data", ds1.getKey() + "," + ds1.getChildrenCount() + "," + ds1.child("time").getValue() + ", " +  ds1.child("value").getValue());
                        adapter.add(ds1.child("value").getValue() + ", " + ds1.child("time").getValue());
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w("Error", "loadPost:onCancelled", databaseError.toException());
            }
        };

         */

        clear_button = findViewById(R.id.clear_data_button);

        clear_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter.clear();
            }
        });

        setup_button = findViewById(R.id.setup_icon);

        setup_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSetupPage();
            }
        });

        syntheticButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(MainActivity.this, HexUtil.formatHexString(data, true), Toast.LENGTH_SHORT).show();
                        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

                        data syntheticDataPoint = generateData();
                        //String[] values = output.split(" ");
                        //String username = "";
                        String id = Long.toString(System.currentTimeMillis());
                        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        Map<String, Object> reading = new HashMap<>();
                        reading.put("Company", "test");
                        reading.put("Time", date);
                        reading.put("AmbientTemp", syntheticDataPoint.AmbientTemp);
                        reading.put("SkinTemp", syntheticDataPoint.SkinTemp);
                        reading.put("Code", syntheticDataPoint.Code);
                        db.collection("TestData").document(String.valueOf(krabbyPatties)).set(reading, SetOptions.merge());
                        //db.collection("Users").document(account_username).set(reading, SetOptions.merge());

                    }
                });
            }
        });
    }

    public class data {
        public String AmbientTemp;
        public String SkinTemp;
        public String Code;
    }

    public data generateData() {
        data result = new data();
        int num;
        num = ThreadLocalRandom.current().nextInt(0, 4 + 1);
        if (num == 0) {
            result.AmbientTemp = "20.123";
            result.SkinTemp = "35.321";
            result.Code = "0";
        }
        else if (num == 1) {
            result.AmbientTemp = "22.543";
            result.SkinTemp = "21.876";
            result.Code = "1";
        }
        else if (num == 2) {
            result.AmbientTemp = "21.283";
            result.SkinTemp = "38.298";
            result.Code = "2";
        }
        else if (num == 3) {
            result.AmbientTemp = "39.543";
            result.SkinTemp = "37.283";
            result.Code = "3";
        }
        else if (num == 4) {
            result.AmbientTemp = "50.109";
            result.SkinTemp = "36.298";
            result.Code = "4";
        }
        return result;
    }
    public void openSetupPage() {
        Intent intent = new Intent(HomeActivity.this, SetupActivity.class);
        intent.putExtra("accountUsername", account_username.toString());
        intent.putExtra("companyName", company_name.toString());
        startActivity(intent);
    }
}
