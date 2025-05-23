package com.example.busmap.Route.FindRoute;

import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.busmap.FindRouteHelper.LocationManager;
import com.example.busmap.R;
import com.example.busmap.FindRouteHelper.LocationData;
import com.example.busmap.entities.SearchHistory;
import com.example.busmap.entities.station;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InputFindActivity extends AppCompatActivity {
    private LinearLayout linear_CrtLocation, linear_OnMap;
    private ImageView ImgBack;
    private AutoCompleteTextView edtInput;
    private ActivityResultLauncher<Intent> resultLauncher;
    //private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference database;
    private ArrayAdapter<String> adapter;
    private List<station> stationList = new ArrayList<>();
    private station selectedStation;
    private String userId;
    private RecyclerView rv_HistorySearch;
    private SearchHistoryAdapter historyAdapter;
    private List<SearchHistory> historyList = new ArrayList<>();
    private Map<String, SearchHistory> historyMap = new HashMap<>();

    void init(){
        linear_CrtLocation = findViewById(R.id.Linear_CrtLocation);
        linear_OnMap = findViewById(R.id.Linear_OnMap);
        edtInput = findViewById(R.id.searchInput);
        ImgBack = findViewById(R.id.imgback);
        rv_HistorySearch = findViewById(R.id.rV_SearchHistory);
        rv_HistorySearch.setLayoutManager(new LinearLayoutManager(this));

        //fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        database = FirebaseDatabase.getInstance().getReference();
    }

    void initListener(){
        ImgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentBack = new Intent(InputFindActivity.this,FindRouteActivity.class);
                startActivity(intentBack);
            }
        });
        linear_CrtLocation.setOnClickListener(view -> {
            LatLng userLocation = LocationManager.getInstance().getLatLng();
            LocationData currentLocation = new LocationData(userLocation.latitude, userLocation.longitude, "[ Vị trí hiện tại ]");
            Intent resultIntent = new Intent();
            resultIntent.putExtra("Selected_Location", currentLocation);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
        linear_OnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InputFindActivity.this, FindLocationOnMapActivity.class);
                resultLauncher.launch(intent);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_find);
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        init();
        initListener();

        // Khởi tạo adapter
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        edtInput.setAdapter(adapter);

        historyAdapter = new SearchHistoryAdapter(history -> {
            selectedStation = history.getStation();
            LocationData location = new LocationData(
                    selectedStation.getLat(),
                    selectedStation.getLng(),
                    selectedStation.getName()
            );
            Intent resultIntent = new Intent();
            resultIntent.putExtra("Selected_Location", location);
            setResult(RESULT_OK, resultIntent);
            finish();
        }, history -> {
            // Xử lý xóa item
            deleteSearchHistory(history);
        });
        rv_HistorySearch.setAdapter(historyAdapter);

        // Load lịch sử tìm kiếm
        loadSearchHistory();

        fetchStationsFromFirebase();


        edtInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedName = (String) adapterView.getItemAtPosition(i);
                for (station station : stationList) {
                    if (station.getName().equals(selectedName)) {
                        selectedStation = station;
                        // Lưu vào lịch sử khi chọn
                        saveToSearchHistory(selectedStation);
                        break;

                    }
                }
               Toast.makeText(InputFindActivity.this, "lat: " + selectedStation.getLat() + "\nlng: "+ selectedStation.getLng()+"\nName: "+ selectedStation.getName(), Toast.LENGTH_SHORT).show();
                LocationData DreamLocation = new LocationData(selectedStation.getLat(), selectedStation.getLng(), selectedStation.getName());
                Intent resultIntent = new Intent();
                resultIntent.putExtra("Selected_Location", DreamLocation);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Lấy dữ liệu từ Activity FindOnMapActivity
                        LocationData locationData = result.getData().getParcelableExtra("Selected_Location");

                        // Trả kết quả về Activity FindRouteActivity
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("Selected_Location", locationData);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                });

    }


    private void fetchStationsFromFirebase() {
        database.child("station").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> stationNames = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    station station = dataSnapshot.getValue(station.class);
                    if (station != null) {
                        stationList.add(station);
                        stationNames.add(station.getName());
                    }
                }
                adapter.clear();
                adapter.addAll(stationNames);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(InputFindActivity.this, "Lỗi khi lấy dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteSearchHistory(SearchHistory history) {
        String key = null;
        for (Map.Entry<String, SearchHistory> entry : historyMap.entrySet()) {
            if (entry.getValue().getTimestamp() == history.getTimestamp()) {
                key = entry.getKey();
                break;
            }
        }

        if (key != null) {
            database.child("search_history").child(userId).child(key).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã xóa lịch sử", Toast.LENGTH_SHORT).show();
                        loadSearchHistory(); // Tải lại sau khi xóa
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }


    private void saveToSearchHistory(station station) {
        if (station == null) {
            Log.e("SearchHistory", "Station null");
            Toast.makeText(this, "Lỗi: Station null", Toast.LENGTH_SHORT).show();
            return;
        }

        SearchHistory history = new SearchHistory(userId, station);
        DatabaseReference userHistoryRef = database.child("search_history").child(userId);

        userHistoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int newIndex = (int) snapshot.getChildrenCount();
                userHistoryRef.child(String.valueOf(newIndex))
                        .setValue(history)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("SearchHistory", "Lưu lịch sử thành công tại: " + userId + "/" + newIndex);
                            loadSearchHistory();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("SearchHistory", "Lỗi lưu lịch sử: " + e.getMessage());
                            Toast.makeText(InputFindActivity.this, "Lỗi lưu lịch sử: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SearchHistory", "Lỗi truy vấn số lượng: " + error.getMessage());
            }
        });
    }

    private void loadSearchHistory() {
        DatabaseReference historyRef = database.child("search_history").child(userId);

        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        historyList.clear();

                        if (!snapshot.exists()) {
                            Log.d("SearchHistory", "Không có dữ liệu trong snapshot");
                            Toast.makeText(InputFindActivity.this, "Không có lịch sử tìm kiếm", Toast.LENGTH_SHORT).show();
                            historyAdapter.setHistoryList(historyList);
                            return;
                        }

                        for (DataSnapshot data : snapshot.getChildren()) {
                            SearchHistory history = data.getValue(SearchHistory.class);
                            if (history != null && history.getStation() != null) {
                                String key = data.getKey();
                                historyList.add(history);
                                historyMap.put(key, history);
                                Log.d("SearchHistory", "Station: " + history.getStation().getName());
                            } else {
                                Log.e("SearchHistory", "Dữ liệu không hợp lệ tại: " + data.getKey());
                            }
                        }
                        Collections.sort(historyList, (h1, h2) ->
                                Long.compare(h2.getTimestamp(), h1.getTimestamp()));
                        historyAdapter.setHistoryList(historyList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("SearchHistory", "Lỗi Firebase: " + error.getMessage());
                        Toast.makeText(InputFindActivity.this, "Lỗi tải lịch sử: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
//    private void getPlacePredictions(String query) {
//        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();
//        FindAutocompletePredictionsRequest request =
//                FindAutocompletePredictionsRequest.builder()
//                        .setSessionToken(token)
//                        .setQuery(query)
//                        .build();
//
//        placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
//            placeSuggestions.clear();
//            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
//                placeSuggestions.add(prediction.getPrimaryText(null).toString());
//            }
//            updateAutoCompleteTextView();
//        }).addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
//
//    }
//
//    private void updateAutoCompleteTextView() {
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, placeSuggestions);
//        edtInput.setAdapter(adapter);
//    }

}
