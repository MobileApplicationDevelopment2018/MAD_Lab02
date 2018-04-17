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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.api.services.books.model.Volume;
import com.google.api.services.books.model.Volumes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import it.polito.mad.lab02.barcodereader.BarcodeCaptureActivity;
import it.polito.mad.lab02.data.Book;
import it.polito.mad.lab02.utils.IsbnQuery;
import it.polito.mad.lab02.utils.Utilities;
import me.gujun.android.taggroup.TagGroup;

public class AddBookFragment extends Fragment implements IsbnQuery.TaskListener {

    private static final int RC_BARCODE_CAPTURE = 9001;

    private ProgressDialog progressDialog;
    private boolean isTaskRunning;
    private IsbnQuery isbnQuery;
    private int wrapperVisibility;
    private Book book;
    private boolean inAutocomplete;
    private TextView bookTitle, bookAuthor, bookPublisher, bookYear, bookLanguage;
    private EditText isbnEdit, titleEt, authorEt, publisherEt, languageEt;
    private Spinner yearEt;
    private Button scanBarcodeBtn, addBookBtn, resetBtn, startQueryBtn, addBookBtnNoAuto, resetBtnNoAuto;
    private Locale currentLocale;
    private TagGroup mTagGroup;
    private Switch autofill;
    private ViewSwitcher switcher;

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
        if (savedInstanceState == null) {
            wrapperVisibility = View.GONE;
        } else {
            wrapperVisibility = savedInstanceState.getInt("wrapperVisibility");
            addBookBtn.setEnabled(true);
        }

        LinearLayout wrapper = view.findViewById(R.id.ab_autofilled_info_wrapper);
        wrapper.setVisibility(wrapperVisibility);

        autofill.setOnCheckedChangeListener((buttonView, isChecked) -> {
            switcher.showNext();
            View currentView = switcher.getCurrentView();
            if (currentView.getId() == R.id.ab_autofill_book_view && wrapperVisibility == View.VISIBLE) {
                addBookBtn.setEnabled(true);
            } else {
                addBookBtn.setEnabled(false);
            }
        });

        // Buttons watchers
        scanBarcodeBtn.setOnClickListener((view3) -> {
            // launch barcode activity.
            Intent intent = new Intent(this.getContext(), BarcodeCaptureActivity.class);
            startActivityForResult(intent, RC_BARCODE_CAPTURE);
        });

        startQueryBtn.setOnClickListener((view2) -> {
            if (!isTaskRunning) {
                isbnQuery = new IsbnQuery(this);
                isbnQuery.execute(isbnEdit.getText().toString());
            }
        });

        resetBtn.setOnClickListener(v -> clearViews());
        addBookBtn.setOnClickListener(v -> uploadBook());
        resetBtnNoAuto.setOnClickListener(v -> clearViews());
        addBookBtnNoAuto.setOnClickListener(v -> uploadBook());

