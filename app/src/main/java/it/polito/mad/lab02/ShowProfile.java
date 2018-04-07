package it.polito.mad.lab02;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import java.util.Locale;

public class ShowProfile extends AppCompatActivity {

    private static final String USER_PROFILE_KEY = "user_profile";
    private static final String PROFILE_ID = "001";
    private static final int EDIT_PROFILE = 1;
    private UserProfile profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_profile);

        // Initialize the LocalUserProfile
        if (savedInstanceState != null) {
            // If the profile was saved, load it
            profile = (UserProfile) savedInstanceState.getSerializable(USER_PROFILE_KEY);
        }

        if (profile == null) {
            // Otherwise, since now no backend server exists, it is loaded either from shared preferences or
            // using default values; read-only data are set to default values anyway.
            profile = new UserProfile(this, PROFILE_ID, this.getPreferences(Context.MODE_PRIVATE));
        }

        // Fill the views with the data
        fillViews(profile);

        // Set the toolbar
        final Toolbar toolbar = findViewById(R.id.sp_toolbar);
        setSupportActionBar(toolbar);

        // MailTo button
        final ImageButton mailToButton = findViewById(R.id.sp_mail_icon);
        mailToButton.setOnClickListener(v -> {
            Uri uri = Uri.parse("mailto:" + profile.getEmail());
            Intent mailTo = new Intent(Intent.ACTION_SENDTO, uri);
            if (mailTo.resolveActivity(getPackageManager()) != null) {
                startActivity(mailTo);
            }
        });

        // ShowCity button
        final ImageButton showCityButton = findViewById(R.id.sp_locate_icon);
        showCityButton.setOnClickListener(v -> {
            Uri uri = Uri.parse("http://maps.google.co.in/maps?q=" + profile.getLocation());
            Intent showCity = new Intent(Intent.ACTION_VIEW, uri);
            if (showCity.resolveActivity(getPackageManager()) != null) {
                startActivity(showCity);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_show_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sp_edit_profile:
                Intent toEdit = new Intent(getApplicationContext(), EditProfile.class);
                toEdit.putExtra(UserProfile.PROFILE_INFO_KEY, profile);
                startActivityForResult(toEdit, EDIT_PROFILE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case EDIT_PROFILE:
                if (resultCode == RESULT_OK) {

                    profile = (UserProfile) data.getSerializableExtra(UserProfile.PROFILE_INFO_KEY);
                    // Update the views
                    fillViews(profile);

                    SharedPreferences.Editor editor = this.getPreferences(Context.MODE_PRIVATE).edit();
                    profile.save(PROFILE_ID, editor);
                    editor.apply();
                }

                break;

            default:
                break;

        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the values
        outState.putSerializable(USER_PROFILE_KEY, profile);
    }

    private void fillViews(UserProfile profile) {
        TextView username = findViewById(R.id.sp_username);
        TextView location = findViewById(R.id.sp_location);
        TextView biography = findViewById(R.id.sp_description);
        ImageView image = findViewById(R.id.sp_profile_picture);

        RatingBar rating = findViewById(R.id.sp_rating_bar);
        TextView lentBooks = findViewById(R.id.sp_lent_books_number);
        TextView borrowedBooks = findViewById(R.id.sp_borrowed_books_number);
        TextView toBeReturnedBooks = findViewById(R.id.sp_to_be_returned_number);

        username.setText(profile.getUsername());
        location.setText(profile.getLocation());
        biography.setText(profile.getBiography());

        image.post(() ->
                image.setImageBitmap(profile.getImageBitmapOrDefault(this, image.getWidth(), image.getHeight()))
        );

        rating.setRating(profile.getRating());

        Locale currentLocale = getResources().getConfiguration().locale;
        lentBooks.setText(String.format(currentLocale, "%d", profile.getLentBooks()));
        borrowedBooks.setText(String.format(currentLocale, "%d", profile.getBorrowedBooks()));
        toBeReturnedBooks.setText(String.format(currentLocale, "%d", profile.getToBeReturnedBooks()));
    }
}
