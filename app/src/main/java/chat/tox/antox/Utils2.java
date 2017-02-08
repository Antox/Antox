package chat.tox.antox;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by zoff99 on 21.01.2017.
 */

public class Utils2
{
    public static String getMd5Hash(String input)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String md5 = number.toString(16);

            while (md5.length() < 32)
            {
                md5 = "0" + md5;
            }

            return md5;
        }
        catch (NoSuchAlgorithmException e)
        {
            System.out.println("Utils2:getMd5Hash:" + "Ex1:MD5" + e.getLocalizedMessage());
            return null;
        }
        catch (Exception e)
        {
            System.out.println("Utils2:getMd5Hash:" + "Ex2:MD5" + e.getLocalizedMessage());
            return null;
        }
    }
}
