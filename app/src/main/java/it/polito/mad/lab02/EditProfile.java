package it.polito.mad.lab02;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class EditProfile extends AppCompatActivity {

    private static final String ORIGINAL_PROFILE_KEY = "original_profile";
    private static final String CURRENT_PROFILE_KEY = "current_profile";
    private static final String IMAGE_CHANGED_KEY = "image_changed";
    private static final String IMAGE_PATH = "profile_pic";
    private static final String IMAGE_PATH_TMP = "profile_pic_tmp";

    private static final int CAMERA = 2;
    private static final int GALLERY = 3;
    private static final int PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 4;

    private EditText username, location, biography;
    private ImageView imageView;
    private BottomSheetDialog bottomSheetDialog;
    private boolean imageChanged;
    private UserProfile originalProfile, currentProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        username = findViewById(R.id.ep_input_username);
        location = findViewById(R.id.ep_input_location);
        biography = findViewById(R.id.ep_input_biography);
        imageView = findViewById(R.id.ep_profile_picture);

        // Initialize the Profile instances
        if (savedInstanceState != null) {
            // If they was saved, load them
            originalProfile = (UserProfile) savedInstanceState.getSerializable(ORIGINAL_PROFILE_KEY);
            currentProfile = (UserProfile) savedInstanceState.getSerializable(CURRENT_PROFILE_KEY);
            imageChanged = savedInstanceState.getBoolean(IMAGE_CHANGED_KEY, false);
        } else {
            // Otherwise, obtain them through the intent
            originalProfile = (UserProfile) this.getIntent().getSerializableExtra(UserProfile.PROFILE_INFO_KEY);
            currentProfile = new UserProfile(originalProfile);
            imageChanged = false;
        }

        // Set the toolbar
        final Toolbar toolbar = findViewById(R.id.ep_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Fill the views with the data
        fillViews(currentProfile);

        bottomSheetDialog = new BottomSheetDialog(this);
        final View sheetView = this.getLayoutInflater().inflate(R.layout.bottom_sheet_picture_dialog, null);
        bottomSheetDialog.setContentView(sheetView);

        final FloatingActionButton floatingActionButton = findViewById(R.id.ep_camera_button);
        floatingActionButton.setOnClickListener(v -> {
            LinearLayout camera = bottomSheetDialog.findViewById(R.id.bs_camera_option);
            LinearLayout gallery = bottomSheetDialog.findViewById(R.id.bs_gallery_option);
            LinearLayout reset = bottomSheetDialog.findViewById(R.id.bs_reset_option);

            camera.setOnClickListener(v1 -> {
                cameraTakePicture();
                bottomSheetDialog.dismiss();
            });

            gallery.setOnClickListener(v2 -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_EXTERNAL_STORAGE);
                } else {
                    galleryLoadPicture();
                }

                bottomSheetDialog.dismiss();
            });

            reset.setOnClickListener(v3 -> {
                this.imageChanged = false;
                currentProfile.update(null);
                imageView.setImageBitmap(currentProfile.getImageBitmapOrDefault(this.getResources(), imageView.getWidth(), imageView.getHeight()));
                bottomSheetDialog.dismiss();
            });

            bottomSheetDialog.show();
        });

        username.addTextChangedListener(
                new Utilities.GenericTextWatcher(username, getString(R.string.invalid_username),
                        string -> !Utilities.isNullOrWhitespace(string)));

        location.addTextChangedListener(
                new Utilities.GenericTextWatcherEmptyOrInvalid(location,
                        getString(R.string.invalid_location_empty),
                        getString(R.string.invalid_location_wrong),
                        string -> !Utilities.isNullOrWhitespace(string),
                        string -> Utilities.isValidLocation(string)));

        if (savedInstanceState == null && originalProfile == null) {
            Toast.makeText(this, R.string.complete_profile, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (bottomSheetDialog.isShowing()) {
            bottomSheetDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_edit_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.ep_save_profile:
                commitChanges();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {

        updateProfileInfo(currentProfile);
        if (!currentProfile.equals(originalProfile) || this.imageChanged) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.discard_changes))
                    .setPositiveButton(getString(android.R.string.yes), (dialog, which) -> cleanupAndFinish())
                    .setNegativeButton(getString(android.R.string.no), null)
                    .show();
        } else {
            cleanupAndFinish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        updateProfileInfo(currentProfile);
        outState.putSerializable(ORIGINAL_PROFILE_KEY, originalProfile);
        outState.putSerializable(CURRENT_PROFILE_KEY, currentProfile);
        outState.putBoolean(IMAGE_CHANGED_KEY, imageChanged);
    }

    private void galleryLoadPicture() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (galleryIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(galleryIntent, GALLERY);
        }
    }

    private void cameraTakePicture() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {

            File imageFile = new File(this.getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_PATH_TMP);
            Uri imageUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID.concat(".fileprovider"), imageFile);

            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(cameraIntent, CAMERA);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        ImageView imageView = findViewById(R.id.ep_profile_picture);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAMERA:

                    File imageFileCamera = new File(this.getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_PATH_TMP);

                    if (imageFileCamera.exists()) {
                        currentProfile.update(imageFileCamera.getAbsolutePath());
                        imageView.setImageBitmap(currentProfile.getImageBitmapOrDefault(this.getResources(), imageView.getWidth(), imageView.getHeight()));
                        this.imageChanged = true;
                    }
                    break;

                case GALLERY:
                    if (data != null && data.getData() != null) {

                        // Move the image to a temporary location
                        File imageFileGallery = new File(this.getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_PATH_TMP);
                        try {
                            Utilities.copyFile(new File(Utilities.getRealPathFromURI(this, data.getData())), imageFileGallery);
                        } catch (IOException e) {
                            Utilities.showErrorMessage(this, R.string.failed_obtain_picture);
                        }

                        currentProfile.update(imageFileGallery.getAbsolutePath());
                        imageView.setImageBitmap(currentProfile.getImageBitmapOrDefault(this.getResources(), imageView.getWidth(), imageView.getHeight()));

                        this.imageChanged = true;
                    }
                    break;

                default:
                    break;

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {

            case PERMISSIONS_REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    galleryLoadPicture();
                }

                return;
            }

            default:
                break;
        }
    }

    private void fillViews(UserProfile profile) {
        username.setText(profile.getUsername());
        location.setText(profile.getLocation());
        biography.setText(profile.getBiography());

        imageView.post(() ->
                imageView.setImageBitmap(profile.getImageBitmapOrDefault(this.getResources(), imageView.getWidth(), imageView.getHeight()))
        );
    }

    private void updateProfileInfo(UserProfile profile) {
        String usernameStr = username.getText().toString();
        String locationStr = location.getText().toString();
        String biographyStr = biography.getText().toString();

        profile.update(usernameStr, locationStr, biographyStr);
    }

    private void commitChanges() {
        updateProfileInfo(currentProfile);

        if (!currentProfile.isValid()) {
            Utilities.showErrorMessage(this, R.string.incorrect_values);
            return;
        }

        // Save the image permanently
        if (this.imageChanged && currentProfile.getImagePath() != null) {
            File sourceFile = new File(currentProfile.getImagePath());
            File destinationFile = new File(this.getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_PATH);
            if (!sourceFile.renameTo(destinationFile)) {
                Utilities.showErrorMessage(this, R.string.failed_obtain_picture);
                return;
            }
            currentProfile.update(destinationFile.getAbsolutePath());
        }

        if (originalProfile == null || !originalProfile.equals(currentProfile) || this.imageChanged) {

            ProgressDialog dialog = ProgressDialog.show(this, "",
                    "Saving your personal data. Please wait...", true);

            currentProfile.saveToFirebase(this.getResources(), (t) -> {
                Intent intent = new Intent(getApplicationContext(), ShowProfile.class);
                intent.putExtra(UserProfile.PROFILE_INFO_KEY, currentProfile);
                setResult(RESULT_OK, intent);
                dialog.cancel();
                finish();
            }, (t) -> {
                dialog.cancel();
                Utilities.showErrorMessage(this, R.string.failed_save_data);
            });

        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void cleanupAndFinish() {
        File tmpImageFile = new File(this.getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_PATH_TMP);
        if (tmpImageFile.exists()) {
            tmpImageFile.deleteOnExit();
        }
        finish();
    }
}
