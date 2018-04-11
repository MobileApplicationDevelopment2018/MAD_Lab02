package it.polito.mad.lab02.data;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.Patterns;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

import it.polito.mad.lab02.R;
import it.polito.mad.lab02.utils.PictureUtilities;
import it.polito.mad.lab02.utils.Utilities;

public class UserProfile implements Serializable {

    public static final String PROFILE_INFO_KEY = "profile_info_key";

    private static final String FIREBASE_USERS_KEY = "users";
    private static final String FIREBASE_DATA_KEY = "data";
    private static final String FIREBASE_PROFILE_KEY = "profile";

    private static final String FIREBASE_STORAGE_USERS_FOLDER = "users";
    private static final String FIREBASE_STORAGE_IMAGE_NAME = "profile";

    private static final int PROFILE_PICTURE_SIZE = 1024;
    private static final int PROFILE_PICTURE_QUALITY = 50;

    private final Data data;
    private String localImagePath;

    public UserProfile() {
        this.data = new Data();
    }

    public UserProfile(@NonNull Data data, @NonNull Resources resources) {
        this.data = data;
        this.localImagePath = null;
        trimFields(resources);
    }

    public UserProfile(@NonNull UserProfile other) {
        this.data = new Data(other.data);
    }

    public UserProfile(FirebaseUser user, @NonNull Resources resources) {
        this.data = new Data();

        if (user != null) {
            this.data.profile.email = user.getEmail();
            this.data.profile.username = user.getDisplayName();

            for (UserInfo profile : user.getProviderData()) {
                if (this.data.profile.username == null && profile.getDisplayName() != null) {
                    this.data.profile.username = profile.getDisplayName();
                }
            }

            if (this.data.profile.username == null) {
                this.data.profile.username = getUsernameFromEmail(this.data.profile.email);

            }

            this.data.profile.username = Utilities.trimString(this.data.profile.username, resources.getInteger(R.integer.max_length_username));
            this.data.profile.location = resources.getString(R.string.default_city);
        }
    }

