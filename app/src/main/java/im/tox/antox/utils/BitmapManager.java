package im.tox.antox.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;


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

    private static int calculateSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

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
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), options);
        if(options.outWidth > 0 && options.outHeight > 0)
            return true;
        else
            return false;
    }

    public static void loadBitmap(File file, int id, ImageView imageView) {
        final String imageKey = String.valueOf(id);
        final Bitmap bitmap = getBitmapFromMemCache(imageKey);

        if(bitmap != null) {
            Log.d("BitmapManager", "Found image in cache");
            imageView.setImageBitmap(bitmap);
        } else {
            Log.d("BitmapManager", "Image not in cache");
            // Decode image
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inSampleSize = calculateSampleSize(options, 100,100);
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            final Bitmap bmp = BitmapFactory.decodeFile(file.getPath(), options);
            imageView.setImageBitmap(bmp);
            // Add bitmap to cache
            addBitmapToMemoryCache(imageKey, bmp);
        }

    }
}
