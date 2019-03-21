package home.stanislavpoliakov.meet27_practice;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.transition.Visibility;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements MainCallback{
    private static final String TAG = "meet27_logs";
    private TextView textView;
    private ProgressBar progressBar;
    private MainHelper helper;
    int progress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initCheckPermissions();
        initProgressGroup();

        helper = new MainHelper(this);
        helper.prepareGallery();
    }

    private void initProgressGroup() {
        textView = findViewById(R.id.textView);
        progressBar = findViewById(R.id.progressBar);

        setGroupVisibility(View.VISIBLE);

        progressBar.setMax(20);
        progress = 0;
    }

    private void setGroupVisibility(int visibility) {
        textView.setVisibility(visibility);
        progressBar.setVisibility(visibility);
    }

    private void initCheckPermissions() {
        int check = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (check == -1) requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        check = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (check == -1) requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    @Override
    public void updateProgress(int progress) {
        progressBar.setProgress(progress, true);
        //Log.d(TAG, "updateProgress: Thread = " + Thread.currentThread());
    }

    @Override
    public void preparingDone() {
        setGroupVisibility(View.GONE);
    }
}
