package it.polito.mad.lab02;

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

    private FirebaseAuth mAuth;

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
        updateNavigationView();

        if (mAuth.getCurrentUser() != null) {
            showSnackBar(getString(R.string.sign_in_welcome_back) + " " + mAuth.getCurrentUser().getDisplayName());
        }
    }

    private void updateNavigationView() {

        NavigationView drawer = findViewById(R.id.nav_view);
        View header = drawer.getHeaderView(0);

        ImageView profilePicture = header.findViewById(R.id.nh_profile_picture);
        TextView username = header.findViewById(R.id.nh_username);
        TextView email = header.findViewById(R.id.nh_email);
        drawer.getMenu().clear();

        if (mAuth.getCurrentUser() != null) {
            //TODO: set the profile picture
            username.setText(mAuth.getCurrentUser().getDisplayName());
            email.setVisibility(View.VISIBLE);
            email.setText(mAuth.getCurrentUser().getEmail());
            drawer.inflateMenu(R.menu.activity_main_drawer_signed_in);
        } else {
            //TODO: reset the profile picture
            username.setText(R.string.anonymous);
            email.setVisibility(View.INVISIBLE);
            email.setText("");
            drawer.inflateMenu(R.menu.activity_main_drawer);
        }
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

            case R.id.nav_sign_in:
                signIn();
                break;

            case R.id.nav_add_book:
                Toast.makeText(this, "Add book clicked", Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_profile:
                Intent toShowProfile = new Intent(getApplicationContext(), ShowProfile.class);
                startActivity(toShowProfile);
                break;

            case R.id.nav_sign_out:
                signOut();
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == RESULT_OK && mAuth.getCurrentUser() != null) {

                int welcomeStringId = R.string.sign_in_welcome;
                if (mAuth.getCurrentUser().getMetadata() != null &&
                        mAuth.getCurrentUser().getMetadata().getCreationTimestamp() == mAuth.getCurrentUser().getMetadata().getLastSignInTimestamp()) {
                    welcomeStringId = R.string.sign_in_welcome;
                }

                showSnackBar(getString(welcomeStringId) + " " + mAuth.getCurrentUser().getDisplayName());
                updateNavigationView();
                return;
            }

            if (response == null) {
                showSnackBar(R.string.sign_in_cancelled);
                return;
            }

            if (response.getError() != null && response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                showSnackBar(R.string.sign_in_no_internet_connection);
                return;
            }

            showSnackBar(R.string.sign_in_unknown_error);
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
                    showSnackBar(R.string.sign_out_succeeded);
                    updateNavigationView();
                })
                .addOnFailureListener(t -> {
                    showSnackBar(R.string.sign_out_failed);
                });
    }

    private void showSnackBar(@StringRes int messageId) {
        showSnackBar(getString(messageId));
    }

    private void showSnackBar(@NonNull String message) {
        Snackbar.make(findViewById(R.id.main_coordinator_layout), message, Snackbar.LENGTH_LONG)
                .show();
    }
}

