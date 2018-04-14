package it.polito.mad.lab02.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.support.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PictureUtilities {

    public static ByteArrayOutputStream compressImage(@NonNull String imagePath, int targetWidth, int targetHeight, int quality) {

        if (targetWidth <= 0 || targetHeight <= 0 || quality < 0 || quality > 100) {
            throw new IllegalArgumentException();
        }

        Bitmap image = PictureUtilities.getImage(imagePath, targetWidth, targetHeight);
        if (image == null) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        return image.compress(Bitmap.CompressFormat.WEBP, quality, out) ? out : null;
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

        Bitmap image = BitmapFactory.decodeFile(imagePath, bmOptions);
        if (image == null) {
            return null;
        }

        return rotateImage(image, getRotation(imagePath));
    }

    private static int getRotation(@NonNull String imagePath) {
        ExifInterface exifInterface = null;

        try {
            exifInterface = new ExifInterface(imagePath);
        } catch (IOException e) {
            return 0;
        }


        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);
        switch (orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;

            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;

            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;

            case ExifInterface.ORIENTATION_NORMAL:
            default:
                return 0;
        }
    }

    private static Bitmap rotateImage(@NonNull Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }
}
