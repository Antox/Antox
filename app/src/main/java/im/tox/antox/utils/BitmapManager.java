package im.tox.antox.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;


public final class BitmapManager {

    private static LruCache<String, Bitmap> mMemoryCache;

    public BitmapManager() {
        // Get max memory
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Adjust this to fit needs - approx a min of 4MB
        final int cacheSize = maxMemory / 8;
        // Set cache size
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            // Measure cache size in kb rather than number of items
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    private static Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    private static void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if(getBitmapFromMemCache(key) == null)
            mMemoryCache.put(key, bitmap);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static boolean checkValidImage(File file) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
            byte[] byteArr = decodeBytes(fis);
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(byteArr, 0, byteArr.length, options);
            if(options.outWidth > 0 && options.outHeight > 0)
                return true;
            else
                return false;
        } catch (FileNotFoundException e) {
            Log.d("BitMapManager", "File not found when trying to be used for FileInputStream in checkValidImage");
            e.printStackTrace();
            return false;
        }
    }

    private static byte[] decodeBytes(InputStream inputStream) {
        byte[] byteArr = new byte[0];
        byte[] buffer = new byte[1024];
        int len;
        int count = 0;

        try {
            while ((len = inputStream.read(buffer)) > -1) {
                if (len != 0) {
                    if (count + len > byteArr.length) {
                        byte[] newbuf = new byte[(count + len) * 2];
                        System.arraycopy(byteArr, 0, newbuf, 0, count);
                        byteArr = newbuf;
                    }

                    System.arraycopy(buffer, 0, byteArr, count, len);
                    count += len;
                }
            }

            return byteArr;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void loadBitmap(File file, int id, ImageView imageView) {
        final String imageKey = String.valueOf(id);
        Bitmap bitmap = getBitmapFromMemCache(imageKey);

        if(bitmap != null) {
            Log.d("BitmapManager", "Found image in cache");
            imageView.setImageBitmap(bitmap);
        } else {
            Log.d("BitmapManager", "Image not in cache");
            // Read file into byte array and decode that
            FileInputStream fis;
            try {
                fis = new FileInputStream(file);
                byte [] byteArr = decodeBytes(fis);
                // Decode bounds and calculate sample size
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                bitmap = BitmapFactory.decodeByteArray(byteArr, 0 , byteArr.length, options);
                options.inSampleSize = calculateInSampleSize(options, 100, 100);
                // Deocde image and set it
                options.inPurgeable = true;
                options.inInputShareable = true;
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                bitmap = BitmapFactory.decodeByteArray(byteArr, 0, byteArr.length, options);
                imageView.setImageBitmap(bitmap);
                // Add bitmap to cache
                addBitmapToMemoryCache(imageKey, bitmap);
            } catch(FileNotFoundException e) {
                Log.d("BitMapManager", "File not found when trying to be used for FileInputStream");
                e.printStackTrace();
            }

        }
    }
}