    public static void loadFromFirebase(@NonNull OnDataLoadSuccess onSuccess, @NonNull OnDataLoadFailure onFailure) {

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        assert currentUser != null;

        FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_USERS_KEY)
                .child(currentUser.getUid())
                .child(FIREBASE_DATA_KEY)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        onSuccess.apply(dataSnapshot.getValue(UserProfile.Data.class));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        onFailure.apply(databaseError);
                    }
                });
    }

    private static String getUsernameFromEmail(@NonNull String email) {
        return email.substring(0, email.indexOf('@'));
    }

    public void resetProfilePicture() {
        setLocalImagePath(null);
    }

    public void update(@NonNull String username, @NonNull String location, @NonNull String biography) {
        this.data.profile.username = username;
        this.data.profile.location = location;
        this.data.profile.biography = biography;
    }

    private void trimFields(@NonNull Resources resources) {
        this.data.profile.username = Utilities.trimString(this.data.profile.username, resources.getInteger(R.integer.max_length_username));
        this.data.profile.location = Utilities.trimString(this.data.profile.location, resources.getInteger(R.integer.max_length_location));
        this.data.profile.biography = Utilities.trimString(this.data.profile.biography, resources.getInteger(R.integer.max_length_biography));
    }

    public String getUsername() {
        return this.data.profile.username;
    }

    public String getLocation() {
        return this.data.profile.location;
    }

    public String getBiography() {
        return this.data.profile.biography;
    }

    private boolean hasProfilePicture() {
        return this.data.profile.hasProfilePicture;
    }

    public float getRating() {
        return this.data.statistics.rating;
    }

    public int getLentBooks() {
        return this.data.statistics.lentBooks;
    }

    public int getBorrowedBooks() {
        return this.data.statistics.borrowedBooks;
    }

    public int getToBeReturnedBooks() {
        return this.data.statistics.toBeReturnedBooks;
    }

    public String getEmail() {
        return this.data.profile.email;
    }

    public boolean isAnonymous() {
        return this.data.profile.email == null;
    }

    public boolean isLocal() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null || isAnonymous() || this.data.profile.email.equals(user.getEmail());

    }

    public boolean isValid() {
        return Patterns.EMAIL_ADDRESS.matcher(this.data.profile.email).matches() &&
                !Utilities.isNullOrWhitespace(this.data.profile.username) &&
                !Utilities.isNullOrWhitespace(this.data.profile.location) &&
                Utilities.isValidLocation(this.data.profile.location);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof UserProfile)) {
            return false;
        }

        UserProfile otherUP = (UserProfile) other;

        return Utilities.equals(this.getEmail(), otherUP.getEmail()) &&
                Utilities.equals(this.getUsername(), otherUP.getUsername()) &&
                Utilities.equals(this.getLocation(), otherUP.getLocation()) &&
                Utilities.equals(this.getBiography(), otherUP.getBiography()) &&
                this.hasProfilePicture() == otherUP.hasProfilePicture() &&
                Utilities.equals(this.localImagePath, otherUP.localImagePath) &&
                Float.compare(this.getRating(), otherUP.getRating()) == 0 &&
                this.getLentBooks() == otherUP.getLentBooks() &&
                this.getBorrowedBooks() == otherUP.getBorrowedBooks() &&
                this.getToBeReturnedBooks() == otherUP.getToBeReturnedBooks();
    }

    public boolean imageUpdated(UserProfile other) {
        return this.hasProfilePicture() != other.hasProfilePicture() ||
                !Utilities.equals(this.localImagePath, other.localImagePath);
    }

    public Task<Void> saveToFirebase(@NonNull Resources resources) {
        this.trimFields(resources);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        assert currentUser != null;

        return FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_USERS_KEY)
                .child(currentUser.getUid())
                .child(FIREBASE_DATA_KEY)
                .child(FIREBASE_PROFILE_KEY)
                .setValue(this.data.profile);
    }

    public Task<?> updateImageOnFirebase() {
        if (hasProfilePicture() && localImagePath != null) {

            // TODO: check if it is necessary to do this in a worker thread
            ByteArrayOutputStream out = PictureUtilities.compressImage(localImagePath, PROFILE_PICTURE_SIZE, PROFILE_PICTURE_SIZE, PROFILE_PICTURE_QUALITY);
            if (out != null) {

                StorageMetadata metadata = new StorageMetadata.Builder()
                        .setContentType("image/webp")
                        .build();

                return getProfilePictureReference()
                        .putBytes(out.toByteArray(), metadata)
                        .addOnSuccessListener(t ->
                                this.localImagePath = null);
            }
        }

        return getProfilePictureReference()
                .delete();

    }

    private StorageReference getProfilePictureReference() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        assert currentUser != null;

        return FirebaseStorage.getInstance().getReference()
                .child(FIREBASE_STORAGE_USERS_FOLDER)
                .child(currentUser.getUid())
                .child(FIREBASE_STORAGE_IMAGE_NAME);
    }

    public String getLocalImagePath() {
        return this.localImagePath;
    }

    public void setLocalImagePath(String path) {
        this.data.profile.hasProfilePicture = path != null;
        localImagePath = path;
    }

    public Object getImageReference() {
        return this.localImagePath == null
                ? this.hasProfilePicture()
                ? this.getProfilePictureReference()
                : null
                : this.localImagePath;
    }

    public interface OnDataLoadSuccess {
        void apply(UserProfile.Data data);
    }

    public interface OnDataLoadFailure {
        void apply(DatabaseError databaseError);
    }

    /* Fields need to be public to enable Firebase to access them */
    @SuppressWarnings("WeakerAccess")
    public static class Data implements Serializable {

        public Profile profile;
        public Statistics statistics;

        public Data() {
            this.profile = new Profile();
            this.statistics = new Statistics();
        }

        public Data(@NonNull Data other) {
            this.profile = new Profile(other.profile);
            this.statistics = new Statistics(other.statistics);
        }

        private static class Profile implements Serializable {
            public String email;
            public String username;
            public String location;
            public String biography;
            public boolean hasProfilePicture;

            public Profile() {
                this.email = null;
                this.username = null;
                this.location = null;
                this.biography = null;
                this.hasProfilePicture = false;
            }

            public Profile(@NonNull Profile other) {
                this.email = other.email;
                this.username = other.username;
                this.location = other.location;
                this.biography = other.biography;
                this.hasProfilePicture = other.hasProfilePicture;
            }
        }

        private static class Statistics implements Serializable {
            public float rating;
            public int lentBooks;
            public int borrowedBooks;
            public int toBeReturnedBooks;

            public Statistics() {
                this.rating = 0;
                this.lentBooks = 0;
                this.borrowedBooks = 0;
                this.toBeReturnedBooks = 0;
            }

            public Statistics(@NonNull Statistics other) {
                this.rating = other.rating;
                this.lentBooks = other.lentBooks;
                this.borrowedBooks = other.borrowedBooks;
                this.toBeReturnedBooks = other.toBeReturnedBooks;
            }
        }
    }
}