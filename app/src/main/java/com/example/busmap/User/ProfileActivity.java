package com.example.busmap.User;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.busmap.R;
import com.example.busmap.UserManager;
import com.example.busmap.account.RatingActivity;
import com.example.busmap.account.infocompany;
import com.example.busmap.entities.user;

public class ProfileActivity extends AppCompatActivity {

    private LinearLayout btnUpgrade;
    private TextView txtPersonalInfo, txtSettings, txtRateApp, txtCompanyInfo, txtTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Ánh xạ View
        btnUpgrade = findViewById(R.id.btnUpgrade);
        txtPersonalInfo = findViewById(R.id.txtPersonalInfo);
        txtRateApp = findViewById(R.id.txtRateApp);
        txtCompanyInfo = findViewById(R.id.txtCompanyInfo);
        txtTitle = findViewById(R.id.txtTitle);

        user user_info = UserManager.getUserFromSharedPreferences(this);
        txtTitle.setText(user_info.getName());

        // Xử lý sự kiện khi nhấn vào "Nâng cấp"
        btnUpgrade.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng nâng cấp chưa khả dụng", Toast.LENGTH_SHORT).show();
        });

        // Khi nhấn vào "Thông tin cá nhân" → mở `AccountActivity`
        txtPersonalInfo.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, AccountActivity.class);
            startActivity(intent);
        });



        // Khi nhấn vào "Đánh giá ứng dụng" → mở `RatingActivity`
        txtRateApp.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, RatingActivity.class);
            startActivity(intent);
        });

        // Khi nhấn vào "Thông tin công ty" → mở `InfoCompanyActivity`
        txtCompanyInfo.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, infocompany.class);
            startActivity(intent);
        });
    }
}
