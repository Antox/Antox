package im.tox.antox;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Class for loading and saving the data file to be used by JTox(byte[] data, ...)
 * @author Mark Winter (astonex)
 */
public class ToxDataFile {

    /**
     * Other clients use the file name data so we shall as well
     */
    private String fileName = "data";

    /* Some variables for debgging */
    private byte[] dataContent;
    private String dataContentString;

    public ToxDataFile() {
    }

    /**
     * Method to check if the data file exists before attempting to use it
     * @param ctx
     * @return
     */
    public boolean doesFileExist(Context ctx) {
        File file = ctx.getFileStreamPath(fileName);
        return file.exists();
    }

    /**
     * Method for deleting the tox data file
     * @param ctx
     */
    public void deleteFile(Context ctx) {
        File file = ctx.getFileStreamPath(fileName);
        file.delete();
    }

    /**
     * Method for loading data from a saved file and return it. Requires the context of the activity
     * or service calling it.
     * @param ctx
     * @return
     */
    public byte[] loadFile(Context ctx) {
        File dataFile = ctx.getFileStreamPath(fileName);
        int sizeOfFile = (int) dataFile.length();
        byte[] data = new byte[sizeOfFile];
        try {
            BufferedInputStream buff = new BufferedInputStream(new FileInputStream(dataFile));
            buff.read(data, 0, data.length);
            buff.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    /**
     * Method for saving tox data.
     * Requires the data to be saved and the context of the activity or service calling it
     * @param dataToBeSaved
     * @param ctx
     */
    public void saveFile(byte[] dataToBeSaved, Context ctx) {
        FileOutputStream outputStream;

        try {
            outputStream = ctx.openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(dataToBeSaved);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
