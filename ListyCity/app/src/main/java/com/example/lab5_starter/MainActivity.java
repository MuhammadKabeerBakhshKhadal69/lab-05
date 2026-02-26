package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private FirebaseFirestore db;
    private CollectionReference citiesRef;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle("Hold on a city to delete");
        }

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

//        addDummyData();
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");
        setupRealtimeListener();


        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(),"City Details");
        });

        cityListView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            City cityToDelete = cityArrayList.get(i);
            deleteCity(cityToDelete); // Calls the method we wrote above
            return true; // Returns true to consume the click
        });

    }

    // Inside MainActivity.java

    @Override
    public void updateCity(City city, String title, String year) {
        // 1. Identify the document to update (using the old name as the ID)
        DocumentReference docRef = citiesRef.document(city.getName());

        // 2. If the name changed, we actually need to delete the old doc and create a new one
        // because document IDs (the city name) cannot be renamed.
        if (!city.getName().equals(title)) {
            docRef.delete(); // Delete old ID
            citiesRef.document(title).set(new City(title, year)); // Create new ID
        } else {
            // If name is the same, just update the province
            docRef.update("province", year);
        }
    }

    // Add this to handle deletions
    private void deleteCity(City city) {
        citiesRef.document(city.getName())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Document deleted"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error deleting", e));
    }

    @Override
    public void addCity(City city) {
        // DO NOT manually add to cityArrayList or call notifyDataSetChanged here.
        // The SnapshotListener will do that automatically when the DB changes.

        // Using the city name as the document ID
        citiesRef.document(city.getName())
                .set(city)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "City successfully added!"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error writing document", e));
    }

    public void addDummyData(){
        City m1 = new City("Edmonton", "AB");
        City m2 = new City("Vancouver", "BC");
        cityArrayList.add(m1);
        cityArrayList.add(m2);
        cityArrayAdapter.notifyDataSetChanged();
    }

    private void setupRealtimeListener() {
        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", "Listen failed.", error);
                return;
            }

            if (value != null) {
                // Always clear the list first to avoid duplicates
                cityArrayList.clear();

                for (QueryDocumentSnapshot snapshot : value) {
                    // Use the same keys used in your City class/Firestore
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");

                    if (name != null && province != null) {
                        cityArrayList.add(new City(name, province));
                        Log.d("Firestore", "City: " + name + " fetched");
                    }
                }

                // This is the most important part to update the UI
                cityArrayAdapter.notifyDataSetChanged();
            }
        });
}}