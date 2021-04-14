package com.partyfm.radio.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.mediarouter.app.MediaRouteActionProvider;
import androidx.mediarouter.app.MediaRouteDialogFactory;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.material.navigation.NavigationView;
import com.partyfm.radio.Config;
import com.partyfm.radio.R;
import com.partyfm.radio.fragments.FragmentHome;
import com.partyfm.radio.fragments.FragmentRadio;
import com.partyfm.radio.services.PlayerService;
import com.partyfm.radio.utilities.Station;

import java.util.ArrayList;

import static com.partyfm.radio.Config.RADIO_STREAM_URL_HIGH;
import static com.partyfm.radio.Config.RADIO_STREAM_URL_LOW;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private final static String COLLAPSING_TOOLBAR_FRAGMENT_TAG = "collapsing_toolbar";
    private final static String SELECTED_TAG = "selected_index";
    private final static int TOOLBAR = 0;

    private static int selectedIndex;
    public static int mStationIndex = 0;
    private DrawerLayout drawerLayout;

    public static ArrayList<Station> mStationList = new ArrayList<>();
    public static Station mCurrentStation = null;
    public static String urlToPlay = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initStationList();

        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        drawerLayout = findViewById(R.id.drawer_layout);
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            Animation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setInterpolator(new DecelerateInterpolator());

            drawerLayout.setVisibility(View.VISIBLE);
            drawerLayout.setAnimation(fadeIn);
        }, 3000);

        if (savedInstanceState != null) {
            navigationView.getMenu().getItem(savedInstanceState.getInt(SELECTED_TAG)).setChecked(true);
            return;
        }

        selectedIndex = TOOLBAR;

        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,
                new FragmentHome(), COLLAPSING_TOOLBAR_FRAGMENT_TAG).commit();
    }

    private void initStationList(){
        mStationList.clear();
        for(int i=0; i<2; i++){
            String stationUrl = RADIO_STREAM_URL_LOW;
            String stationName = "Lav kvalitet";
            if(i == 1){
                stationUrl = RADIO_STREAM_URL_HIGH;
                stationName = "Høj kvalitet";
            }

            Station mStation = new Station(stationName, stationUrl, "", "");
            mStationList.add(mStation);
        }
        mStationIndex = 0;
        urlToPlay = Config.RADIO_STREAM_URL_LOW;
        mCurrentStation = mStationList.get(0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_VOLUME_DOWN == event.getKeyCode() || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {

            Intent playIntent = new Intent(this, PlayerService.class);
            playIntent.setAction(Config.MEDIA_ACTION_UNMUTE);
            startService(playIntent);

            FragmentRadio.isMuteed = false;
            FragmentRadio.buttonMute.setImageResource(R.drawable.ic_sound);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_TAG, selectedIndex);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(
                        mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(FragmentRadio.mediaSelector);
        mediaRouteActionProvider.setDialogFactory(new MediaRouteDialogFactory());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_share) {
            Intent sendInt = new Intent(Intent.ACTION_SEND);
            sendInt.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            sendInt.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text) + "\nhttps://play.google.com/store/apps/details?id=" + getPackageName());
            sendInt.setType("text/plain");
            startActivity(Intent.createChooser(sendInt, "Share"));
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.drawer_home:
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;

            case R.id.drawer_rate:
                final String appName = getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appName)));
                }
                return true;

            case R.id.drawer_about:
                Intent about = new Intent(this, ActivityAbout.class);
                startActivity(about);

                return true;

            case R.id.drawer_exit:
                exitDialog();

                return true;
        }

        return false;
    }

    public void setupNavigationDrawer(Toolbar toolbar) {
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            exitDialog();
        }
    }

    public void exitDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setIcon(R.drawable.ic_icon);
        dialog.setTitle(R.string.app_name);
        dialog.setMessage(getResources().getString(R.string.message));
        dialog.setPositiveButton(getResources().getString(R.string.quit), (dialogInterface, i) -> {

            Intent intentDisMissNotification = new Intent(MainActivity.this, PlayerService.class);
            intentDisMissNotification.setAction(Config.MEDIA_NOTIFI_EXIT);
            startService(intentDisMissNotification);

            MainActivity.this.finish();

        });

        dialog.setNegativeButton(getResources().getString(R.string.minimize), (dialogInterface, i) -> minimizeApp());

        dialog.setNeutralButton(getResources().getString(R.string.cancel), (dialogInterface, i) -> {

        });
        dialog.show();
    }

    public void minimizeApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        Log.d("", "onResume() was called");
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        Intent intentDisMissNotification = new Intent(MainActivity.this, PlayerService.class);
        intentDisMissNotification.setAction(Config.MEDIA_NOTIFI_EXIT);
        startService(intentDisMissNotification);

        MainActivity.this.finish();

        super.onDestroy();
    }

}
