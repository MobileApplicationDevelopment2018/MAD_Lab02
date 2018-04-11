package it.polito.mad.lab02.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.support.annotation.DrawableRes;
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

    private static Bitmap rotateImage(@NonNull Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    public static Bitmap loadImage(String imagePath, int targetWidth, int targetHeight,
                                   @NonNull Resources resources, @DrawableRes int defaultDrawable) {
        ExifInterface ei = null;
        Bitmap bitmap = null;

        if (imagePath != null) {
            try {
                bitmap = PictureUtilities.getImage(imagePath, targetWidth, targetHeight);
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
}
