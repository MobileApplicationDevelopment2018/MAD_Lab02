package it.polito.mad.mad2018.data;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.books.model.Volume;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.polito.mad.mad2018.R;
import it.polito.mad.mad2018.utils.PictureUtilities;
import it.polito.mad.mad2018.utils.Utilities;

public class Book implements Serializable {
    public static final int INITIAL_YEAR = 1900;
    private static final String FIREBASE_BOOKS_KEY = "books";
    private static final String FIREBASE_DATA_KEY = "data";

    public static final int BOOK_PICTURE_SIZE = 1024;
    public static final int BOOK_PICTURE_QUALITY = 50;
    private static final String FIREBASE_STORAGE_BOOKS_FOLDER = "books";
    private static final String FIREBASE_STORAGE_IMAGE_NAME = "picture";
    private String bookId;
    private final Book.Data data;

    public Book(@NonNull String bid, @NonNull Data data) {
        this.bookId = bid;
        this.data = data;
    }

    public Book(@NonNull String isbn, @NonNull Volume.VolumeInfo volumeInfo, Locale locale) {
        this.bookId = null;
        this.data = new Data();

        this.data.bookInfo.isbn = isbn;
        this.data.bookInfo.title = volumeInfo.getTitle();
        this.data.bookInfo.authors = volumeInfo.getAuthors();
        this.data.bookInfo.publisher = volumeInfo.getPublisher();

        if (volumeInfo.getLanguage() != null) {
            this.data.bookInfo.language = new Locale(volumeInfo.getLanguage())
                    .getDisplayLanguage(locale);
        }

        String year = volumeInfo.getPublishedDate();
        if (year != null && year.length() >= 4) {
            this.data.bookInfo.year = Integer.parseInt(year.substring(0, 4));
        }

        if (volumeInfo.getCategories() != null) {
            for (String category : volumeInfo.getCategories()) {
                this.data.bookInfo.tags.add(String.format(locale, "%s", category));
            }
        }
    }

    public Book(String isbn, @NonNull String title, @NonNull List<String> authors, @NonNull String language,
                String publisher, int year, String conditions, @NonNull List<String> tags,
                @NonNull Resources resources) {
        this.bookId = null;
        this.data = new Data();

        for (String author : authors) {
            if (!Utilities.isNullOrWhitespace(author)) {
                this.data.bookInfo.authors.add(Utilities.trimString(author, resources.getInteger(R.integer.max_length_author)));
            }
        }

        this.data.bookInfo.isbn = isbn;
        this.data.bookInfo.title = Utilities.trimString(title, resources.getInteger(R.integer.max_length_title));
        this.data.bookInfo.language = Utilities.trimString(language, resources.getInteger(R.integer.max_length_language));
        this.data.bookInfo.publisher = Utilities.trimString(publisher, resources.getInteger(R.integer.max_length_publisher));
        this.data.bookInfo.year = year;

        this.data.bookInfo.conditions = conditions;
        for (String tag : tags) {
            if (!Utilities.isNullOrWhitespace(tag)) {
                this.data.bookInfo.tags.add(Utilities.trimString(tag, resources.getInteger(R.integer.max_length_tag)));
            }
        }
    }

    public String getBookId() {
        return this.bookId;
    }

    public String getIsbn() {
        return this.data.bookInfo.isbn;
    }

    public String getTitle() {
        return this.data.bookInfo.title;
    }

    public List<String> getAuthors() {
        return this.data.bookInfo.authors;
    }

    public String getAuthors(@NonNull String delimiter) {
        return TextUtils.join(delimiter, this.data.bookInfo.authors);
    }

    public String getLanguage() {
        return this.data.bookInfo.language;
    }

    public String getPublisher() {
        return this.data.bookInfo.publisher;
    }

    public int getYear() {
        return this.data.bookInfo.year;
    }

    public String getConditions() {
        return this.data.bookInfo.conditions;
    }

    public List<String> getTags() {
        return this.data.bookInfo.tags;
    }

    private StorageReference getBookPictureReference() {
        assert this.bookId != null;
        return FirebaseStorage.getInstance().getReference()
                .child(FIREBASE_STORAGE_BOOKS_FOLDER)
                .child(this.bookId)
                .child(FIREBASE_STORAGE_IMAGE_NAME);
    }

    public Task<?> saveToFirebase() {
        return saveToFirebase(null);
    }

    public Task<?> saveToFirebase(ByteArrayOutputStream picture) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        assert currentUser != null;

        this.data.uid = currentUser.getUid();
        this.bookId = FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_BOOKS_KEY)
                .push()
                .getKey();

        List<Task<?>> tasks = new ArrayList<>();

        tasks.add(FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_BOOKS_KEY)
                .child(bookId)
                .child(FIREBASE_DATA_KEY)
                .setValue(this.data));

        tasks.add(FirebaseDatabase.getInstance().getReference()
                .child(UserProfile.FIREBASE_USERS_KEY)
                .child(this.data.uid)
                .child(UserProfile.FIREBASE_DATA_KEY)
                .child(UserProfile.FIREBASE_BOOKS_KEY)
                .child(bookId)
                .setValue(true));

        if (picture != null) {
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType(PictureUtilities.IMAGE_CONTENT_TYPE_UPLOAD)
                    .build();

            tasks.add(getBookPictureReference()
                    .putBytes(picture.toByteArray(), metadata));
        }

        return Tasks.whenAllSuccess(tasks);
    }

    /* Fields need to be public to enable Firebase to access them */
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    public static class Data implements Serializable {
        public String uid;
        public BookInfo bookInfo;

        public Data() {
            this.uid = null;
            this.bookInfo = new BookInfo();
        }

        private static class BookInfo implements Serializable {
            public String isbn;
            public String title;
            public List<String> authors;
            public String language;
            public String publisher;
            public int year;
            public String conditions;
            public List<String> tags;

            public BookInfo() {
                this.isbn = null;
                this.title = null;
                this.authors = new ArrayList<>();
                this.language = null;
                this.publisher = null;
                this.year = INITIAL_YEAR;
                this.conditions = null;
                this.tags = new ArrayList<>();
            }
        }
    }
}
