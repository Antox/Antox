package im.tox.tox4j.impl.jni;

@SuppressWarnings({"checkstyle:emptylineseparator", "checkstyle:linelength"})
public final class ToxCryptoJni {

  static {
    ToxLoadJniLibrary.load("tox4j-c");
  }

  static native byte[] toxPassKeyEncrypt(byte[] data, byte[] passKey);
  static native byte[] toxGetSalt(byte[] data);
  static native boolean toxIsDataEncrypted(byte[] data);
  static native byte[] toxDeriveKeyWithSalt(byte[] passphrase, byte[] salt);
  static native byte[] toxDeriveKeyFromPass(byte[] passphrase);
  static native byte[] toxPassKeyDecrypt(byte[] data, byte[] passKey);
  static native byte[] toxHash(byte[] data);

  public static native void randombytes(byte[] buffer);
  public static native int cryptoBoxKeypair(byte[] publicKey, byte[] secretKey);
  public static native int cryptoBox(byte[] cipherText, byte[] plainText, byte[] nonce, byte[] publicKey, byte[] privateKey);
  public static native int cryptoBoxOpen(byte[] plainText, byte[] cipherText, byte[] nonce, byte[] publicKey, byte[] privateKey);
  public static native int cryptoBoxBeforenm(byte[] sharedKey, byte[] publicKey, byte[] privateKey);
  public static native int cryptoBoxAfternm(byte[] cipherText, byte[] plainText, byte[] nonce, byte[] sharedKey);
  public static native int cryptoBoxOpenAfternm(byte[] plainText, byte[] cipherText, byte[] nonce, byte[] sharedKey);

}
