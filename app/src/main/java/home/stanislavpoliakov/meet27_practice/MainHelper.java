package home.stanislavpoliakov.meet27_practice;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Класс инициализации начальных значений
 * Здесь мы определяем список адресов картинок, формируем Url и загружаем данные
 */
public class MainHelper {
    private static final String TAG = "meet27_logs";
    private Context context;
    private MainCallback mActivity;


    //private Set<String> resourceSet;

    public MainHelper(Context context) {
        this.context = context;

        try {
            mActivity = (MainCallback) context;
        } catch (ClassCastException ex) {
            Log.w(TAG, "Main Activity must iplement MainCallback interface", ex);
        }
    }

    private class DownloadAndSaveTask extends AsyncTask<Void, Integer, Void> {
        private int progress;

        @Override
        protected void onPreExecute() {
            initResourceSet();
            progress = 0;
        }

        /**
         * Метод асинхронной работы
         * @param voids ничего не принимает
         * @return результат работы
         */
        @Override @WorkerThread
        protected Void doInBackground(Void... voids) {
            // Очищаем галлерею, чтобы не переполнять устройство загрузкой. Поскольку работаем на
            // эмуляторе - очищаем всю. Конечно, на реальном устройстве очистка была бы выборочной
            clearStorage();

            // Получаем Stream коллекции и сохраняем каждый элемент в галлерее
            //getCollection().forEach(MainHelper.this::saveBitmap);

            initResourceSet().stream().parallel()
                    .map(MainHelper.this::stringToUrl)
                    .map(MainHelper.this::getBitmap)
                    .peek(MainHelper.this::saveBitmap)
                    .forEach(b -> publishProgress(1));

            //Log.d(TAG, "doInBackground: Thread = " + Thread.currentThread());
            // Загружаем данные из галлереи и возращаем
            //return loadCollection();
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            //super.onProgressUpdate(values);
            progress += values[0];
            //Log.d(TAG, "onProgressUpdate: Thread = " + Thread.currentThread());
            mActivity.updateProgress(progress);
        }

        /**
         * После окончания раоты прячем ProgressBar и инициализируем переменные
         */
        @Override @UiThread
        protected void onPostExecute(Void aVoid) {
            mActivity.preparingDone();
            //setGroupVisibility(View.GONE);

            // Сохраняем результат работы
            /*data = bitmaps;

            // Инициализируем адаптер RecyclerView коллекцией изображений
            RecyclerViewAdapter mAdapter = new RecyclerViewAdapter(MainActivity.this,
                    new ArrayList<>(data.keySet()));

            // Отображать будем сеткой в 4 колонки
            GridLayoutManager layoutManager =
                    new GridLayoutManager(MainActivity.this, 4,
                            GridLayoutManager.VERTICAL, false);

            recyclerView.setAdapter(mAdapter);
            recyclerView.setLayoutManager(layoutManager);*/
        }
    }

    private void clearStorage() {

        // Определеяем инструмент доступа к внешнему контенту (или контенту контент-провайдера)
        ContentResolver contentResolver = context.getContentResolver();

        // Uri таблицы данных = content://media/external/images/media
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        // Проекция - это идентификатор ресурса в обобщенном виде
        // То есть для наших картинок - это 2423, 2424, 2425... и так далее. То есть то значение,
        // которое определяет наш конечный ресурс. Проекция определяет это значение, как переменную.
        String[] projection = new String[] {MediaStore.Images.Media._ID};

        // Получаем Cursor на все (where = null) данные из таблицы "content://media/external/images/media".
        // Переменная ресурсов = _id
        Cursor cursor = MediaStore.Images.Media.query(contentResolver, uri, projection, null, null);

        // Перемещаемся к первому элементу найденных значений
        cursor.moveToFirst();

        // Пока не прошли все значения... т.е. курсор не указывает за последний элемент (как Iterator)
        while (!cursor.isAfterLast()) {

            // Получаем ID изображения, считанное из единственной колонки с наименованием "_id"
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(projection[0]));

            // Получаем Uri ресурса, который необходимо удалить, путем "склеивания" путей
            // "content://media/external/images/media" + "/" + "2324"
            Uri deleteUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

            // Удаляем ресурс из области контента
            contentResolver.delete(deleteUri, null, null);

            // Перемещаемся к следующему элементу
            cursor.moveToNext();
        }

        // Не забываем закрвать курсор
        cursor.close();
    }

    /**
     * Метод сохранения загруженной картинки в формате Bitmap в галлерею устройства
     * @param bitmap загруженная картинка
     */
    private void saveBitmap(Bitmap bitmap) {
        MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap,
                "Bitmap", "Bitmap image");
    }

    /**
     * Основной метод для получения коллекции картинок
     * @return коллекция картинок, загруженных из сети
     */
    public void prepareGallery() {
        DownloadAndSaveTask task = new DownloadAndSaveTask();
        task.execute();
    }

    /**
     * Метод инициализации множества адресов.
     * На данном этапе адреса хранятся в строках (скопированных из адресной строки браузера)
     */
    private Set<String> initResourceSet() {
        Set<String> resourceSet = new HashSet<>();
        resourceSet.add("https://images.pexels.com/photos/443446/pexels-photo-443446.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=650&w=940");
        resourceSet.add("https://images.pexels.com/photos/8633/nature-tree-green-pine.jpg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/326055/pexels-photo-326055.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/462118/pexels-photo-462118.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/709552/pexels-photo-709552.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/589840/pexels-photo-589840.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/490411/pexels-photo-490411.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/1363876/pexels-photo-1363876.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/50594/sea-bay-waterfront-beach-50594.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/132037/pexels-photo-132037.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/1562/italian-landscape-mountains-nature.jpg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/46253/mt-fuji-sea-of-clouds-sunrise-46253.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/40896/larch-conifer-cone-branch-tree-40896.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/5969/wood-nature-forest-bridge.jpg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/355296/pexels-photo-355296.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/1300510/pexels-photo-1300510.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/547119/pexels-photo-547119.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/70741/cereals-field-ripe-poppy-70741.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/132419/pexels-photo-132419.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        resourceSet.add("https://images.pexels.com/photos/35627/shells-massage-therapy-sand.jpg?auto=compress&cs=tinysrgb&dpr=2&w=500");
        return resourceSet;
    }

    /**
     * Метод конвертации адреса в формате String в адрес в формате URL
     * @param stringUrl адрес в формате String
     * @return адрес в формате URL
     */
    private URL stringToUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
        return url;
    }

    /**
     * Метод загрузки картинки из сети по данному url
     * @param url адрес картинки
     * @return картинка в формате Bitmap
     */
    private Bitmap getBitmap(URL url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            int responseCode = connection.getResponseCode();

            //200 - OK
            if (responseCode == 200) {
                return BitmapFactory.decodeStream(connection.getInputStream());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            // Закрываем открытое соединение в блоке finally
            if (connection != null) connection.disconnect();
        }
        return null;
    }
}
