package com.example.kasparasza.inventoryapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.kasparasza.inventoryapp.AllInventoryViewActivity;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, AllInventoryViewActivity.class);
        startActivity(intent);
        finish();
    }
}
