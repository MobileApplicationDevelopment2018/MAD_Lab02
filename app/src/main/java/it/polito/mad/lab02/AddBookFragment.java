package it.polito.mad.lab02;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.api.services.books.model.Volume;
import com.google.api.services.books.model.Volumes;

import java.util.Arrays;
import java.util.Locale;

import it.polito.mad.lab02.barcodereader.BarcodeCaptureActivity;
import it.polito.mad.lab02.data.Book;
import it.polito.mad.lab02.utils.IsbnQuery;
import it.polito.mad.lab02.utils.Utilities;

public class AddBookFragment extends Fragment implements IsbnQuery.TaskListener {

    private static final int RC_BARCODE_CAPTURE = 9001;

    private ProgressDialog progressDialog;
    private boolean isTaskRunning;
    private IsbnQuery isbnQuery;
    private int wrapperVisibility;

    private Book book;
    private boolean inAutocomplete;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_add_book, container, false);

        if (savedInstanceState == null) {
            wrapperVisibility = View.GONE;
        } else {
            wrapperVisibility = savedInstanceState.getInt("wrapperVisibility");
            view.findViewById(R.id.ab_add_book).setEnabled(true);
            view.findViewById(R.id.ab_clear_fields).setEnabled(true);
        }

        LinearLayout wrapper = view.findViewById(R.id.ab_autofilled_info_wrapper);
        wrapper.setVisibility(wrapperVisibility);

        ViewSwitcher switcher = view.findViewById(R.id.ab_view_switcher);

        Switch autofill = view.findViewById(R.id.ab_autofill);
        autofill.setOnCheckedChangeListener((buttonView, isChecked) -> {
            switcher.showNext();
            View currentView = switcher.getCurrentView();
            if (currentView.getId() == R.id.ab_autofill_book_view && wrapperVisibility == View.VISIBLE) {
                view.findViewById(R.id.ab_add_book).setEnabled(true);
                view.findViewById(R.id.ab_clear_fields).setEnabled(true);
            } else {
                view.findViewById(R.id.ab_add_book).setEnabled(false);
                view.findViewById(R.id.ab_clear_fields).setEnabled(false);
            }
        });

        ImageButton scanBarcodeBtn = view.findViewById(R.id.ab_barcode_scan);
        Button addBookBtn = view.findViewById(R.id.ab_add_book);
        Button startQueryBtn = view.findViewById(R.id.ab_start_query);
        Button resetBtn = view.findViewById(R.id.ab_clear_fields);
        EditText isbnEdit = view.findViewById(R.id.ab_isbn_edit);

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
                } else {
                    if (isbnEdit.getText().length() != 0)
                        isbnEdit.setError(getString(R.string.add_book_invalid_isbn));
                    startQueryBtn.setEnabled(false);
                }
            }
        });

        addBookBtn.setOnClickListener(v -> uploadBook());
        addBookBtn.setClickable(true);

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
            Toast.makeText(getContext(), "Query completed successfully", Toast.LENGTH_SHORT).show();

            LinearLayout wrapper = getView().findViewById(R.id.ab_autofilled_info_wrapper);
            final Volume.VolumeInfo volumeInfo = volumes.getItems().get(0).getVolumeInfo();

            EditText isbnEdit = getView().findViewById(R.id.ab_isbn_edit);
            book = new Book(isbnEdit.getText().toString(), volumeInfo);

            TextView bookTitle = getView().findViewById(R.id.ab_title);
            TextView bookAuthor = getView().findViewById(R.id.ab_author);
            TextView bookPublisher = getView().findViewById(R.id.ab_publisher);
            TextView bookYear = getView().findViewById(R.id.ab_edition_year);

            bookTitle.setText(book.getTitle());
            bookAuthor.setText(book.getAuthors("\n"));
            bookPublisher.setText(book.getPublisher());

            Locale currentLocale = getResources().getConfiguration().locale;
            bookYear.setText(String.format(currentLocale, "%d", book.getYear()));

            wrapperVisibility = View.VISIBLE;
            wrapper.setVisibility(View.VISIBLE);

            getView().findViewById(R.id.ab_add_book).setEnabled(true);
            getView().findViewById(R.id.ab_clear_fields).setEnabled(true);
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
                }
            } else {
                Toast.makeText(this.getContext(), String.format(getString(R.string.barcode_error), CommonStatusCodes.getStatusCodeString(resultCode)), Toast.LENGTH_SHORT);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void clearViews() {
        TextView bookTitle = getView().findViewById(R.id.ab_title);
        TextView bookAuthor = getView().findViewById(R.id.ab_author);
        TextView bookPublisher = getView().findViewById(R.id.ab_publisher);
        TextView bookYear = getView().findViewById(R.id.ab_edition_year);

        bookTitle.setText("");
        bookAuthor.setText("");
        bookPublisher.setText("");
        bookYear.setText("");

        ((EditText) getView().findViewById(R.id.ab_isbn_edit)).setText("");

        LinearLayout wrapper = getView().findViewById(R.id.ab_autofilled_info_wrapper);

        wrapperVisibility = View.GONE;
        wrapper.setVisibility(wrapperVisibility);

        getView().findViewById(R.id.ab_add_book).setEnabled(false);
        getView().findViewById(R.id.ab_clear_fields).setEnabled(false);
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

        // TODO settarlo nel punto opportuno
        inAutocomplete = book != null;

        if (!inAutocomplete) {

            EditText isbnEt = getView().findViewById(R.id.ab_isbn_edit);
            EditText titleEt = getView().findViewById(R.id.ab_title_edit);
            EditText authorEt = getView().findViewById(R.id.ab_author_edit);
            EditText publisherEt = getView().findViewById(R.id.ab_publisher_edit);
            EditText yearEt = getView().findViewById(R.id.ab_edition_year_edit);

            String isbn = isbnEt.getText().toString();
            String title = titleEt.getText().toString();
            String author = authorEt.getText().toString();
            String language = "TO INSERT";
            String publisher = publisherEt.getText().toString();
            int year = Integer.parseInt(yearEt.getText().toString());

            // TODO perform checks

            book = new Book(isbn, title, Arrays.asList(author), language, publisher, year);
        }

        // TODO: load conditions and tags

        book.saveToFirebase(this.getResources())
                .addOnSuccessListener((v) -> {
                    book = null;
                    Toast.makeText(getContext(), "SAVED", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener((v) -> {
                    Log.e("tag", v.getMessage());
                    Toast.makeText(getContext(), "ERROR", Toast.LENGTH_SHORT).show();
                });

    }
}
