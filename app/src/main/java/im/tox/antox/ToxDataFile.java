package im.tox.antox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;

/**
 * Class for loading and saving the data file to be used by JTox(byte[] data,
 * ...)
 * 
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
	 * 
	 * @param ctx
	 * @return
	 */
	public boolean doesFileExist() {
		File myFile = new File("/sdcard/" + fileName);
		return myFile.exists();
		  //Do action
		
//		File file = ctx.getFileStreamPath(fileName);
//		return file.exists();
	}

	/**
	 * Method for deleting the tox data file
	 * 
	 * @param ctx
	 */
	public void deleteFile() {
		File file = new File("/sdcard/" + fileName);
		file.delete();
	}

	/**
	 * Method for loading data from a saved file and return it. Requires the
	 * context of the activity or service calling it.
	 * 
	 * @param ctx
	 * @return
	 */
	public byte[] loadFile() {
		FileInputStream fin = null;
		final File file = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath(), fileName);
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
		// File dataFile = ctx.getFileStreamPath(fileName);
		// int sizeOfFile = (int) dataFile.length();
		// byte[] data = new byte[sizeOfFile];
		// try {
		// BufferedInputStream buff = new BufferedInputStream(new
		// FileInputStream(dataFile));
		// buff.read(data, 0, data.length);
		// buff.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		//
		// return data;

	}

	/**
	 * Method for saving tox data. Requires the data to be saved and the context
	 * of the activity or service calling it
	 * 
	 * @param dataToBeSaved
	 * @param ctx
	 */
	public void saveFile(byte[] dataToBeSaved) {
		File myFile = new File("/sdcard/" + fileName);
		try {
			myFile.createNewFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			FileOutputStream output = new FileOutputStream(myFile);
			output.write(dataToBeSaved, 0, dataToBeSaved.length);
			output.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		// try {
		// outputStream = ctx.openFileOutput(fileName, Context.MODE_PRIVATE);
		// outputStream.write(dataToBeSaved);
		// outputStream.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
	}
}
