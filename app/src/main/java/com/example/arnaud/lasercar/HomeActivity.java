package com.example.arnaud.lasercar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;


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
        Intent intent = new Intent(this, GameSettingsActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.right_to_in, R.anim.in_to_left);
    }

    /* Appelee quand l'utilisateur appuie sur le bouton Options */
    public void settingsMessage(View view)
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.down_to_in, R.anim.in_to_up);
    }
}
