package it.polito.mad.lab02;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import static android.provider.MediaStore.Images.Media.DATA;
import static android.provider.MediaStore.Video;

class Utilities {

    static void showErrorMessage(Context context, @StringRes int message) {
        new AlertDialog.Builder(context)
                .setMessage(context.getString(message))
                .setPositiveButton(context.getString(android.R.string.ok), null)
                .show();
    }

    static boolean isValidLocation(String s) {
        return s != null && s.matches("((\\p{L}\\p{M}*)|\\p{Zs})+");
    }

    static boolean isNullOrWhitespace(String s) {
        if (s == null)
            return true;

        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    static String trimString(String string) {
        return trimString(string, 0);
    }

    static String trimString(String string, int maxLength) {
        if (string == null) {
            return null;
        }

        string = string.trim().replaceAll("\\p{Zs}+", " ");
        if (maxLength > 0 && string.length() > maxLength) {
            string = string.substring(0, maxLength);
        }
        return string;
    }

    static boolean equals(Object a, Object b) {
        return a == b || (a != null) && (b != null) && a.equals(b);
    }

    static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }

        FileChannel source = new FileInputStream(sourceFile).getChannel();
        FileChannel destination = new FileOutputStream(destFile).getChannel();

        if (source != null) {
            destination.transferFrom(source, 0, source.size());
            source.close();
        }

        destination.close();
    }

    static String getRealPathFromURI(@NonNull Activity activity, @NonNull Uri contentUri) {
        String[] proj = {Video.Media.DATA};
        Cursor cursor = activity.managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private static Bitmap rotateImage(@NonNull Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    static Bitmap loadImage(String imagePath, int targetWidth, int targetHeight,
                            @NonNull Resources resources, @DrawableRes int defaultDrawable) {
        ExifInterface ei = null;
        Bitmap bitmap = null;

        if (imagePath != null) {
            try {
                bitmap = Utilities.getImage(imagePath, targetWidth, targetHeight);
                ei = new ExifInterface(imagePath);
            } catch (IOException e) {
                bitmap = null;
            }
        }

        // Use the default image
        if (bitmap == null) {
            return BitmapFactory.decodeResource(resources, defaultDrawable);
        }

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);
        switch (orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(bitmap, 90);

            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(bitmap, 180);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(bitmap, 270);

            case ExifInterface.ORIENTATION_NORMAL:
            default:
                return bitmap;
        }
    }

    private static Bitmap getImage(String imagePath, int targetWidth, int targetHeight) {

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = 0;
        if (targetWidth != 0 && targetHeight != 0) {
            scaleFactor = Math.min(photoW / targetWidth, photoH / targetHeight);
        }

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        return BitmapFactory.decodeFile(imagePath, bmOptions);
    }

    interface TextWatcherValidator {
        boolean isValid(String string);
    }

    static class GenericTextWatcher implements TextWatcher {

        private final EditText textField;
        private final String errorMessage;
        private final TextWatcherValidator validator;

        GenericTextWatcher(@NonNull EditText textField, @NonNull String errorMessage,
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

    static class GenericTextWatcherEmptyOrInvalid implements TextWatcher {

        private final EditText textField;
        private final String emptyErrorMessage, wrongErrorMessage;
        private final TextWatcherValidator emptyInputValidator, wrongInputValidator;

        GenericTextWatcherEmptyOrInvalid(@NonNull EditText textField, @NonNull String emptyErrorMessage, @NonNull String wrongErrorMessage,
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
