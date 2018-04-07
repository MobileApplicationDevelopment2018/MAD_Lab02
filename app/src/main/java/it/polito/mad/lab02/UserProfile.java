package it.polito.mad.lab02;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Patterns;

import java.io.Serializable;

class UserProfile implements Serializable {

    static final String PROFILE_INFO_KEY = "profile_info_key";
    private static final String EMAIL_PREFERENCE_KEY = "email";
    private static final String USERNAME_PREFERENCE_KEY = "username";
    private static final String LOCATION_PREFERENCE_KEY = "location";
    private static final String BIOGRAPHY_PREFERENCE_KEY = "biography";
    private static final String IMAGE_PREFERENCE_KEY = "image";

    private String email;
    private String username;
    private String location;
    private String biography;
    private String imagePath;

    private float rating;
    private int lentBooks;
    private int borrowedBooks;
    private int toBeReturnedBooks;

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

    UserProfile(@NonNull Context ctx, @NonNull String id, @NonNull SharedPreferences sharedPref) {

        this.email = sharedPref.getString(id + "_" + EMAIL_PREFERENCE_KEY, ctx.getString(R.string.default_email));
        this.username = sharedPref.getString(id + "_" + USERNAME_PREFERENCE_KEY, ctx.getString(R.string.default_username));
        this.location = sharedPref.getString(id + "_" + LOCATION_PREFERENCE_KEY, ctx.getString(R.string.default_city));
        this.biography = sharedPref.getString(id + "_" + BIOGRAPHY_PREFERENCE_KEY, ctx.getString(R.string.default_biography));

        this.imagePath = null;
        this.imagePath = sharedPref.getString(id + "_" + IMAGE_PREFERENCE_KEY, null);

        this.rating = 4.5f;
        this.lentBooks = 18;
        this.borrowedBooks = 24;
        this.toBeReturnedBooks = 2;
    }

    void update(@NonNull String email, @NonNull String username, @NonNull String location, @NonNull String biography) {
        this.email = email;
        this.username = username;
        this.location = location;
        this.biography = biography;
    }

    void update(String imagePath) {
        this.imagePath = imagePath;
    }

    void trimFields() {
        this.username = this.username.trim();
        this.location = this.location.trim();
        this.biography = this.biography.trim();
        this.username = this.username.replaceAll("\\p{Zs}+", " ");
        this.location = this.location.replaceAll("\\p{Zs}+", " ");
        this.biography = this.biography.replaceAll("\\p{Zs}+", " ");
    }

    void save(@NonNull String id, @NonNull SharedPreferences.Editor sharedPrefEditor) {
        sharedPrefEditor.putString(id + "_" + EMAIL_PREFERENCE_KEY, this.getEmail());
        sharedPrefEditor.putString(id + "_" + USERNAME_PREFERENCE_KEY, this.getUsername());
        sharedPrefEditor.putString(id + "_" + LOCATION_PREFERENCE_KEY, this.getLocation());
        sharedPrefEditor.putString(id + "_" + BIOGRAPHY_PREFERENCE_KEY, this.getBiography());
        if (this.imagePath != null) {
            sharedPrefEditor.putString(id + "_" + IMAGE_PREFERENCE_KEY, this.imagePath);
        }
    }

    boolean isValid() {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                !Utilities.isNullOrWhitespace(username) &&
                !Utilities.isNullOrWhitespace(location) &&
                Utilities.isValidLocation(location);
    }

    String getEmail() {
        return this.email;
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

    Bitmap getImageBitmapOrDefault(@NonNull Context ctx, int targetWidth, int targetHeight) {
        return Utilities.loadImage(this.imagePath, targetWidth, targetHeight, ctx.getResources(), R.drawable.default_header);
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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof UserProfile)) {
            return false;
        }

        UserProfile otherUP = (UserProfile) other;

        String thisImagePath = this.imagePath;
        String otherImagePath = otherUP.imagePath;

        return this.getEmail().equals(otherUP.getEmail()) &&
                this.getUsername().equals(otherUP.getUsername()) &&
                this.getLocation().equals(otherUP.getLocation()) &&
                this.getBiography().equals(otherUP.getBiography()) &&
                Float.compare(this.getRating(), otherUP.getRating()) == 0 &&
                this.getLentBooks() == otherUP.getLentBooks() &&
                this.getBorrowedBooks() == otherUP.getBorrowedBooks() &&
                this.getToBeReturnedBooks() == otherUP.getToBeReturnedBooks() &&
                (thisImagePath == null && otherImagePath == null ||
                        thisImagePath != null && thisImagePath.equals(otherImagePath));
    }
}
