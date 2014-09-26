package im.tox.antox.tox;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ToxDataFile {

	private String fileName;
    private Context ctx;

	public ToxDataFile(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        ctx = context;
        fileName = preferences.getString("active_account","");
	}

    public ToxDataFile(Context context, String fileName) {
        ctx = context;
        this.fileName = fileName;
    }

	/**
	 * Method to check if the data file exists before attempting to use it
	 * @return
	 */
	public boolean doesFileExist() {
        if (ctx==null) {
            Log.d("ToxDataFile", "Context is null!");
        }
        Log.d("ToxDataFile", "fileName: " + fileName);
		File myFile = ctx.getFileStreamPath(fileName);
        if (myFile == null) {
            Log.d("ToxDataFile", "myFile is null!");
        }
		return myFile.exists();
	}

	/**
	 * Method for deleting the tox data file
	 */
	public void deleteFile() {
		ctx.deleteFile(fileName);
	}

    /**
     * Checks if external storage is available to read
     * @return
     * THIS NEEDS TO BE DONE PROPERLY
     */


	/**
	 * Method for loading data from a saved file and return it. Requires the
	 * context of the activity or service calling it.
	 *
	 * @return
	 */
	public byte[] loadFile() {
		FileInputStream fin = null;
		final File file = ctx.getFileStreamPath(fileName);
		byte[] data = null;
		try {
			fin = new FileInputStream(file);
			data = new byte[(int) file.length()];
			fin.read(data);

		} catch (FileNotFoundException e) {
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fin != null) {
					fin.close();
				}
			} catch (IOException ioe) {
				System.out.println("Error while closing stream: " + ioe);
			}
		}
		return data;
	}

	/**
	 * Method for saving tox data. Requires the data to be saved and the context
	 * of the activity or service calling it
	 * 
	 * @param dataToBeSaved
	 */
	public void saveFile(byte[] dataToBeSaved) {
		File myFile = ctx.getFileStreamPath(fileName);
		try {
			myFile.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			FileOutputStream output = new FileOutputStream(myFile);
			output.write(dataToBeSaved, 0, dataToBeSaved.length);
			output.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
