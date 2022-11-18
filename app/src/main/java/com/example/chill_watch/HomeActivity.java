package com.example.chill_watch;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeActivity extends AppCompatActivity {

    ImageButton setup_button;
    Button show_more;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_main);

        ListView listView = (ListView) findViewById(R.id.list_inputs);
        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(adapter);

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

        setup_button = findViewById(R.id.setup_icon);

        setup_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSetupPage();
            }
        });
    }

    public void openSetupPage() {
        Intent intent = new Intent(this, SetupActivity.class);
        startActivity(intent);
    }
}
