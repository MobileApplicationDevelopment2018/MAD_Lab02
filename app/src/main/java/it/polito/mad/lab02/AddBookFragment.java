package it.polito.mad.lab02;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.api.services.books.model.Volume;
import com.google.api.services.books.model.Volumes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import it.polito.mad.lab02.barcodereader.BarcodeCaptureActivity;
import it.polito.mad.lab02.data.Book;
import it.polito.mad.lab02.utils.IsbnQuery;
import it.polito.mad.lab02.utils.Utilities;
import me.gujun.android.taggroup.TagGroup;

public class AddBookFragment extends Fragment implements IsbnQuery.TaskListener {

    private static final String TASK_RUNNING = "task_running";
    private static final int RC_BARCODE_CAPTURE = 9001;

    private ProgressDialog progressDialog;
    private boolean isTaskRunning;
    private IsbnQuery isbnQuery;

    private EditText isbnEdit, titleEt, publisherEt, languageEt;
    private Spinner yearSpinner, conditionSpinner;
    private Button scanBarcodeBtn, addBookBtn, resetBtn, autocompleteBtn;
    private TagGroup tagGroup, authorEtGroup;

    private Locale currentLocale;

    public static AddBookFragment newInstance() {
        AddBookFragment fragment = new AddBookFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_add_book, container, false);
        findViews(view);

        // Buttons watchers
        scanBarcodeBtn.setOnClickListener(v -> {
            // launch barcode activity.
            Intent intent = new Intent(this.getContext(), BarcodeCaptureActivity.class);
            startActivityForResult(intent, RC_BARCODE_CAPTURE);
        });

        autocompleteBtn.setOnClickListener(v -> {
            if (!isTaskRunning) {
                isbnQuery = new IsbnQuery(this);
                isbnQuery.execute(isbnEdit.getText().toString());
            }
        });

        resetBtn.setOnClickListener(v -> clearViews(true));
        addBookBtn.setOnClickListener(v -> uploadBook());

        // Tag watcher
        tagGroup.setOnClickListener(v -> ((TagGroup) v.findViewById(R.id.tag_group)).submitTag());

        // Fields watchers
        isbnEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

