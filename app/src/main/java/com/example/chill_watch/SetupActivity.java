package com.example.chill_watch;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.clj.fastble.data.BleDevice;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.exception.BleException;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.fragment.NavHostFragment;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SetupActivity extends AppCompatActivity {

    private static final int BATCH_SIZE = 32;
    private static final int CONTENT_VIEW_ID = 10101010;


    ImageButton home_button;
    Button clear_button;
    ImageButton add_ble;
    ImageButton add_company;
    ImageButton sign_in_button;
    String company_name = "Test";
    String account_username = "Test User";
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    Integer krabbyPatties = 0;
    Integer batchNum = 0;
    Integer curListVal = 0;
    List<Map<String, Object>> batchList = new ArrayList<>();

    public String Username = "Test";

    final private int REQUEST_CODE_PERMISSION_LOCATION = 0;
    private AlertDialog.Builder dialogBuilder;
    private BleDeviceAdapter bleDeviceAdapter;
    private BleDevice activeBleDevice;
    private TextView companyName;
    private TextView signin_username;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_main);

        Intent intent = getIntent();
        String companyString = intent.getStringExtra("companyName");
        companyName = findViewById(R.id.company_id);

        String accountUsername = intent.getStringExtra("accountUsername");
        signin_username = findViewById(R.id.signin_username);

        if (accountUsername != null && !accountUsername.isEmpty()) {
            signin_username.setText(accountUsername);
            account_username = accountUsername;
        }

        if (companyString != null && !companyString.isEmpty()) {
            companyName.setText(companyString);
            company_name = companyString;
        }

        bleDeviceAdapter = new BleDeviceAdapter(SetupActivity.this, android.R.layout.select_dialog_singlechoice);

        //build dialog for scanning BLE devices
        dialogBuilder = new AlertDialog.Builder(SetupActivity.this);
        dialogBuilder.setIcon(R.drawable.ic_launcher_foreground);
        dialogBuilder.setTitle("Select a HEM device");

        dialogBuilder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialogBuilder.setAdapter(bleDeviceAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BleDevice bleDevice = bleDeviceAdapter.getItem(which);
                AlertDialog.Builder builderInner = new AlertDialog.Builder(SetupActivity.this);
                builderInner.setMessage(bleDevice.getName() + ", " + bleDevice.getMac());
                builderInner.setTitle("Your selected HEM device is");
                builderInner.setPositiveButton("Connect and Subscribe", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,int which) { connect(bleDevice); }
                });
                builderInner.show();
            }
        });

        add_ble = findViewById(R.id.bluetooth_button);
        add_ble.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScan();
            }
        });


        Username = getResources().getString(R.string.username);

        home_button = findViewById(R.id.home_icon_setup);
        home_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //NavHostFragment navHostFragment = (NavHostFragment)getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                //MainFragment fragment = (MainFragment)navHostFragment.getChildFragmentManager().getFragments().get(0);
                //fragment.updateDeviceTextView(activeBleDevice.getName() + ", " + activeBleDevice.getMac());
                //openHomePage();
            }
        });


        add_company = findViewById(R.id.company_button);
        add_company.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCompanyPage();
            }
        });

        sign_in_button = findViewById(R.id.signin_button);
        sign_in_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSignInPage();
            }
        });

        ListView listView = (ListView) findViewById(R.id.list_inputs);
        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(adapter);

        TextView status_message = findViewById(R.id.status);
        TextView skin_temp = findViewById(R.id.temperatureValue);
        FloatingActionButton syntheticButton = findViewById(R.id.addSyntheticButton);

        final DocumentReference docRef = db.collection("Users").document(account_username);
        //final DocumentReference docRef = db.collection("TestData").document(String.valueOf(batchNum));

        //final DocumentReference docRef = db.collection("Synthetic").document("Synth");
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

        syntheticButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(MainActivity.this, HexUtil.formatHexString(data, true), Toast.LENGTH_SHORT).show();
                        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
                        //batchNum++;

                        dataElement syntheticDataPoint = generateData();
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
                        //db.collection("Synthetic").document("Synth").set(reading, SetOptions.merge());
                        // batchNum++;
                        db.collection("Users").document(account_username).set(reading, SetOptions.merge());

                    }
                });
            }
        });


        checkPermissions();

        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000)
                .setOperateTimeout(5000);
    }
    public class dataElement {
        public String AmbientTemp;
        public String SkinTemp;
        public String Code;
    }

    public dataElement generateData() {
        dataElement result = new dataElement();
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

    public void onSignOut() {
        FirebaseAuth.getInstance().signOut();
    }

    public void openCompanyPage() {
        Intent intent = new Intent(this, CompanyActivity.class);
        intent.putExtra("accountUsername", account_username.toString());
        intent.putExtra("companyName", company_name.toString());
        intent.putExtra("krabbyPatties", batchNum);
        startActivity(intent);
    }

    public void openHomePage() {
        Intent intent = new Intent(SetupActivity.this, HomeActivity.class);
        intent.putExtra("accountUsername", account_username.toString());
        intent.putExtra("companyName", company_name.toString());
        intent.putExtra("krabbyPatties", krabbyPatties);
        startActivity(intent);
    }

    public void openSignInPage() {
        Intent intent = new Intent(this, SignIn.class);
        intent.putExtra("accountUsername", account_username.toString());
        intent.putExtra("companyName", company_name.toString());
        intent.putExtra("krabbyPatties", batchNum);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().destroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //BLE
    private void startScan() {
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                bleDeviceAdapter.clear();
                bleDeviceAdapter.notifyDataSetChanged();
                dialogBuilder.show();
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                /*
                String name = bleDevice.getName();
                String searchString = "HEM_";
                if (name != null && !name.isEmpty()) {
                    String subName = name.substring(0, searchString.length()-1);
                    if (searchString == subName) {
                        bleDeviceAdapter.add(bleDevice);
                        bleDeviceAdapter.notifyDataSetChanged();
                    }
                }
                 */
                bleDeviceAdapter.add(bleDevice);
                bleDeviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {

            }
        });
    }

    private void connect(final BleDevice bleDevice) {
        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                Toast.makeText(SetupActivity.this, "Failed to connect.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                activeBleDevice = bleDevice;



                Toast.makeText(SetupActivity.this, "Connected.", Toast.LENGTH_LONG).show();

                setContentView(R.layout.setup_main);
                TextView bluetooth_id = findViewById(R.id.bluetooth_id);
                bluetooth_id.setText(activeBleDevice.getName() + ", " + activeBleDevice.getMac());

                // NavHostFragment navHostFragment = (NavHostFragment)getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                // MainFragment fragment = (MainFragment)navHostFragment.getChildFragmentManager().getFragments().get(0);
                // fragment.updateDeviceTextView(activeBleDevice.getName() + ", " + activeBleDevice.getMac());

                BluetoothGattCharacteristic notifyCharacteristic = null;

                for (BluetoothGattService bgs: gatt.getServices()) {
                    for (BluetoothGattCharacteristic bgc: bgs.getCharacteristics()) {
                        int property = bgc.getProperties();
                        if ((property & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            notifyCharacteristic = bgc;
                            break;
                        }
                    }
                }

                BleManager.getInstance().notify(
                        bleDevice,
                        notifyCharacteristic.getService().getUuid().toString(),
                        notifyCharacteristic.getUuid().toString(),
                        new BleNotifyCallback() {

                            @Override
                            public void onCharacteristicChanged(byte[] data) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //Toast.makeText(MainActivity.this, HexUtil.formatHexString(data, true), Toast.LENGTH_SHORT).show();
                                        Toast.makeText(SetupActivity.this, new String(data), Toast.LENGTH_LONG).show();
                                        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

                                        String output = new String(data);
                                        String[] values = output.split(" ");
                                        String username = "TestUser";
                                        String id = Long.toString(System.currentTimeMillis());
                                        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                                        if (values.length == 3) {
                                            krabbyPatties++;

                                            // Test App Store:

                                            Map<String, Object> reading = new HashMap<>();
                                            reading.put("Time", date);
                                            if (values[0] != null) {
                                                reading.put("AmbientTemp", values[0]);
                                            }
                                            if (values[1] != null) {
                                                reading.put("SkinTemp", values[1]);
                                            }
                                            if (values[2] != null) {
                                                reading.put("Code", values[2]);
                                            }
                                            //reading.put("AmbientTemp", values[0]);
                                            //reading.put("SkinTemp", values[1]);
                                            //reading.put("Code", values[2]);
                                            batchList.add(reading);
                                            //db.collection("TestData").document(krabbyPatties.toString()).set(reading, SetOptions.merge());



                                            // Real App Store:
                                            /*
                                            Map<String, Object> reading = new HashMap<>();
                                            reading.put("Company", companyName);
                                            reading.put("Time", date);
                                            reading.put("AmbientTemp", values[0]);
                                            reading.put("SkinTemp", values[1]);
                                            reading.put("Code", values[2]);
                                            batchList.add(reading);
                                            db.collection("Users").document(account_username).set(reading, SetOptions.merge());
                                            */

                                            if (batchList.size() == BATCH_SIZE) {
                                                sumbitBatch();
                                            }

                                            /*
                                            database.child("inputs").child(id).child("time").setValue(date);
                                            database.child("inputs").child(id).child("skin_temp").setValue(values[0]);
                                            database.child("inputs").child(id).child("ambient").setValue(values[1]);
                                            database.child("inputs").child(id).child("code").setValue(values[2]);

                                             */
                                        }

                                    }
                                });
                            }

                            @Override
                            public void onNotifySuccess() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(SetupActivity.this, "notify success", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onNotifyFailure(final BleException exception) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(SetupActivity.this, "notify failed", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                        });

            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {

            }
        });
    }

    //Permission
    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (permissionDeniedList != null && !permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_LOCATION:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            onPermissionGranted(permissions[i]);
                        }
                    }
                }
                break;
        }
    }

    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    Toast.makeText(getApplicationContext(), "Permissions are granted", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Permissions are granted", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    public void sumbitBatch() {
        for (Map<String, Object> curData : batchList) {
            //db.collection("TestData").document(batchNum.toString()).set(curData, SetOptions.merge());
             db.collection("Users").document(account_username).set(curData, SetOptions.merge());
            batchNum += 1;
        }
        batchList.clear();
    }
}
