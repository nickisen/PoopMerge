package com.gif.poopmerge; // KORRIGIERT

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lege die GameView direkt als Inhaltsansicht fest
        setContentView(new GameView(this));
    }
}