package im.tox.antox.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileInputStream;

/**
 * Created by dethstar on 22/06/14.
 */
public class BitMapHelper {

    public static Bitmap decodeSampledBitmapFromFile(FileInputStream res,
                                                         int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        // Decode bitmap with inSampleSize set
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inJustDecodeBounds = false;
        options.inSampleSize = 10;
        return BitmapFactory.decodeStream(res, null, options);
    }
}