        // Tag watcher
        mTagGroup.setOnClickListener(v -> ((TagGroup) v.findViewById(R.id.tag_group)).submitTag());

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
                if (Utilities.validateIsbn(isbnEdit.getText().toString())) {
                    isbnEdit.setError(null);
                    startQueryBtn.setEnabled(true);
                    addBookBtn.setEnabled(true);
                    addBookBtnNoAuto.setEnabled(true);
                } else {
                    if (isbnEdit.getText().length() != 0)
                        isbnEdit.setError(getString(R.string.add_book_invalid_isbn));
                    startQueryBtn.setEnabled(false);
                    addBookBtn.setEnabled(true);
                    addBookBtnNoAuto.setEnabled(false);
                }
            }
        });

        fillSpinnerYear(view);
        currentLocale = getResources().getConfiguration().locale;

        return view;
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
            Toast.makeText(getContext(), getResources().getString(R.string.add_book_isbn_query_failed), Toast.LENGTH_SHORT).show();
        } else if (volumes.getTotalItems() == 0 || volumes.getItems() == null) {
            Toast.makeText(getContext(), getResources().getString(R.string.add_book_isbn_query_no_results), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), R.string.add_book_query_ok, Toast.LENGTH_SHORT).show();

            LinearLayout wrapper = getView().findViewById(R.id.ab_autofilled_info_wrapper);
            final Volume.VolumeInfo volumeInfo = volumes.getItems().get(0).getVolumeInfo();

            isbnEdit = getView().findViewById(R.id.ab_isbn_edit);
            String language = new Locale(volumeInfo.getLanguage()).getDisplayLanguage(currentLocale);
            book = new Book(isbnEdit.getText().toString(), volumeInfo, language);

            fillViewsAutofilled(book);

            wrapperVisibility = View.VISIBLE;
            wrapper.setVisibility(View.VISIBLE);

            addBookBtn.setEnabled(true);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isTaskRunning", isTaskRunning);
        outState.putInt("wrapperVisibility", wrapperVisibility);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);

                    if (!barcode.displayValue.equals(((EditText) getView().findViewById(R.id.ab_isbn_edit)).getText()))
                        clearViews();

                    ((EditText) getView().findViewById(R.id.ab_isbn_edit)).setText(barcode.displayValue);
                    addBookBtnNoAuto.setEnabled(true);
                }
            } else {
                Toast.makeText(this.getContext(), String.format(getString(R.string.barcode_error), CommonStatusCodes.getStatusCodeString(resultCode)), Toast.LENGTH_SHORT);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void clearViews() {
        bookTitle.setText("");
        bookAuthor.setText("");
        bookPublisher.setText("");
        bookYear.setText("");
        isbnEdit.setText("");

        LinearLayout wrapper = getView().findViewById(R.id.ab_autofilled_info_wrapper);

        wrapperVisibility = View.GONE;
        wrapper.setVisibility(wrapperVisibility);

        addBookBtn.setEnabled(false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getActivity().setTitle(R.string.add_book);

        if (savedInstanceState == null) {
            isTaskRunning = false;
        } else {
            isTaskRunning = savedInstanceState.getBoolean("isTaskRunning");
        }

        if (isTaskRunning) {
            progressDialog = ProgressDialog.show(getActivity(),
                    getResources().getString(R.string.add_book_isbn_loading_title),
                    getResources().getString(R.string.add_book_isbn_loading_message));
        }
    }

    private void uploadBook() {
        inAutocomplete = getView().findViewById(R.id.ab_autofilled_info_wrapper).isShown();

        if (!inAutocomplete) {
            String isbn = isbnEdit.getText().toString();
            String title = titleEt.getText().toString();
            String author = authorEt.getText().toString();
            String language = languageEt.getText().toString();
            String publisher = publisherEt.getText().toString();
            int year = Integer.parseInt(yearEt.getSelectedItem().toString());

            if (checkMandatoryFieldsInput(title, author)) {
                title = Utilities.trimString(title, titleEt.length());
                author = Utilities.trimString(author, authorEt.length());
                language = Utilities.trimString(language, titleEt.length());
                publisher = Utilities.trimString(publisher, publisherEt.length());

                book = new Book(isbn, title, Arrays.asList(author), language, publisher, year);
            } else {
                Toast.makeText(getContext(), getResources().getString(R.string.add_book_error), Toast.LENGTH_SHORT).show();
                return;
            }

        }

        String condition = ((Spinner) getView().findViewById(R.id.add_book_condition_edit)).getSelectedItem().toString();
        String[] tags = ((TagGroup) getView().findViewById(R.id.tag_group)).getTags();

        book.setConditions(condition);
        book.setTags(Arrays.asList(tags));
        book.saveToFirebase(this.getResources())
                .addOnSuccessListener((v) -> {
                    book = null;
                    Toast.makeText(getContext(), getResources().getString(R.string.add_book_saved), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener((v) -> {
                    Toast.makeText(getContext(), getResources().getString(R.string.add_book_error), Toast.LENGTH_SHORT).show();
                });

    }

    private void findViews(View view) {
        // Buttons
        scanBarcodeBtn = view.findViewById(R.id.ab_barcode_scan);
        addBookBtn = view.findViewById(R.id.ab_add_book);
        startQueryBtn = view.findViewById(R.id.ab_start_query);
        resetBtn = view.findViewById(R.id.ab_clear_fields);
        addBookBtnNoAuto = view.findViewById(R.id.ab_add_book_no_autofill);
        resetBtnNoAuto = view.findViewById(R.id.ab_clear_fields_no_autofill);

        // Isbn view
        isbnEdit = view.findViewById(R.id.ab_isbn_edit);

        // Autofilled views
        bookTitle = view.findViewById(R.id.ab_title);
        bookAuthor = view.findViewById(R.id.ab_author);
        bookPublisher = view.findViewById(R.id.ab_publisher);
        bookYear = view.findViewById(R.id.ab_edition_year);
        bookLanguage = view.findViewById(R.id.ab_language);

        // User-filled views
        titleEt = view.findViewById(R.id.ab_title_edit);
        authorEt = view.findViewById(R.id.ab_author_edit);
        publisherEt = view.findViewById(R.id.ab_publisher_edit);
        languageEt = view.findViewById(R.id.ab_language_edit);
        yearEt = view.findViewById(R.id.ab_edition_year_edit);

        // Tags
        mTagGroup = view.findViewById(R.id.tag_group);

        // Autofill
        autofill = view.findViewById(R.id.ab_autofill);

        // View Switcher
        switcher = view.findViewById(R.id.ab_view_switcher);
    }

    private void fillViewsAutofilled(Book book) {
        bookTitle.setText(book.getTitle());
        bookAuthor.setText(book.getAuthors("\n"));
        bookPublisher.setText(book.getPublisher());
        bookYear.setText(String.format(currentLocale, "%d", book.getYear()));
        bookLanguage.setText(book.getLanguage());
    }

    private void fillSpinnerYear(View view) {
        ArrayList<String> years = new ArrayList<>();
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);

        for (int i = 1900; i <= thisYear; i++)
            years.add(Integer.toString(i));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_item, years);

        Spinner spinYear = (Spinner) view.findViewById(R.id.ab_edition_year_edit);
        spinYear.setAdapter(adapter);
    }

    private boolean checkMandatoryFieldsInput(String title, String author) {
        titleEt.setError(null);
        authorEt.setError(null);

        if (Utilities.isNullOrWhitespace(title)) {
            titleEt.setError(getResources().getString(R.string.ab_field_must_not_be_empty));
            return false;
        }

        if (Utilities.isNullOrWhitespace(author)) {
            authorEt.setError(getResources().getString(R.string.ab_field_must_not_be_empty));
            return false;
        }

        return true;
    }
}
