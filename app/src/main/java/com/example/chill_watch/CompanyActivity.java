package com.example.chill_watch;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class CompanyActivity extends AppCompatActivity {

    EditText company_name;
    EditText company_code;
    MaterialButton register_button;
    MaterialButton back_button;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.company_main);

        company_name = findViewById(R.id.company_name);
        company_code = findViewById(R.id.company_code);
        register_button = findViewById(R.id.register);
        back_button = findViewById(R.id.company_back);

        register_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String companyId = company_name.getText().toString();
                String companyCode = company_code.getText().toString();
                final DocumentReference docRef = db.collection("Companies").document(companyId);

                docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("New Data", "Listen failed.", e);
                            return;
                        }

                        if (snapshot != null && snapshot.exists()) {
                            Log.d("New Data", "Current data: " + snapshot.getData());
                            String companyField = snapshot.getString("code");
                            if (snapshot.getString("code").equals(companyCode)) {
                                addUserToCompany(companyId);
                            }
                        } else {
                            Log.d("New Data", "No such document");
                        }
                    }
                });
            }
        });

        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CompanyActivity.this, SetupActivity.class);
                startActivity(intent);
            }
        });

    }
    public void addUserToCompany(String companyId) {
        Intent intent = new Intent(CompanyActivity.this, SetupActivity.class);
        intent.putExtra("companyString", companyId);
        startActivity(intent);
    }
}
