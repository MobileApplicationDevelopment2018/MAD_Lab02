package it.polito.mad.lab02;

import android.content.Context;
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
    private static final String FIREBASE_PROFILE_KEY = "profile";

    private String email;
    private String username;
    private String location;
    private String biography;
    private String imagePath;

    private float rating;
    private int lentBooks;
    private int borrowedBooks;
    private int toBeReturnedBooks;

    UserProfile() {
        this.email = null;
        this.username = null;
        this.location = null;
        this.biography = null;
        this.imagePath = null;

        this.rating = 0;
        this.lentBooks = 0;
        this.borrowedBooks = 0;
        this.toBeReturnedBooks = 0;
    }

    UserProfile(@NonNull UserProfile other) {
        this.email = other.getEmail();
        this.username = other.getUsername();
        this.location = other.getLocation();
        this.biography = other.getBiography();

        this.imagePath = other.imagePath;

        this.rating = other.getRating();
        this.lentBooks = other.getLentBooks();
        this.borrowedBooks = other.getBorrowedBooks();
        this.toBeReturnedBooks = other.getToBeReturnedBooks();
    }

    UserProfile(@NonNull Context context, FirebaseUser user) {
        super();

        if (user != null) {
            this.email = user.getEmail();
            this.username = user.getDisplayName();

            for (UserInfo profile : user.getProviderData()) {
                if (this.username == null && profile.getDisplayName() != null) {
                    this.username = profile.getDisplayName();
                }
            }

            if (this.username == null) {
                this.username = getUsernameFromEmail(this.email);

            }

            this.username = this.username
                    .trim().replaceAll("\\p{Zs}+", " ");

            int maxLength = context.getResources().getInteger(R.integer.max_length_username);
            if (this.username.length() > maxLength) {
                this.username = this.username.substring(0, maxLength);
            }

            this.location = context.getString(R.string.default_city);
        }
    }

    static void loadFromFirebase(@NonNull OnDataLoadSuccess onSuccess, @NonNull OnDataLoadFailure onFailure) {

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        assert currentUser != null;

        FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_USERS_KEY)
                .child(currentUser.getUid())
                .child(FIREBASE_PROFILE_KEY)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        onSuccess.apply(dataSnapshot);
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
        this.imagePath = imagePath;
    }

    void update(@NonNull String username, @NonNull String location, @NonNull String biography) {
        this.username = username;
        this.location = location;
        this.biography = biography;
    }

    private void trimFields() {
        this.username = this.username.trim().replaceAll("\\p{Zs}+", " ");
        this.location = this.location.trim().replaceAll("\\p{Zs}+", " ");

        if (this.biography != null) {
            this.biography = this.biography.trim().replaceAll("\\p{Zs}+", " ");
        }
    }

    String getUsername() {
        return this.username;
    }

    String getLocation() {
        return this.location;
    }

    String getBiography() {
        return this.biography;
    }

    String getImagePath() {
        return this.imagePath;
    }

    float getRating() {
        return this.rating;
    }

    int getLentBooks() {
        return this.lentBooks;
    }

    int getBorrowedBooks() {
        return this.borrowedBooks;
    }

    int getToBeReturnedBooks() {
        return this.toBeReturnedBooks;
    }

    /* BEGIN GETTERS */
    String getEmail() {
        return this.email;
    }

    @Exclude
    boolean isAnonymous() {
        return this.email == null;
    }
    /* END GETTERS */

    @Exclude
    boolean isLocal() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        assert this.email != null;

        return this.email.equals(user.getEmail());
    }

    @Exclude
    boolean isValid() {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                !Utilities.isNullOrWhitespace(username) &&
                !Utilities.isNullOrWhitespace(location) &&
                Utilities.isValidLocation(location);
    }

    @Exclude
    Bitmap getImageBitmapOrDefault(@NonNull Context ctx, int targetWidth, int targetHeight) {
        return Utilities.loadImage(this.imagePath, targetWidth, targetHeight, ctx.getResources(), R.drawable.default_header);
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

    Task<Void> saveToFirebase() {
        this.trimFields();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        assert currentUser != null;

        return FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_USERS_KEY)
                .child(currentUser.getUid())
                .child(FIREBASE_PROFILE_KEY)
                .setValue(this);
    }

    void saveToFirebase(@NonNull OnSuccessListener<? super Void> onSuccess, @NonNull OnFailureListener onFailure) {
        saveToFirebase()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    interface OnDataLoadSuccess {
        void apply(DataSnapshot dataSnapshot);
    }

    interface OnDataLoadFailure {
        void apply(DatabaseError databaseError);
    }
}
