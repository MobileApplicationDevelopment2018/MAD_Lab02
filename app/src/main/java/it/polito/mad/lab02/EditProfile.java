package it.polito.mad.lab02;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.polito.mad.lab02.data.UserProfile;
import it.polito.mad.lab02.utils.AppCompatActivityDialog;
import it.polito.mad.lab02.utils.GlideApp;
import it.polito.mad.lab02.utils.GlideRequest;
import it.polito.mad.lab02.utils.PictureUtilities;
import it.polito.mad.lab02.utils.TextWatcherUtilities;
import it.polito.mad.lab02.utils.Utilities;

public class EditProfile extends AppCompatActivityDialog<EditProfile.DialogID> {

    private static final String ORIGINAL_PROFILE_KEY = "original_profile";
    private static final String CURRENT_PROFILE_KEY = "current_profile";
    private static final String IS_COMMITTING_KEY = "isCommitting";
    private static final String IMAGE_PATH_TMP = "profile_picture_tmp";

    private static final int CAMERA = 2;
    private static final int GALLERY = 3;
    private static final int PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 4;

    private EditText username, location, biography;
    private UserProfile originalProfile, currentProfile;

    private boolean isCommitting;
    private AsyncTask<Void, Void, PictureUtilities.CompressedImage> pictureProcessingTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        username = findViewById(R.id.ep_input_username);
        location = findViewById(R.id.ep_input_location);
        biography = findViewById(R.id.ep_input_biography);

        pictureProcessingTask = null;

