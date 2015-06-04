package com.example.arnaud.lasercar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


public class HomeActivity extends Activity
{
    // Redéfinition de la fonction principale
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homescreen);
        PlayActivity.flagPlayActivity = false; // Flag indiquant que l'on n'est pas dans PlayActivity
    }

    /* Appelee quand l'utilisateur appuie sur le bouton Jouer ! */
    public void playMessage(View view)
    {
        Intent intent = new Intent(this, PlayActivity.class);
        startActivity(intent);
    }

    /* Appelee quand l'utilisateur appuie sur le bouton Options */
    public void settingsMessage(View view)
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
