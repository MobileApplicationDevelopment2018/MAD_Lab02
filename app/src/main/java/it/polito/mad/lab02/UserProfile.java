package it.polito.mad.lab02;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Patterns;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;

class UserProfile implements Serializable {

    static final String PROFILE_INFO_KEY = "profile_info_key";

    private static final String FIREBASE_USERS_KEY = "users";
    private static final String FIREBASE_DATA_KEY = "data";
    private static final String FIREBASE_PROFILE_KEY = "profile";

    private final Data data;

    UserProfile() {
        this.data = new Data();
    }

    UserProfile(@NonNull Data data, @NonNull Resources resources) {
        this.data = data;
        trimFields(resources);
    }

    UserProfile(@NonNull UserProfile other) {
        this.data = new Data(other.data);
    }

    UserProfile(FirebaseUser user, @NonNull Resources resources) {
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

    static void loadFromFirebase(@NonNull OnDataLoadSuccess onSuccess, @NonNull OnDataLoadFailure onFailure) {

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

    void update(String imagePath) {
        this.data.profile.imagePath = imagePath;
    }

    void update(@NonNull String username, @NonNull String location, @NonNull String biography) {
        this.data.profile.username = username;
        this.data.profile.location = location;
        this.data.profile.biography = biography;
    }

    private void trimFields(@NonNull Resources resources) {
        this.data.profile.username = Utilities.trimString(this.data.profile.username, resources.getInteger(R.integer.max_length_username));
        this.data.profile.location = Utilities.trimString(this.data.profile.location, resources.getInteger(R.integer.max_length_location));
        this.data.profile.biography = Utilities.trimString(this.data.profile.biography, resources.getInteger(R.integer.max_length_biography));
    }

    String getUsername() {
        return this.data.profile.username;
    }

    String getLocation() {
        return this.data.profile.location;
    }

    String getBiography() {
        return this.data.profile.biography;
    }

    String getImagePath() {
        return this.data.profile.imagePath;
    }

    float getRating() {
        return this.data.statistics.rating;
    }

    int getLentBooks() {
        return this.data.statistics.lentBooks;
    }

    int getBorrowedBooks() {
        return this.data.statistics.borrowedBooks;
    }

    int getToBeReturnedBooks() {
        return this.data.statistics.toBeReturnedBooks;
    }

    String getEmail() {
        return this.data.profile.email;
    }

    boolean isAnonymous() {
        return this.data.profile.email == null;
    }

    boolean isLocal() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null || isAnonymous() || this.data.profile.email.equals(user.getEmail());

    }

    boolean isValid() {
        return Patterns.EMAIL_ADDRESS.matcher(this.data.profile.email).matches() &&
                !Utilities.isNullOrWhitespace(this.data.profile.username) &&
                !Utilities.isNullOrWhitespace(this.data.profile.location) &&
                Utilities.isValidLocation(this.data.profile.location);
    }

    @Exclude
    Bitmap getImageBitmapOrDefault(@NonNull Resources resources, int targetWidth, int targetHeight) {
        return Utilities.loadImage(this.data.profile.imagePath, targetWidth, targetHeight, resources, R.drawable.default_header);
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
                Utilities.equals(this.getImagePath(), otherUP.getImagePath()) &&
                Float.compare(this.getRating(), otherUP.getRating()) == 0 &&
                this.getLentBooks() == otherUP.getLentBooks() &&
                this.getBorrowedBooks() == otherUP.getBorrowedBooks() &&
                this.getToBeReturnedBooks() == otherUP.getToBeReturnedBooks();
    }

    Task<Void> saveToFirebase(@NonNull Resources resources) {
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

    void saveToFirebase(@NonNull Resources resources, @NonNull OnSuccessListener<? super Void> onSuccess, @NonNull OnFailureListener onFailure) {
        saveToFirebase(resources)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    interface OnDataLoadSuccess {
        void apply(UserProfile.Data data);
    }

    interface OnDataLoadFailure {
        void apply(DatabaseError databaseError);
    }

    /* Fields need to be public to enable Firebase to access them */
    @SuppressWarnings("WeakerAccess")
    static class Data implements Serializable {

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
            public String imagePath;

            public Profile() {
                this.email = null;
                this.username = null;
                this.location = null;
                this.biography = null;
                this.imagePath = null;
            }

            public Profile(@NonNull Profile other) {
                this.email = other.email;
                this.username = other.username;
                this.location = other.location;
                this.biography = other.biography;
                this.imagePath = other.imagePath;
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
