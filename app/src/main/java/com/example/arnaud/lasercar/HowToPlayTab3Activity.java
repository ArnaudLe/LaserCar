package com.example.arnaud.lasercar;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class HowToPlayTab3Activity extends ActionBarActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_to_play_tab3);
        getSupportActionBar().setTitle("Comment jouer ?");
    }

    @Override
    public void onBackPressed() {this.getParent().onBackPressed();}

}
