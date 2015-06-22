package com.example.arnaud.lasercar;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

public class HowToPlayActivity extends TabActivity
{
    private TabHost tabHost;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_to_play);

        this.tabHost = getTabHost();

        /* ================================================================================ */
        /* ========================= INSTANCIATION DES ONGLETS ============================ */
        /* ================================================================================ */
        setupTab("But du jeu", "tab1", new Intent().setClass(this, HowToPlayTab1Activity.class));
        setupTab("Règles", "tab2", new Intent().setClass(this, HowToPlayTab2Activity.class));
        setupTab("Commande", "tab3", new Intent().setClass(this, HowToPlayTab3Activity.class));
    }

    private void setupTab(String name, String tag, Intent intent)
    {
        tabHost.addTab(tabHost.newTabSpec(tag).setIndicator(createTabView(tabHost.getContext(), name)).setContent(intent));
    }

    private static View createTabView(final Context context, final String text)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.tab_item, null);
        TextView tv = (TextView) view.findViewById(R.id.tabsText);
        tv.setText(text);

        return view;
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
