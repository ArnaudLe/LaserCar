package com.example.arnaud.lasercar;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.MenuItem;


public class AboutActivity extends ActionBarActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
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
