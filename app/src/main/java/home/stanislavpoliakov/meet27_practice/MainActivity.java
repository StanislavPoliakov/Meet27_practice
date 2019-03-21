package home.stanislavpoliakov.meet27_practice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.paging.PagedList;
import androidx.paging.PositionalDataSource;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.transition.Visibility;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements MainCallback {

    private static final String TAG = "meet27_logs";
    private TextView textView;
    private ProgressBar progressBar;
    private MainHelper helper;
    private int firstId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Проверяем настройки доступа
        initCheckPermissions();

        // Инициализируем прогресс-бар и надпись
        initProgressGroup();

        helper = new MainHelper(this);

        // Подготоваливаем галлерею
        helper.prepareGallery();
    }

    /**
     * Метод инициализации "Paged" RecyclerView
     */
    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        // Инициализируем политику доступа к элементам хранилища
        MyDataSouce dataSource = new MyDataSouce();

        // Подготавливаем параметры списка элементов для отображения
        PagedList.Config config = new PagedList.Config.Builder()
                .setPageSize(3) // Количество элементов в загружаемой пачке
                .setEnablePlaceholders(false) // Не отображать заглушки
                .setInitialLoadSizeHint(5) // Перованчальная загрузка, количество элементов
                .setPrefetchDistance(1) // Как далеко от края списка будем инициировать дальнейшую загрузку
                .build();

        // Собираем сам список
        PagedList<Bitmap> pagedList = new PagedList.Builder<>(dataSource, config)
                .setFetchExecutor(Executors.newCachedThreadPool()) // Работа в многопоточном режиме
                .setNotifyExecutor(new MainThreadExecutor()) // Notify бросать в MainLooper
                .build();

        // Инициализируем "Paged" адаптер
        MyPagingAdapter mAdapter = new MyPagingAdapter(new DiffCallback());

        // Прикручиваем к адаптеру список элементов
        mAdapter.submitList(pagedList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);

        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(layoutManager);
    }

    /**
     * Класс исполнителя UI-потока
     */
    private class MainThreadExecutor implements Executor {
        private Handler mHandler;

        public MainThreadExecutor() {
            mHandler = new Handler(getMainLooper());
        }

        @Override
        public void execute(Runnable command) {
            mHandler.post(command);
        }
    }

    /**
     * Класс, описывающий политику наполнения списка
     */
    private class MyDataSouce extends PositionalDataSource<Bitmap> {

        /**
         * Метод обработки первоначальной загрузки
         * @param params
         * @param callback
         */
        @Override
        public void loadInitial(@NonNull LoadInitialParams params, @NonNull LoadInitialCallback<Bitmap> callback) {
            callback.onResult(loadCollection(params.requestedStartPosition, params.requestedLoadSize), 0);
        }

        /**
         * Метод обработки "рабочих" загрузок
         * @param params
         * @param callback
         */
        @Override
        public void loadRange(@NonNull LoadRangeParams params, @NonNull LoadRangeCallback<Bitmap> callback) {
            callback.onResult(loadCollection(params.startPosition, params.loadSize));
        }
    }

    /**
     * Метод получения данных из галлереи устройства
     * @param start стартовая позиция
     * @param range интервал загрузки
     * @return список элементов
     */
    private List<Bitmap> loadCollection(int start, int range) {

        List<Bitmap> bitmapCollection = new ArrayList<>();

        ContentResolver contentResolver = getContentResolver();

        // Адрес то таблицы элементов в хранилище
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        // Какие столбцы будем выводить
        String[] projection = new String[]{MediaStore.Images.Media._ID};

        // ID первой загруженной картинки
        String startPosition = String.valueOf(firstId + start);

        // ID последней загруженной картинки
        String stopPosition = String.valueOf(firstId + start + range);

        Log.d(TAG, "loadCollection: startPosition = " + startPosition +
                  ", stopPosition = " + stopPosition);

        // Получаем курсор на картинки, ID которых находятся в заданном интервале, включая
        // границы интервала (BETWEEN)
        Cursor cursor = MediaStore.Images.Media.query(contentResolver, uri,
                projection, "_id BETWEEN ? AND ?",
                new String[]{startPosition, stopPosition}, null);


        Log.d(TAG, "loadCollection: cursor.size = " + cursor.getCount());

        // Перемещаем курсор на первую позицию
        cursor.moveToFirst();

        // Проходим весь список до последнего элемента
        while (!cursor.isAfterLast()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(projection[0]));
            Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            try {
                // Получаем картинку по идентификатору ресурса
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                // Сохраняем значения в коллекцию
                bitmapCollection.add(bitmap);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            cursor.moveToNext();
        }

        return bitmapCollection;
    }

    /**
     * Метод инициализации группы UI-элементов в период подготовки галлерии устройства
     * Группа состоит из прогресс-бара и текстовой строки.
     *
     * Напишу здесь примечание к R.Strings: &#8230; - это многоточие!
     */
    private void initProgressGroup() {
        textView = findViewById(R.id.textView);
        progressBar = findViewById(R.id.progressBar);

        setGroupVisibility(View.VISIBLE);

        // Максимум загрузки установлек хардкодом в количество файлов
        progressBar.setMax(20);
    }

    /**
     * Меняем видимость группы загрузки
     * @param visibility значение видимости
     */
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
    }

    @Override
    public void preparingDone(int firstId) {
        setGroupVisibility(View.GONE);
        this.firstId = firstId;
        initRecyclerView();

        Log.d(TAG, "preparingDone: firstId = " + firstId);
    }
}
