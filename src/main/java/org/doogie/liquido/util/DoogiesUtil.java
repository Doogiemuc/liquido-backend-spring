package org.doogie.liquido.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;

public class DoogiesUtil {

  public static boolean isEmpty(String s) {
    return s == null || s.trim().length() == 0;
  }

  //http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
  public static String _stream2String(InputStream inputStream) throws IOException {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length;
    while ((length = inputStream.read(buffer)) != -1) {
      result.write(buffer, 0, length);
    }
    try {
      return result.toString("UTF-8");
    } catch (UnsupportedEncodingException e) {
      //You should really know "UTF-8" :-)
      return "";  // yeah ... i know
    }
  }

  /** @return a Data n days ago */
  public static Date daysAgo(int days) {
    return new Date(System.currentTimeMillis() - days * 3600*24*1000);
  }

  /** add or subtract n days from the given date */
  public static Date addDays(Date date, int days)
  {
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.add(Calendar.DATE, days); //minus number would decrement the days
    return cal.getTime();
  }



  private static ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

  /**
   * convert a long to byte array.
   * (Used for hashing with MessageDigest.)
   * @param x any long value
   * @return the long as a byte array
   */
  public static byte[] longToBytes(long x) {
    buffer.putLong(0, x);
    return buffer.array();
  }

  /**
   * convert a byte array into its HEY representation as String
   * @param byteData
   * @return
   */
  public static String bytesToString(byte[] byteData) {
    //convert the byte to hex format method 2
    StringBuffer hexString = new StringBuffer();
    for (int i=0;i<byteData.length;i++) {
      String hex=Integer.toHexString(0xff & byteData[i]);
      if(hex.length()==1) hexString.append('0');
      hexString.append(hex);
    }
    return hexString.toString();
  }
}
