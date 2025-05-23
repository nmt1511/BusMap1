package com.example.busmap.Route.RouteDetail;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.busmap.R;
import com.example.busmap.entities.BusStop;
import com.example.busmap.entities.station;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BusRouteActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private DatabaseReference databaseRef;
    private List<LatLng> stationLocations = new ArrayList<>();
    private View bottomSheet;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private ViewPager2 viewPager;
    private ArrayList<station> StationList = new ArrayList<>();
    private List<Integer> stationIds = new ArrayList<>();
    private Map<Integer, Marker> stationMarkers = new HashMap<>();
    private int selectedStationId = -1; // Lưu ID của trạm đang chọn
    String routeId;

    void init() {
        viewPager = findViewById(R.id.view_pager);
        bottomSheet = findViewById(R.id.bottom_sheet_layout);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_detail);
        init();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        routeId = getIntent().getStringExtra("route_id");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.maproute);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        setHalfScreenHeight(); // Đặt Bottom Sheet chiếm 1/2 màn hình
        setupBottomSheetCallback();
        setupTabLayout();
    }
    private void setHalfScreenHeight() {
        bottomSheet.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int halfScreenHeight = screenHeight / 3; // Lấy 33% chiều cao màn hình
            bottomSheetBehavior.setPeekHeight(halfScreenHeight); // Mở 1/2 màn hình
        });
    }

    private BitmapDescriptor bitmapDescriptorFromVector(int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(this, vectorResId);
        if (vectorDrawable == null) {
            Log.e("BusRouteActivity", "Không tìm thấy icon: " + vectorResId);
            return BitmapDescriptorFactory.defaultMarker(); // Trả về marker mặc định nếu lỗi
        }

        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


    public void moveToStation(double latitude, double longitude, int stationId) {
        if (mMap != null) {
            LatLng stationLocation = new LatLng(latitude, longitude);
            float zoomLevel = 15;

            // Điều chỉnh vị trí camera để marker nằm cao hơn trên màn hình
            LatLng adjustedPosition = adjustCameraPosition(stationLocation);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(adjustedPosition, zoomLevel));

            // Cập nhật marker
            if (stationMarkers.containsKey(stationId)) {
                if (selectedStationId != -1 && stationMarkers.containsKey(selectedStationId)) {
                    stationMarkers.get(selectedStationId).setIcon(bitmapDescriptorFromVector(R.drawable.ic_station_big));
                }

                stationMarkers.get(stationId).setIcon(bitmapDescriptorFromVector(R.drawable.ic_station_focus));
                selectedStationId = stationId;
            }
        } else {
            Log.e("BusRouteActivity", "Google Map chưa sẵn sàng!");
        }
    }



    private LatLng adjustCameraPosition(LatLng originalPosition) {
        Projection projection = mMap.getProjection();
        Point screenPoint = projection.toScreenLocation(originalPosition);

        int offsetY = -300; // Dịch bản đồ xuống 300 pixel (có thể điều chỉnh)
        screenPoint.set(screenPoint.x, screenPoint.y - offsetY);

        return projection.fromScreenLocation(screenPoint);
    }


    private void setupBottomSheetCallback() {
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    Log.d("BottomSheet", "Mở toàn màn hình");
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    Log.d("BottomSheet", "Thu nhỏ");
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (mMap != null) {
                    mMap.setPadding(0, 0, 0, (int) (slideOffset * bottomSheet.getHeight() / 2));
                }
            }
        });
    }



    private void setupTabLayout() {
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        // Lấy routeId từ Intent
        //String routeId = getIntent().getStringExtra("route_id");
        if (routeId == null) {
            Log.e("BusRouteActivity", "routeId không tìm thấy");
            return;
        }

        // Truyền routeId vào Adapter
        RoutePagerAdapter adapter = new RoutePagerAdapter(this, routeId);
        viewPager.setAdapter(adapter);

        // Kết nối TabLayout với ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Biểu đồ giờ");
                    break;
                case 1:
                    tab.setText("Trạm dừng");
                    break;
                case 2:
                    tab.setText("Thông tin");
                    break;
                case 3:
                    tab.setText("Đánh Giá");
                    break;

            }
        }).attach();
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        loadRouteStations();
    }

    private void loadRouteStations() {
        if (routeId != null) {
            Log.e("Firebase_ID_route", "Route_id: "+ routeId);
            databaseRef.child("busstop").orderByChild("route_id").equalTo(routeId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot busStopSnapshot) {
                            if (!busStopSnapshot.exists()) {
                                Log.e("Firebase", "No data found for route_id: " + routeId);
                                return;
                            }
                            List<Integer> stationIds = new ArrayList<>();
                            for (DataSnapshot snapshot : busStopSnapshot.getChildren()) {
                                Log.d("Firebase", "Snapshot: " + snapshot.getValue());
                                Integer stationId = snapshot.child("station_id").getValue(Integer.class);
                                if (stationId != null) {
                                    stationIds.add(stationId);
                                } else {
                                    Log.e("Firebase", "Station ID is null for some bus stop.");
                                }
                            }
                            // Use the list of station IDs as needed
                            Log.d("Firebase", "Station IDs: " + stationIds.toString());
                            loadStationDetails(stationIds);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("Firebase", "Failed to fetch bus stops", error.toException());
                        }
                    });
        } else {
            Log.e("BusRouteActivity", "Route ID is null.");
        }
    }
    private void loadStationDetails(List<Integer> stationIds) {
        databaseRef.child("station").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot stationSnapshot) {
                Map<Integer, station> stationMap = new HashMap<>();

                if (BusRouteActivity.this.isFinishing() || mMap == null) {
                    Log.e("BusRouteActivity", "Activity đã bị hủy hoặc Google Map chưa sẵn sàng.");
                    return;
                }

                stationMarkers.clear(); // Xóa các marker cũ trước khi tải mới

                for (DataSnapshot snapshot : stationSnapshot.getChildren()) {
                    Integer id = snapshot.child("id").getValue(Integer.class);
                    Double lat = snapshot.child("lat").getValue(Double.class);
                    Double lng = snapshot.child("lng").getValue(Double.class);
                    String name = snapshot.child("name").getValue(String.class);

                    // Kiểm tra các giá trị null
                    if (id == null || lat == null || lng == null || name == null) {
                        Log.e("Firebase", "Dữ liệu trạm không hợp lệ, bỏ qua.");
                        continue;
                    }

                    if (stationIds.contains(id)) {
                        // Thêm station vào danh sách
                        stationMap.put(id, new station(id, name, lat, lng));

                        LatLng location = new LatLng(lat, lng);

                        // Chạy trên UI Thread để cập nhật giao diện
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (mMap != null) {
                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(location)
                                        .title(name)
                                        .icon(bitmapDescriptorFromVector(R.drawable.ic_station_big))); // Sử dụng icon mới

                                // Kiểm tra và gắn marker vào map
                                if (marker != null) {
                                    stationMarkers.put(id, marker);
                                }
                            }
                        });
                    }
                }

                // Cập nhật danh sách các trạm
                StationList.clear();
                for (int id : stationIds) {
                    if (stationMap.containsKey(id)) {
                        StationList.add(stationMap.get(id));
                    }
                }

                // Cập nhật danh sách tọa độ
                stationLocations.clear();
                for (station sta : StationList) {
                    stationLocations.add(new LatLng(sta.getLat(), sta.getLng()));
                }

                // Vẽ polyline sau khi đã có danh sách trạm dừng
                drawPolyline();

                // Cập nhật camera để hiển thị trạm dừng đầu tiên
                if (!stationLocations.isEmpty()) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stationLocations.get(0), 14));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to fetch stations", error.toException());
            }
        });
    }


    private void drawPolyline() {
        if (stationLocations.size() > 1) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(stationLocations)
                    .width(8)
                    .color(ContextCompat.getColor(this, R.color.green));  // Use ContextCompat for compatibility
            mMap.addPolyline(polylineOptions);
        }
    }

}