        // Initialize the Profile instances
        if (savedInstanceState != null) {
            // If they was saved, load them
            originalProfile = (UserProfile) savedInstanceState.getSerializable(ORIGINAL_PROFILE_KEY);
            currentProfile = (UserProfile) savedInstanceState.getSerializable(CURRENT_PROFILE_KEY);
            isCommitting = savedInstanceState.getBoolean(IS_COMMITTING_KEY, false);
        } else {
            // Otherwise, obtain them through the intent
            originalProfile = (UserProfile) this.getIntent().getSerializableExtra(UserProfile.PROFILE_INFO_KEY);
            currentProfile = new UserProfile(originalProfile);
            isCommitting = false;
        }

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
        floatingActionButton.setOnClickListener(v -> this.openDialog(DialogID.DIALOG_CHOOSE_PICTURE, true));

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
            Toast.makeText(this, R.string.complete_profile,
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (this.isCommitting) {
            this.commitChanges();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (this.isCommitting && this.pictureProcessingTask != null) {
            this.pictureProcessingTask.cancel(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing() && currentProfile.getLocalImagePath() != null) {
            File tmpImageFile = new File(currentProfile.getLocalImagePath());
            tmpImageFile.deleteOnExit();
        }
    }

    @Override
    public void onBackPressed() {
        updateProfileInfo(currentProfile);
        if (currentProfile.profileUpdated(originalProfile)) {
            openDialog(DialogID.DIALOG_CONFIRM_EXIT, true);
        } else {
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        updateProfileInfo(currentProfile);
        outState.putSerializable(ORIGINAL_PROFILE_KEY, originalProfile);
        outState.putSerializable(CURRENT_PROFILE_KEY, currentProfile);
        outState.putBoolean(IS_COMMITTING_KEY, isCommitting);
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
                        currentProfile.setProfilePicture(imageFileCamera.getAbsolutePath());
                        loadImageProfile(currentProfile);
                    }
                    break;

                case GALLERY:
                    if (data != null && data.getData() != null) {

                        // Move the image to a temporary location
                        File imageFileGallery = new File(this.getApplicationContext()
                                .getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_PATH_TMP);
                        try {
                            Utilities.copyFile(new File(Utilities.getRealPathFromURI(this,
                                    data.getData())), imageFileGallery);
                        } catch (IOException e) {
                            this.openDialog(DialogID.DIALOG_ERROR_FAILED_OBTAIN_PICTURE, true);
                        }

                        currentProfile.setProfilePicture(imageFileGallery.getAbsolutePath());
                        loadImageProfile(currentProfile);
                    }
                    break;

                default:
                    break;

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
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

    private void galleryLoadPicture() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (galleryIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(galleryIntent, GALLERY);
        }
    }

    private void cameraTakePicture() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {

            File imageFile = new File(this.getApplicationContext()
                    .getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_PATH_TMP);
            Uri imageUri = FileProvider.getUriForFile(this,
                    BuildConfig.APPLICATION_ID.concat(".fileprovider"), imageFile);

            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(cameraIntent, CAMERA);
        }
    }

    private void commitChanges() {
        updateProfileInfo(currentProfile);

        if (!currentProfile.isValid()) {
            this.openDialog(DialogID.DIALOG_ERROR_INCORRECT_VALUES, true);
            return;
        }

        if (originalProfile == null || originalProfile.profileUpdated(currentProfile)) {

            OnSuccessListener<List<?>> onSuccess = t -> {
                isCommitting = false;
                Intent intent = new Intent(getApplicationContext(), ShowProfileFragment.class);
                intent.putExtra(UserProfile.PROFILE_INFO_KEY, currentProfile);
                setResult(RESULT_OK, intent);
                finish();
            };
            OnFailureListener onFailure = t -> {
                isCommitting = false;
                this.openDialog(DialogID.DIALOG_ERROR_FAILED_SAVE_DATA, true);
            };

            this.isCommitting = true;
            this.openDialog(DialogID.DIALOG_SAVING, false);

            // A new profile picture has to be uploaded: it is prepared in background
            // and then it is uploaded to firebase and the other changes are committed
            if (currentProfile.imageUpdated(originalProfile) && currentProfile.hasProfilePicture()) {
                currentProfile.processProfilePictureAsync(picture -> {

                    if (picture == null) {
                        onFailure.onFailure(new Exception());
                        return;
                    }

                    currentProfile.setProfilePictureThumbnail(picture.getThumbnail());

                    List<Task<?>> tasks = new ArrayList<>();
                    tasks.add(currentProfile.saveToFirebase(this.getResources()));
                    tasks.add(currentProfile.uploadProfilePictureToFirebase(picture.getPicture()));
                    Tasks.whenAllSuccess(tasks)
                            .addOnSuccessListener(this, onSuccess)
                            .addOnFailureListener(this, onFailure);

                });
            }

            // Otherwise, there is no need for the asynchronous pre-processing phase
            else {
                List<Task<?>> tasks = new ArrayList<>();
                tasks.add(currentProfile.saveToFirebase(this.getResources()));
                if (currentProfile.imageUpdated(originalProfile) && !currentProfile.hasProfilePicture()) {
                    tasks.add(currentProfile.deleteProfilePictureFromFirebase());
                }
                Tasks.whenAllSuccess(tasks)
                        .addOnSuccessListener(this, onSuccess)
                        .addOnFailureListener(this, onFailure);
            }

        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void fillViews(UserProfile profile) {
        username.setText(profile.getUsername());
        location.setText(profile.getLocation());
        biography.setText(profile.getBiography());

        loadImageProfile(profile);
    }

    private void loadImageProfile(UserProfile profile) {
        ImageView imageView = findViewById(R.id.ep_profile_picture);

        GlideRequest<Drawable> thumbnail = GlideApp
                .with(this)
                .load(currentProfile.getProfilePictureThumbnail())
                .centerCrop();

        GlideApp.with(this)
                .load(profile.getProfilePictureReference())
                .signature(new ObjectKey(profile.getProfilePictureLastModified()))
                .thumbnail(thumbnail)
                .fallback(R.drawable.default_header)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(imageView);
    }

    private void updateProfileInfo(UserProfile profile) {
        String usernameStr = username.getText().toString();
        String locationStr = location.getText().toString();
        String biographyStr = biography.getText().toString();

        profile.update(usernameStr, locationStr, biographyStr);
    }

    @Override
    protected void openDialog(@NonNull DialogID dialogId, boolean dialogPersist) {
        super.openDialog(dialogId, dialogPersist);

        Dialog dialogInstance = null;
        switch (dialogId) {
            case DIALOG_CHOOSE_PICTURE:
                dialogInstance = openDialogChoosePicture();
                break;

            case DIALOG_SAVING:
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

        if (dialogInstance != null) {
            this.setDialogInstance(dialogInstance);
        }
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

    public enum DialogID {
        DIALOG_NONE,
        DIALOG_CHOOSE_PICTURE,
        DIALOG_SAVING,
        DIALOG_CONFIRM_EXIT,
        DIALOG_ERROR_INCORRECT_VALUES,
        DIALOG_ERROR_FAILED_SAVE_DATA,
        DIALOG_ERROR_FAILED_OBTAIN_PICTURE
    }
}
