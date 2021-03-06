package com.mikerinehart.rideguide.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.mikerinehart.rideguide.R;

import com.mikerinehart.rideguide.models.User;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;

import de.psdev.licensesdialog.licenses.MITLicense;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;

public class SettingActivity extends ActionBarActivity {

    public static Toolbar settings_toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setTintColor(getResources().getColor(R.color.ColorPrimary));

        setContentView(R.layout.activity_setting);
        settings_toolbar = (android.support.v7.widget.Toolbar)findViewById(R.id.tool_bar);


        setSupportActionBar(settings_toolbar);
        settings_toolbar.setTitle("Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        settings_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        new Handler().post(new Runnable() {
            @Override public void run() {
                getFragmentManager().beginTransaction()
                        .replace(R.id.settings_container, new SettingsFragment2())
                        .commit();
            }
        });
        System.gc(); // Clear RAM

    }

    @Override
    public void onBackPressed() {
        Log.i("OnBackPressed", "Back pressed");

        if (!MainActivity.isMainActivityOpen()) {
            SharedPreferences userPref = this.getSharedPreferences("CURRENT_USER", Context.MODE_PRIVATE);
            User me;
            Gson gson = new Gson();
            String jsonMe = userPref.getString("CURRENT_USER", "not_found");
            if (jsonMe.equalsIgnoreCase("not_found")) {
            } else {
                me = gson.fromJson(jsonMe, User.class);
                Intent mainActivityIntent = new Intent(getApplicationContext(), MainActivity.class);
                mainActivityIntent.putExtra("me", me);
                startActivity(mainActivityIntent);
            }
        }
        super.finish();
    }

    public static class SettingsFragment2 extends PreferenceFragment {

        SharedPreferences sp;
        SharedPreferences notifications;
        SharedPreferences.Editor editor;
        SharedPreferences.Editor notificationsEditor;
        Context c;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs);
            sp = getActivity().getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
            notifications = getActivity().getSharedPreferences(Constants.NOTIFICATIONS, Context.MODE_PRIVATE);
            editor = sp.edit();
            notificationsEditor = notifications.edit();

            c = getActivity();

            com.jenzz.materialpreference.Preference resetTutorialScreens = (com.jenzz.materialpreference.Preference)findPreference("resetTutorialScreens");
            com.jenzz.materialpreference.Preference clearNotifications = (com.jenzz.materialpreference.Preference)findPreference("clearNotifications");

            com.jenzz.materialpreference.Preference ossLicense = (com.jenzz.materialpreference.Preference)findPreference("ossLicense");
            com.jenzz.materialpreference.Preference aboutApp = (com.jenzz.materialpreference.Preference)findPreference("aboutApp");

            resetTutorialScreens.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    editor.remove(Constants.SHOWDRAWERSHOWCASE);
                    editor.remove(Constants.SHOWDRAWERHANDLESHOWCASE);
                    MainActivity.showDrawerShowcase = true;
                    MainActivity.showDrawerHandleShowcase = true;
                    editor.remove(Constants.SHOWPROFILESHOWCASE);
                    editor.remove(Constants.SHOWSHIFTSSHOWCASE);
                    editor.remove(Constants.SHOWRIDESSHOWCASE);
                    editor.remove(Constants.SHOWRESERVATIONSSHOWCASE);

                    editor.commit();
                    Toast.makeText(getActivity().getBaseContext(), "Tutorial screens reset!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });

            clearNotifications.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    notificationsEditor.clear();
                    notificationsEditor.commit();
                    Toast.makeText(getActivity().getBaseContext(), "Notifications cleared!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });

            ossLicense.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final Notices notices = new Notices();

                    notices.addNotice(new Notice("Android Async HTTP", "http://loopj.com/android-async-http/", "James Smith", new ApacheSoftwareLicense20()));
                    notices.addNotice(new Notice("Android-MaterialPreference", "https://github.com/jenzz/Android-MaterialPreference", "Jens Driller", new MITLicense()));
                    notices.addNotice(new Notice("Gson", "https://code.google.com/p/google-gson/", null, new ApacheSoftwareLicense20()));
                    notices.addNotice(new Notice("MaterialEditText", "https://github.com/rengwuxian/MaterialEditText", "rengwuxian", new ApacheSoftwareLicense20()));
                    notices.addNotice(new Notice("Material Range Bar", "https://github.com/oli107/material-range-bar", "AppyVet, Inc.", new ApacheSoftwareLicense20()));
                    notices.addNotice(new Notice("Material Dialogs", "https://github.com/afollestad/material-dialogs", "Aiden Follestad", new MITLicense()));
                    notices.addNotice(new Notice("Material Design Library", "https://github.com/navasmdc/MaterialDesignLibrary", null, new ApacheSoftwareLicense20()));
                    notices.addNotice(new Notice("PagerSlidingTabStrip", "https://github.com/jpardogo/PagerSlidingTabStrip", "Andreas Stuetz", new ApacheSoftwareLicense20()));
                    notices.addNotice(new Notice("Parceler", "https://github.com/johncarl81/parceler", "John Ericksen", new ApacheSoftwareLicense20()));
                    notices.addNotice(new Notice("Picasso", "http://square.github.io/picasso/", "Square, Inc.", new ApacheSoftwareLicense20()));
                    notices.addNotice(new Notice("ShowcaseView", "https://github.com/amlcurran/ShowcaseView", "Alex Curran", new ApacheSoftwareLicense20()));
                    notices.addNotice(new Notice("SlideDateTimePicker", "https://github.com/jjobes/SlideDateTimePicker", null, new ApacheSoftwareLicense20()));
                    notices.addNotice(new Notice("Snackbar", "https://github.com/nispok/snackbar", null, new MITLicense()));
                    notices.addNotice(new Notice("Sticky Headers Recyclerview", "https://github.com/timehop/sticky-headers-recyclerview", "Timehop", new ApacheSoftwareLicense20()));
                    notices.addNotice(new Notice("SystemBarTint", "https://github.com/jgilfelt/SystemBarTint", "Jeff Gilfelt", new ApacheSoftwareLicense20()));

                    new LicensesDialog.Builder(c).setNotices(notices).setIncludeOwnLicense(true).build().show();


                    return false;
                }
            });

            aboutApp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final String license = "RideGuide is an Android application created by Mike Rinehart for the" +
                                            " 2015 Mobile Application Development (MAD) Contest, sponsored by State Farm. RideGuide " +
                            " is an application designed to help students find designated drivers on their campus.\n\n" +
                            "By using this application you assume all liabilities associated with the use/misuse of this application.\n" +
                            "Payment for a non-licensed taxi service (designated driving) is illegal. Under no circumstances are you (as a passenger) obligated to pay" +
                            " for services provided by either this app or your driver.\n\nUse of Venmo\u00a9 services included within this application may not be used for" +
                            " selling a service/good. Use of Venmo\u00a9 services may be used strictly for donation purposes only.";

                    new MaterialDialog.Builder(c)
                            .title("RideGuide")
                            .titleGravity(GravityEnum.CENTER)
                            .content(license)
                            .positiveText("OK")
                            .build().show();

                    return false;
                }
            });
        }
    }
}
