package it.polito.mad.lab02.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.books.Books;
import com.google.api.services.books.Books.Volumes.List;
import com.google.api.services.books.model.Volumes;

import java.io.IOException;

public class IsbnQuery extends AsyncTask<String, Object, Volumes> {

    private TaskListener mListener;
    private ConnectivityManager mConnectivityManager;

    public IsbnQuery(TaskListener listener) {
        mListener = listener;
    }

    @Override
    protected Volumes doInBackground(String... isbns) {

        if (isCancelled())
            return null;

        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        final Books booksClient;
        booksClient = new Books.Builder(new ApacheHttpTransport(), jsonFactory, null).build();

        List volumesList;
        Volumes volumes;
        try {
            volumesList = booksClient.volumes().list("isbn:" + isbns[0]);
            // Execute the query.
            volumes = volumesList.execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return volumes;

    }

    @Override
    protected void onPreExecute() {

        mListener.onTaskStarted();
        if (!isNetworkConnected()) {
            Log.i(getClass().getName(), "No internet connection");
            cancel(true);
        }
    }

    @Override
    protected void onPostExecute(Volumes volumes) {
        mListener.onTaskFinished(volumes);
    }

    private boolean isNetworkConnected() {

        if (mConnectivityManager == null)
            mConnectivityManager = (ConnectivityManager) ((Fragment) mListener).getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        assert mConnectivityManager != null;
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public interface TaskListener {
        void onTaskStarted();

        void onTaskFinished(Volumes result);
    }
}

