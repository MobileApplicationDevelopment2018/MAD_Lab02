package it.polito.mad.lab02.data;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.books.model.Volume;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Book implements Serializable {
    private static final String FIREBASE_BOOKS_KEY = "books";
    private static final String FIREBASE_DATA_KEY = "data";
    private static final String FIREBASE_BOOK_INFO_KEY = "book_info";

    private final Book.Data data;

    public Book(Data data) {
        this.data = data;
    }

    public Book(@NonNull String isbn, @NonNull Volume.VolumeInfo volumeInfo) {
        this.data = new Data();
        this.data.bookInfo.isbn = isbn;
        this.data.bookInfo.title = volumeInfo.getTitle();
        this.data.bookInfo.authors = volumeInfo.getAuthors();
        this.data.bookInfo.language = volumeInfo.getLanguage();
        this.data.bookInfo.publisher = volumeInfo.getPublisher();

        String year = volumeInfo.getPublishedDate();
        if (year != null && year.length() >= 4) {
            this.data.bookInfo.year = Integer.parseInt(year.substring(0, 4));
        }
    }

    public Book(String isbn, @NonNull String title, @NonNull List<String> authors, @NonNull String language,
                String publisher, int year) {
        this.data = new Data();
        this.data.bookInfo.isbn = isbn;
        this.data.bookInfo.title = title;
        this.data.bookInfo.authors = authors;
        this.data.bookInfo.language = language;
        this.data.bookInfo.publisher = publisher;
        this.data.bookInfo.year = year;
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

    public void setConditions(@NonNull String conditions) {
        this.data.bookInfo.conditions = conditions;
    }

    public List<String> getTags() {
        return this.data.bookInfo.tags;
    }

    public void setTags(@NonNull List<String> tags) {
        this.data.bookInfo.tags = tags;
    }

    private void trimFields(@NonNull Resources resources) {
        // TODO: trim blank spaces, etc. ?
    }

    public Task<?> saveToFirebase(@NonNull Resources resources) {
        this.trimFields(resources);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        assert currentUser != null;

        String bookId = FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_BOOKS_KEY)
                .push()
                .getKey();

        List<Task<?>> tasks = new ArrayList<>();

        tasks.add(FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_BOOKS_KEY)
                .child(bookId)
                .child(FIREBASE_DATA_KEY)
                .child(FIREBASE_BOOK_INFO_KEY)
                .setValue(this.data.bookInfo));

        tasks.add(FirebaseDatabase.getInstance().getReference()
                .child(UserProfile.FIREBASE_USERS_KEY)
                .child(currentUser.getUid())
                .child(UserProfile.FIREBASE_DATA_KEY)
                .child(UserProfile.FIREBASE_BOOKS_KEY)
                .child(bookId)
                .setValue(true));

        return Tasks.whenAllSuccess(tasks);
    }

    /* Fields need to be public to enable Firebase to access them */
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    public static class Data implements Serializable {
        public BookInfo bookInfo;

        public Data() {
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
                this.authors = null;
                this.language = null;
                this.publisher = null;
                this.year = 1900;
                this.conditions = null;
                this.tags = null;
            }
        }
    }
}
