package it.polito.mad.lab02.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import static android.provider.MediaStore.Images.Media.DATA;
import static android.provider.MediaStore.Video;

public class Utilities {

    public static Dialog openErrorDialog(Context context, @StringRes int message) {
        return new AlertDialog.Builder(context)
                .setMessage(context.getString(message))
                .setPositiveButton(context.getString(android.R.string.ok), null)
                .show();
    }

    public static boolean isValidLocation(String s) {
        return s != null && s.matches("((\\p{L}\\p{M}*)|\\p{Zs})+");
    }

    public static boolean isNullOrWhitespace(String s) {
        if (s == null)
            return true;

        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String trimString(String string) {
        return trimString(string, 0);
    }

    public static String trimString(String string, int maxLength) {
        if (string == null) {
            return null;
        }

        string = string.trim().replaceAll("\\p{Zs}+", " ");
        if (maxLength > 0 && string.length() > maxLength) {
            string = string.substring(0, maxLength);
        }
        return string;
    }

    public static boolean equals(Object a, Object b) {
        return a == b || (a != null) && (b != null) && a.equals(b);
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
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

    public static String getRealPathFromURI(@NonNull Activity activity, @NonNull Uri contentUri) {
        String[] proj = {Video.Media.DATA};
        Cursor cursor = activity.managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
}
