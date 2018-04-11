package it.polito.mad.lab02.utils;

import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class TextWatcherUtilities {

    public interface TextWatcherValidator {
        boolean isValid(String string);
    }

    public static class GenericTextWatcher implements TextWatcher {

        private final EditText textField;
        private final String errorMessage;
        private final TextWatcherValidator validator;

        public GenericTextWatcher(@NonNull EditText textField, @NonNull String errorMessage,
                                  @NonNull TextWatcherValidator validator) {
            this.textField = textField;
            this.errorMessage = errorMessage;
            this.validator = validator;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (!validator.isValid(editable.toString())) {
                textField.setError(errorMessage);
            }
        }
    }

    public static class GenericTextWatcherEmptyOrInvalid implements TextWatcher {

        private final EditText textField;
        private final String emptyErrorMessage, wrongErrorMessage;
        private final TextWatcherValidator emptyInputValidator, wrongInputValidator;

        public GenericTextWatcherEmptyOrInvalid(@NonNull EditText textField, @NonNull String emptyErrorMessage, @NonNull String wrongErrorMessage,
                                                @NonNull TextWatcherValidator emptyInputValidator, @NonNull TextWatcherValidator wrongInputValidator) {
            this.textField = textField;
            this.emptyErrorMessage = emptyErrorMessage;
            this.wrongErrorMessage = wrongErrorMessage;
            this.emptyInputValidator = emptyInputValidator;
            this.wrongInputValidator = wrongInputValidator;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {

            if (!emptyInputValidator.isValid(editable.toString())) {
                textField.setError(emptyErrorMessage);
            } else if (!wrongInputValidator.isValid(editable.toString())) {
                textField.setError(wrongErrorMessage);
            }
        }
    }
}
