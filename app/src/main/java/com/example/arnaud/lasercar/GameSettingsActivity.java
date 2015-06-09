package com.example.arnaud.lasercar;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.NumberPicker;


public class GameSettingsActivity extends ActionBarActivity
{
    private NumberPicker np;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_settings);

        NumberPicker np = (NumberPicker) findViewById(R.id.np_player);
        np.setMaxValue(6);
        np.setMinValue(2);

        np.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // désactive clavier pour le NumberPicker
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

    /* ================================================================================ */
    /* =========================== ANIMATION BOUTON RETOUR ============================ */
    /* ================================================================================ */
    // Retour du smartphone
    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        // public void overridePendingTransition (int enterAnim, int exitAnim)
        overridePendingTransition(R.anim.left_to_in, R.anim.in_to_right);
    }
    // Retour de l'ActionBar
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
            overridePendingTransition(R.anim.left_to_in, R.anim.in_to_right);
            return true;
        }
        return false;
    }
}