                boolean isbnValid = Utilities.validateIsbn(isbnEdit.getText().toString());
                autocompleteBtn.setEnabled(isbnValid);
                if (!isbnValid && isbnEdit.getText().length() != 0) {
                    isbnEdit.setError(getString(R.string.add_book_invalid_isbn));
                }
            }
        });

        fillSpinnerYear(view);
        currentLocale = getResources().getConfiguration().locale;

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        assert getActivity() != null;
        getActivity().setTitle(R.string.add_book);

        if (savedInstanceState != null && savedInstanceState.getBoolean(TASK_RUNNING)) {
            progressDialog = ProgressDialog.show(getActivity(),
                    getResources().getString(R.string.add_book_isbn_loading_title),
                    getResources().getString(R.string.add_book_isbn_loading_message));
        }
    }

    @Override
    public void onDetach() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onDetach();
    }

    @Override
    public void onTaskStarted() {
        isTaskRunning = true;
        progressDialog = ProgressDialog.show(getActivity(),
                getResources().getString(R.string.add_book_isbn_loading_title),
                getResources().getString(R.string.add_book_isbn_loading_message));
    }

    @Override
    public void onTaskFinished(Volumes volumes) {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }

        isTaskRunning = false;
        if (volumes == null) {
            Toast.makeText(getContext(), getResources().getString(R.string.add_book_isbn_query_failed), Toast.LENGTH_LONG).show();
        } else if (volumes.getTotalItems() == 0 || volumes.getItems() == null) {
            Toast.makeText(getContext(), getResources().getString(R.string.add_book_isbn_query_no_results), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), R.string.add_book_query_ok, Toast.LENGTH_LONG).show();

            final Volume.VolumeInfo volumeInfo = volumes.getItems().get(0).getVolumeInfo();
            Book book = new Book(isbnEdit.getText().toString(), volumeInfo, currentLocale);
            fillViews(book);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(TASK_RUNNING, isTaskRunning);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {

                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    if (!barcode.displayValue.equals(isbnEdit.getText().toString())) {
                        clearViews(false);
                    }

                    isbnEdit.setText(barcode.displayValue);
                }
            } else {
                Toast.makeText(this.getContext(), String.format(getString(R.string.barcode_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)), Toast.LENGTH_LONG)
                        .show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void clearViews(boolean clearIsbn) {
        if (clearIsbn) {
            isbnEdit.setText(null);
        }

        titleEt.setText(null);
        authorEtGroup.setTags(new LinkedList<>());
        publisherEt.setText(null);
        yearSpinner.setSelection(0);
        languageEt.setText(null);
        tagGroup.setTags(new LinkedList<>());
    }

    private void uploadBook() {

        String isbn = isbnEdit.getText().toString();
        String title = titleEt.getText().toString();
        String[] authors = authorEtGroup.getTags();
        String language = languageEt.getText().toString();
        String publisher = publisherEt.getText().toString();
        int year = Integer.parseInt(yearSpinner.getSelectedItem().toString());

        if (!checkMandatoryFieldsInput(isbn, title, authors))
            return;

        String condition = conditionSpinner.getSelectedItem().toString();
        List<String> tags = Arrays.asList(tagGroup.getTags());
        Book book = new Book(isbn, title, Arrays.asList(authors), language, publisher, year,
                condition, tags, getResources());

        book.saveToFirebase()
                .addOnSuccessListener((v) -> {
                    Toast.makeText(getContext(), getResources().getString(R.string.add_book_saved), Toast.LENGTH_SHORT).show();
                    clearViews(true);
                })
                .addOnFailureListener((v) -> Toast.makeText(getContext(), getResources().getString(R.string.add_book_error), Toast.LENGTH_SHORT).show());

    }

    private void findViews(View view) {
        // Buttons
        scanBarcodeBtn = view.findViewById(R.id.ab_barcode_scan);
        autocompleteBtn = view.findViewById(R.id.ab_autocomplete);
        addBookBtn = view.findViewById(R.id.ab_add_book);
        resetBtn = view.findViewById(R.id.ab_clear_fields);

        // Isbn view
        isbnEdit = view.findViewById(R.id.ab_isbn_edit);

        // User-filled views
        titleEt = view.findViewById(R.id.ab_title_edit);
        authorEtGroup = view.findViewById(R.id.tag_group_authors);
        publisherEt = view.findViewById(R.id.ab_publisher_edit);
        languageEt = view.findViewById(R.id.ab_language_edit);
        yearSpinner = view.findViewById(R.id.ab_edition_year_edit);
        conditionSpinner = view.findViewById(R.id.ab_conditions);

        // Tags
        tagGroup = view.findViewById(R.id.tag_group);
    }

    private void fillViews(@NonNull Book book) {
        clearViews(false);

        titleEt.setText(book.getTitle());
        publisherEt.setText(book.getPublisher());
        authorEtGroup.setTags(book.getAuthors());

        int selection = book.getYear() - Book.INITIAL_YEAR;
        if (selection < 0) {
            selection = 0;
        }
        yearSpinner.setSelection(selection);
        languageEt.setText(book.getLanguage());
        tagGroup.setTags(book.getTags());
    }

    private void fillSpinnerYear(View view) {
        ArrayList<String> years = new ArrayList<>();
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);

        for (int i = Book.INITIAL_YEAR; i <= thisYear; i++)
            years.add(Integer.toString(i));

        assert getActivity() != null;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, years);

        Spinner spinYear = view.findViewById(R.id.ab_edition_year_edit);
        spinYear.setAdapter(adapter);
    }

    private boolean checkMandatoryFieldsInput(String isbn, String title, String[] authors) {
        titleEt.setError(null);

        boolean ok = true;

        if (Utilities.isNullOrWhitespace(title)) {
            Toast.makeText(getContext(), getResources().getString(R.string.ab_field_must_not_be_empty), Toast.LENGTH_LONG).show();
            ok = false;
        }

        for (String author : authors) {
            if (Utilities.isNullOrWhitespace(author)) {
                Toast.makeText(getContext(), getResources().getString(R.string.ab_field_must_not_be_empty), Toast.LENGTH_LONG).show();
                ok = false;
                break;
            }
        }

        return ok && isbn.length() == 0 || Utilities.validateIsbn(isbn);
    }
}
