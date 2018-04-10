package it.polito.mad.lab02;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int RC_SIGN_IN = 1;
    private static final int RC_COMPLETE_REGISTRATION = 2;
    private static final int RC_SHOW_PROFILE = 3;

    private FirebaseAuth mAuth;
    private UserProfile localProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mAuth = FirebaseAuth.getInstance();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Profile already cached locally
        if (savedInstanceState != null) {
            localProfile = (UserProfile) savedInstanceState.getSerializable(UserProfile.PROFILE_INFO_KEY);
            if (localProfile != null) {
                updateNavigationView();
                return;
            }
        }

        // Profile to be obtained from database
        localProfile = new UserProfile();
        if (mAuth.getCurrentUser() != null) {
            loadProfileFromFirebase();
        } else {
            updateNavigationView();
        }
    }

    private void updateNavigationView() {

        NavigationView drawer = findViewById(R.id.nav_view);
        View header = drawer.getHeaderView(0);

        ImageView profilePicture = header.findViewById(R.id.nh_profile_picture);
        TextView username = header.findViewById(R.id.nh_username);
        TextView email = header.findViewById(R.id.nh_email);
        drawer.getMenu().clear();

        if (!localProfile.isAnonymous()) {
            //TODO: set the profile picture
            username.setText(localProfile.getUsername());
            email.setVisibility(View.VISIBLE);
            email.setText(localProfile.getEmail());
            drawer.inflateMenu(R.menu.activity_main_drawer_signed_in);
        } else {
            //TODO: reset the profile picture
            username.setText(R.string.anonymous);
            email.setVisibility(View.INVISIBLE);
            email.setText("");
            drawer.inflateMenu(R.menu.activity_main_drawer);
        }

        drawer.getMenu().getItem(0).setChecked(true);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {

            case R.id.nav_explore:
                Toast.makeText(this, "Explore clicked", Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_sign_in:
                signIn();
                break;

            case R.id.nav_add_book:
                Toast.makeText(this, "Add book clicked", Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_profile:
                Intent toShowProfile = new Intent(getApplicationContext(), ShowProfile.class);
                toShowProfile.putExtra(UserProfile.PROFILE_INFO_KEY, localProfile);
                startActivityForResult(toShowProfile, RC_SHOW_PROFILE);
                break;

            case R.id.nav_sign_out:
                signOut();
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(UserProfile.PROFILE_INFO_KEY, localProfile);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RC_SIGN_IN:
                IdpResponse response = IdpResponse.fromResultIntent(data);

                // Successfully signed in
                if (resultCode == RESULT_OK && mAuth.getCurrentUser() != null) {
                    loadProfileFromFirebase();
                    return;
                }

                if (response == null) {
                    showToast(R.string.sign_in_cancelled);
                    return;
                }

                if (response.getError() != null && response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    showToast(R.string.sign_in_no_internet_connection);
                    return;
                }

                showToast(R.string.sign_in_unknown_error);
                break;

            case RC_COMPLETE_REGISTRATION:
                if (resultCode == RESULT_OK) {
                    localProfile = (UserProfile) data.getSerializableExtra(UserProfile.PROFILE_INFO_KEY);
                    updateNavigationView();
                }

            case RC_SHOW_PROFILE:
                if (resultCode == RESULT_OK) {
                    localProfile = (UserProfile) data.getSerializableExtra(UserProfile.PROFILE_INFO_KEY);
                    updateNavigationView();
                }

                break;

            default:
                break;
        }
    }

    private void signIn() {
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setAvailableProviders(Arrays.asList(
                                new AuthUI.IdpConfig.EmailBuilder().build(),
                                new AuthUI.IdpConfig.GoogleBuilder().build()))
                        .build(),
                RC_SIGN_IN);
    }

    private void signOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnSuccessListener(t -> {
                    onSignOut();
                })
                .addOnFailureListener(t -> {
                    showToast(R.string.sign_out_failed);
                });
    }

    private void onSignOut() {
        showToast(R.string.sign_out_succeeded);
        localProfile = new UserProfile();
        updateNavigationView();
    }

    private void showToast(@StringRes int message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showToast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void loadProfileFromFirebase() {
        ProgressDialog dialog = ProgressDialog.show(this, "",
                getString(R.string.fui_progress_dialog_loading), true);
        UserProfile.loadFromFirebase(data -> {
            dialog.cancel();

            if (data == null) {
                completeRegistration();
            } else {
                localProfile = new UserProfile(data, this.getResources());
                updateNavigationView();
                showToast(getString(R.string.sign_in_welcome_back) + " " + localProfile.getUsername());
            }

        }, error -> {
            dialog.cancel();
            Utilities.showErrorMessage(this, R.string.failed_load_data);
            signOut();
        });
    }

    private void completeRegistration() {

        assert mAuth.getCurrentUser() != null;
        localProfile = new UserProfile(mAuth.getCurrentUser(), this.getResources());
        localProfile.saveToFirebase(this.getResources());

        String message = getString(R.string.sign_in_welcome) + " " + localProfile.getUsername();
        Snackbar.make(findViewById(R.id.main_coordinator_layout), message, Snackbar.LENGTH_LONG)
                .setAction(R.string.edit_profile, v -> {
                    Intent toEditProfile = new Intent(getApplicationContext(), EditProfile.class);
                    toEditProfile.putExtra(UserProfile.PROFILE_INFO_KEY, localProfile);
                    startActivityForResult(toEditProfile, RC_SHOW_PROFILE);
                }).show();

        updateNavigationView();
    }
}