package it.polito.mad.lab02;

import android.app.ProgressDialog;
import android.content.Context;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONObject;

import it.polito.mad.lab02.barcodereader.BarcodeCaptureActivity;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AddBookFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AddBookFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AddBookFragment extends Fragment implements IsbnQuery.TaskListener {

    private static final int RC_BARCODE_CAPTURE = 9001;

    private OnFragmentInteractionListener mListener;

    private ProgressDialog progressDialog;
    private boolean isTaskRunning;
    private IsbnQuery isbnQuery;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AddBookFragment.
     */
    public static AddBookFragment newInstance() {
        AddBookFragment fragment = new AddBookFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            isTaskRunning = false;
        } else {
            isTaskRunning = savedInstanceState.getBoolean("isTaskRunning");
        }

        if (isTaskRunning) {
            progressDialog = ProgressDialog.show(getActivity(), "Loading", "Retrieving ISBN info");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_add_book, container, false);
        Switch autofill = view.findViewById(R.id.add_book_autofill);
        autofill.setOnClickListener((view1) -> {
            ViewSwitcher switcher = view.findViewById(R.id.add_book_view_switcher);
            switcher.showNext();
        });

        ImageButton scanBarcodeBtn = view.findViewById(R.id.add_book_barcode_scan);
        Button startQueryBtn = view.findViewById(R.id.add_book_start_query);
        EditText isbnEdit = view.findViewById(R.id.add_book_isbn_edit);

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

        isbnEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (validateIsbn(isbnEdit.getText().toString())) {
                    isbnEdit.setError(null);
                    startQueryBtn.setEnabled(true);
                } else {
                    isbnEdit.setError("Invalid ISBN");
                    startQueryBtn.setEnabled(false);
                }
            }
        });
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed() {
        if (mListener != null) {
            mListener.onFragmentInteraction();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        mListener = null;
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onDetach();
    }

    @Override
    public void onTaskStarted() {
        isTaskRunning = true;
        progressDialog = ProgressDialog.show(getActivity(), "Loading", "Retrieving ISBN info");
    }

    @Override
    public void onTaskFinished(JSONObject result) {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        isTaskRunning = false;
        if (result != null) {
            LinearLayout wrapper = getView().findViewById(R.id.add_book_autofilled_info_wrapper);
            //TODO: fill textviews with desirialized JSON
            wrapper.setVisibility(View.VISIBLE);
        }
    }

    private boolean validateIsbn(String isbn) {
        if (isbn == null)
            return false;

        //remove any hyphens
        isbn = isbn.replaceAll("-", "");

        try {
            if (isbn.length() == 13) {
                int tot = 0;
                for (int i = 0; i < 12; i++) {
                    int digit = Integer.parseInt(isbn.substring(i, i + 1));
                    tot += (i % 2 == 0) ? digit : digit * 3;
                }

                //checksum must be 0-9. If calculated as 10 then = 0
                int checksum = 10 - (tot % 10);
                if (checksum == 10) {
                    checksum = 0;
                }

                return checksum == Integer.parseInt(isbn.substring(12));

            } else if (isbn.length() == 10) {
                int tot = 0;
                for (int i = 0; i < 9; i++) {
                    int digit = Integer.parseInt(isbn.substring(i, i + 1));
                    tot += ((10 - i) * digit);
                }

                String checksum = Integer.toString((11 - (tot % 11)) % 11);
                if ("10".equals(checksum)) {
                    checksum = "X";
                }

                return checksum.equals(isbn.substring(9));

            } else return false;

        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isTaskRunning", isTaskRunning);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    ((EditText) getView().findViewById(R.id.add_book_isbn_edit)).setText(barcode.displayValue);
                }
            } else {
                Toast.makeText(this.getContext(), String.format(getString(R.string.barcode_error), CommonStatusCodes.getStatusCodeString(resultCode)), Toast.LENGTH_SHORT);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getActivity().setTitle(R.string.add_book);
    }
}
