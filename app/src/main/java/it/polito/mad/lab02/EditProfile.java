package it.polito.mad.lab02;

import android.Manifest;
import android.app.Dialog;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.signature.ObjectKey;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.polito.mad.lab02.data.UserProfile;
import it.polito.mad.lab02.utils.GlideApp;
import it.polito.mad.lab02.utils.TextWatcherUtilities;
import it.polito.mad.lab02.utils.Utilities;

public class EditProfile extends AppCompatActivity {

    private static final String ORIGINAL_PROFILE_KEY = "original_profile";
    private static final String CURRENT_PROFILE_KEY = "current_profile";
    private static final String CURRENT_DIALOG_ID_KEY = "current_dialog_id";
    private static final String IMAGE_PATH_TMP = "profile_picture_tmp";

    private static final int CAMERA = 2;
    private static final int GALLERY = 3;
    private static final int PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 4;
    private DialogID dialogId;

    private EditText username, location, biography;
    private UserProfile originalProfile, currentProfile;
    private Dialog dialogInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        username = findViewById(R.id.ep_input_username);
        location = findViewById(R.id.ep_input_location);
        biography = findViewById(R.id.ep_input_biography);

        // Initialize the Profile instances
        if (savedInstanceState != null) {
            // If they was saved, load them
            originalProfile = (UserProfile) savedInstanceState.getSerializable(ORIGINAL_PROFILE_KEY);
            currentProfile = (UserProfile) savedInstanceState.getSerializable(CURRENT_PROFILE_KEY);
            dialogId = (DialogID) savedInstanceState.getSerializable(CURRENT_DIALOG_ID_KEY);
        } else {
            // Otherwise, obtain them through the intent
            originalProfile = (UserProfile) this.getIntent().getSerializableExtra(UserProfile.PROFILE_INFO_KEY);
            currentProfile = new UserProfile(originalProfile);
        }

        // Restore open dialog (if any)
        if (dialogId == null) {
            dialogId = DialogID.DIALOG_NONE;
        }
        this.openDialog(dialogId);

