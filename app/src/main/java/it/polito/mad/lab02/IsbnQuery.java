package it.polito.mad.lab02;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;

class IsbnQuery extends AsyncTask<String, Object, JSONObject> {

    private final static String baseUrl = "https://www.googleapis.com/books/v1/volumes?q=isbn:";
    private TaskListener mListener;
    private ConnectivityManager mConnectivityManager;

    public IsbnQuery(TaskListener listener) {
        mListener = listener;
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
    protected JSONObject doInBackground(String... isbns) {

        if (isCancelled()) {
            return null;
        }

        String apiUrlString = baseUrl + isbns[0];
        try {
            HttpURLConnection connection;
            try {
                connection = (HttpURLConnection) new URL(apiUrlString).openConnection();
                connection.setRequestMethod("GET");
                connection.setReadTimeout(5000); // 5 seconds
                connection.setConnectTimeout(5000); // 5 seconds
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (ProtocolException e) {
                throw new RuntimeException(e);
            }
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                Log.w(getClass().getName(), apiUrlString + " request failed. Response code: " + responseCode);
                connection.disconnect();
                return null;
            }

            // Read data from response.
            StringBuilder builder = new StringBuilder();
            BufferedReader responseReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = responseReader.readLine();
            while (line != null) {
                builder.append(line);
                line = responseReader.readLine();
            }
            String responseString = builder.toString();
            Log.d(getClass().getName(), "Response String: " + responseString);
            JSONObject responseJson = new JSONObject(responseString);
            // Close connection and return response code.
            connection.disconnect();
            return responseJson;
        } catch (SocketTimeoutException e) {
            Log.w(getClass().getName(), "Connection timed out. Returning null");
            return null;
        } catch (IOException e) {
            Log.d(getClass().getName(), "IOException when connecting to Google Books API.");
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            Log.d(getClass().getName(), "JSONException when connecting to Google Books API.");
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(JSONObject responseJson) {
        mListener.onTaskFinished(responseJson);
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

        void onTaskFinished(JSONObject result);
    }
}

