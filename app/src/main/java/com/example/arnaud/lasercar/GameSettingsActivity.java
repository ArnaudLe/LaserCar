package com.example.arnaud.lasercar;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;


public class GameSettingsActivity extends ActionBarActivity
{
    private EditText pseudo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_settings);
    }

    // Masque le clavier après un clic ailleurs que l'EditText
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.
                INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        return true;
    }

    /* Appelee quand l'utilisateur appuie sur le bouton Valider*/
    public void sendData(View view)
    {
        Intent intent = new Intent(this, PlayActivity.class);
        startActivity(intent);
    }
}