        // Set the toolbar
        final Toolbar toolbar = findViewById(R.id.ep_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Fill the views with the data
        fillViews(currentProfile);

        final FloatingActionButton floatingActionButton = findViewById(R.id.ep_camera_button);
        floatingActionButton.setOnClickListener(v -> this.openDialog(DialogID.DIALOG_CHOOSE_PICTURE));

        username.addTextChangedListener(
                new TextWatcherUtilities.GenericTextWatcher(username, getString(R.string.invalid_username),
                        string -> !Utilities.isNullOrWhitespace(string)));

        location.addTextChangedListener(
                new TextWatcherUtilities.GenericTextWatcherEmptyOrInvalid(location,
                        getString(R.string.invalid_location_empty),
                        getString(R.string.invalid_location_wrong),
                        string -> !Utilities.isNullOrWhitespace(string),
                        string -> Utilities.isValidLocation(string)));

        if (savedInstanceState == null && originalProfile == null) {
            Toast.makeText(this, R.string.complete_profile, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        updateProfileInfo(currentProfile);
        if (!currentProfile.equals(originalProfile)) {
            openDialog(DialogID.DIALOG_CONFIRM_EXIT);
        } else {
            finish();
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        updateProfileInfo(currentProfile);
        outState.putSerializable(ORIGINAL_PROFILE_KEY, originalProfile);
        outState.putSerializable(CURRENT_PROFILE_KEY, currentProfile);

        if (dialogInstance != null && dialogInstance.isShowing()) {
            outState.putSerializable(CURRENT_DIALOG_ID_KEY, dialogId);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAMERA:

                    File imageFileCamera = new File(this.getApplicationContext()
                            .getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_PATH_TMP);

                    if (imageFileCamera.exists()) {
                        currentProfile.setLocalImagePath(imageFileCamera.getAbsolutePath());
                        loadImageProfile(currentProfile);
                    }
                    break;

                case GALLERY:
                    if (data != null && data.getData() != null) {

                        // Move the image to a temporary location
                        File imageFileGallery = new File(this.getApplicationContext()
                                .getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_PATH_TMP);
                        try {
                            Utilities.copyFile(new File(Utilities.getRealPathFromURI(this, data.getData())), imageFileGallery);
                        } catch (IOException e) {
                            this.openDialog(DialogID.DIALOG_ERROR_FAILED_OBTAIN_PICTURE);
                        }

                        currentProfile.setLocalImagePath(imageFileGallery.getAbsolutePath());
                        loadImageProfile(currentProfile);
                    }
                    break;

                default:
                    break;

            }
        }
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

    private void commitChanges() {
        updateProfileInfo(currentProfile);

        if (!currentProfile.isValid()) {
            this.openDialog(DialogID.DIALOG_ERROR_INCORRECT_VALUES);
            return;
        }

        if (originalProfile == null || !originalProfile.equals(currentProfile)) {

            this.openDialog(DialogID.DIALOG_LOADING);
            List<Task<?>> tasks = new ArrayList<>();

            tasks.add(currentProfile.saveToFirebase(this.getResources()));
            if (currentProfile.imageUpdated(originalProfile)) {
                tasks.add(currentProfile.updateImageOnFirebase());
            }

            Tasks.whenAllSuccess(tasks)
                    .addOnSuccessListener(t -> {
                        Intent intent = new Intent(getApplicationContext(), ShowProfile.class);
                        intent.putExtra(UserProfile.PROFILE_INFO_KEY, currentProfile);
                        setResult(RESULT_OK, intent);
                        this.closeDialog();
                        finish();
                    })
                    .addOnFailureListener(t -> {
                        this.closeDialog();
                        this.openDialog(DialogID.DIALOG_ERROR_FAILED_SAVE_DATA);
                    });

        } else {
            setResult(RESULT_CANCELED);
            finish();
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

        loadImageProfile(profile);
    }

    private void updateProfileInfo(UserProfile profile) {
        String usernameStr = username.getText().toString();
        String locationStr = location.getText().toString();
        String biographyStr = biography.getText().toString();

        profile.update(usernameStr, locationStr, biographyStr);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        this.closeDialog();

        if (isFinishing() && currentProfile.getLocalImagePath() != null) {
            File tmpImageFile = new File(currentProfile.getLocalImagePath());
            tmpImageFile.deleteOnExit();
        }
    }

    private void openDialog(@NonNull DialogID dialogId) {
        closeDialog();

        this.dialogId = dialogId;
        switch (dialogId) {
            case DIALOG_CHOOSE_PICTURE:
                dialogInstance = openDialogChoosePicture();
                break;

            case DIALOG_LOADING:
                dialogInstance = ProgressDialog.show(this, null,
                        getString(R.string.saving_data), true);
                break;

            case DIALOG_CONFIRM_EXIT:
                dialogInstance = new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.discard_changes))
                        .setPositiveButton(getString(android.R.string.yes), (dialog, which) -> finish())
                        .setNegativeButton(getString(android.R.string.no), null)
                        .show();
                break;

            case DIALOG_ERROR_INCORRECT_VALUES:
                dialogInstance = Utilities.openErrorDialog(this, R.string.incorrect_values);
                break;

            case DIALOG_ERROR_FAILED_SAVE_DATA:
                dialogInstance = Utilities.openErrorDialog(this, R.string.failed_save_data);
                break;

            case DIALOG_ERROR_FAILED_OBTAIN_PICTURE:
                dialogInstance = Utilities.openErrorDialog(this, R.string.failed_obtain_picture);
                break;
        }
    }

    private void loadImageProfile(UserProfile profile) {
        ImageView imageView = findViewById(R.id.ep_profile_picture);

        GlideApp.with(this)
                .load(profile.getImageReference())
                .centerCrop()
                .placeholder(R.drawable.default_header)
                .signature(new ObjectKey(profile.getProfilePictureLastModified()))
                .into(imageView);
    }

    private Dialog openDialogChoosePicture() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.bottom_sheet_picture_dialog);

        LinearLayout camera = dialog.findViewById(R.id.bs_camera_option);
        LinearLayout gallery = dialog.findViewById(R.id.bs_gallery_option);
        LinearLayout reset = dialog.findViewById(R.id.bs_reset_option);

        assert camera != null;
        assert gallery != null;
        assert reset != null;

        camera.setOnClickListener(v1 -> {
            cameraTakePicture();
            this.closeDialog();
        });

        gallery.setOnClickListener(v2 -> {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_EXTERNAL_STORAGE);
            } else {
                galleryLoadPicture();
            }

            this.closeDialog();
        });

        reset.setOnClickListener(v3 -> {
            currentProfile.resetProfilePicture();
            loadImageProfile(currentProfile);
            this.closeDialog();
        });

        dialog.show();
        return dialog;
    }

    private void closeDialog() {
        if (dialogInstance != null && dialogInstance.isShowing()) {
            dialogInstance.dismiss();
        }
        dialogId = DialogID.DIALOG_NONE;
        dialogInstance = null;
    }

    private enum DialogID {
        DIALOG_NONE,
        DIALOG_CHOOSE_PICTURE,
        DIALOG_LOADING,
        DIALOG_CONFIRM_EXIT,
        DIALOG_ERROR_INCORRECT_VALUES,
        DIALOG_ERROR_FAILED_SAVE_DATA,
        DIALOG_ERROR_FAILED_OBTAIN_PICTURE
    }
}
