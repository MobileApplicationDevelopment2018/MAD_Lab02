package it.polito.mad.lab02;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
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

import it.polito.mad.lab02.data.UserProfile;
import it.polito.mad.lab02.utils.AppCompatActivityDialog;
import it.polito.mad.lab02.utils.Utilities;

public class MainActivity extends AppCompatActivityDialog<MainActivity.DialogID>
        implements NavigationView.OnNavigationItemSelectedListener,
        AddBookFragment.OnFragmentInteractionListener {

    private static final int RC_SIGN_IN = 1;
    private static final int RC_EDIT_PROFILE = 5;
    private static final int RC_EDIT_PROFILE_WELCOME = 6;

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
        }

        // Profile to be obtained from database
        if (localProfile == null) {
            localProfile = new UserProfile();
            if (mAuth.getCurrentUser() != null) {
                loadProfileFromFirebase();
            }
        }

        updateNavigationView();
        if (savedInstanceState == null) {
            showDefaultFragment();
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(UserProfile.PROFILE_INFO_KEY, localProfile);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.

        int id = item.getItemId();

        switch (id) {
            case R.id.nav_explore:
                this.replaceFragment(ExploreFragment.newInstance());
                break;

            case R.id.nav_sign_in:
                signIn();
                break;

            case R.id.nav_add_book:
                this.replaceFragment(AddBookFragment.newInstance());
                break;

            case R.id.nav_profile:
                this.replaceFragment(ShowProfileFragment.newInstance(localProfile));
                break;

            case R.id.nav_sign_out:
                signOut();
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sp_edit_profile:
                this.showEditProfileActivity(RC_EDIT_PROFILE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

                showDefaultFragment();

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

            case RC_EDIT_PROFILE:
                if (resultCode == RESULT_OK) {
                    localProfile = (UserProfile) data.getSerializableExtra(UserProfile.PROFILE_INFO_KEY);
                    this.replaceFragment(ShowProfileFragment.newInstance(localProfile));
                }
                break;

            case RC_EDIT_PROFILE_WELCOME:
                if (resultCode == RESULT_OK) {
                    localProfile = (UserProfile) data.getSerializableExtra(UserProfile.PROFILE_INFO_KEY);
                }
                break;

            default:
                break;
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
                .addOnSuccessListener(t -> onSignOut())
                .addOnFailureListener(t -> showToast(R.string.sign_out_failed));
    }

    private void onSignOut() {
        showToast(R.string.sign_out_succeeded);
        localProfile = new UserProfile();
        updateNavigationView();
        showDefaultFragment();
    }

    private void showToast(@StringRes int message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showToast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void loadProfileFromFirebase() {
        this.openDialog(DialogID.DIALOG_LOADING);

        UserProfile.loadFromFirebase(data -> {
            this.closeDialog();

            if (data == null) {
                completeRegistration();
            } else {
                localProfile = new UserProfile(data, this.getResources());
                updateNavigationView();
                showToast(getString(R.string.sign_in_welcome_back) + " " + localProfile.getUsername());
            }

        }, error -> {
            this.openDialog(DialogID.DIALOG_ERROR_RETRIEVE_DIALOG);
            signOut();
        });
    }

    private void completeRegistration() {

        assert mAuth.getCurrentUser() != null;
        localProfile = new UserProfile(mAuth.getCurrentUser(), this.getResources());
        localProfile.saveToFirebase(this.getResources());

        String message = getString(R.string.sign_in_welcome) + " " + localProfile.getUsername();
        Snackbar.make(findViewById(R.id.main_coordinator_layout), message, Snackbar.LENGTH_LONG)
                .setAction(R.string.edit_profile, v -> showEditProfileActivity(RC_EDIT_PROFILE_WELCOME))
                .show();

        updateNavigationView();
    }

    private void replaceFragment(Fragment instance) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, instance)
                .commit();
    }

    private void showDefaultFragment() {
        NavigationView drawer = findViewById(R.id.nav_view);
        drawer.getMenu().getItem(0).setChecked(true);
        replaceFragment(ExploreFragment.newInstance());
    }

    private void showEditProfileActivity(int code) {
        Intent toEditProfile = new Intent(getApplicationContext(), EditProfile.class);
        toEditProfile.putExtra(UserProfile.PROFILE_INFO_KEY, localProfile);
        startActivityForResult(toEditProfile, code);
    }

    @Override
    protected void openDialog(@NonNull DialogID dialogId) {
        super.openDialog(dialogId);

        Dialog dialog = null;
        switch (dialogId) {
            case DIALOG_LOADING:
                dialog = ProgressDialog.show(this, "",
                        getString(R.string.fui_progress_dialog_loading), true);
                break;
            case DIALOG_ERROR_RETRIEVE_DIALOG:
                dialog = Utilities.openErrorDialog(this, R.string.failed_load_data);
        }

        if (dialog != null) {
            setDialogInstance(dialog);
        }
    }

    @Override
    public void onFragmentInteraction() {

    }

    public enum DialogID {
        DIALOG_LOADING,
        DIALOG_ERROR_RETRIEVE_DIALOG;
    }
}