package im.tox.antox;

import android.Manifest;
import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
        byte[] data = null;

        try {
            FileInputStream inputStream = ctx.openFileInput(fileName);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }

            data = stringBuilder.toString().getBytes();

        } catch (IOException e) {
            e.printStackTrace();
        }

        /* For debugging */
        dataContent = data;
        dataContentString = data.toString();

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
