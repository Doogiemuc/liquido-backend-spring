package org.doogie.liquido.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

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
}
