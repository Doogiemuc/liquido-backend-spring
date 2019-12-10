package org.doogie.liquido.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Handy little utility functions that you <b>always</b> need.
 * See also spring.core.util.*
 * or Google Guava
 */
public class DoogiesUtil {

  public static boolean isEmpty(String s) {
    return s == null || s.trim().length() == 0;
  }

	public static boolean isNotEmpty(String s) { return !isEmpty(s);	}

  //http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
  public static String stream2String(InputStream inputStream) throws IOException {
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
  public static Date daysAgo(long days) {  //BUGFIX: MUST calculate in long!
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

  static final Random rand = new Random();
  /** There is deliberately no number 1(one), and no letters i and j in this array, because they can be interchanged so easily in many fonts. */
  private static final char[] EASY_CHARS = "234567890ABCDEFGHKLMNOPQRSTUVWXYZabcdefghklmnopqrstuvwxyz".toCharArray();

  /**
   * Simply generate some random characters
   * @param len number of chars to generate
   * @return a String of length len with "easy" random characters and numbers
   */
  public static String randString(int len) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < len; i++) {
      buf.append(EASY_CHARS[rand.nextInt(EASY_CHARS.length)]);
    }
    return buf.toString();
  }

  /**
   * Create a string that consists of random digits [0-9] of that length. Can be used as validation token.
   * @param len number of digits to produce
   * @return a string of length "len" that consists of random digits
   */
  public static String randomDigits(int len) {         // Example: len = 3
    long max = (long) Math.pow(len, 10);                // 10^3  = 1000
    long min = (long) Math.pow(len-1, 10);              // 10^2  =  100
    long number = min + (Math.abs(rand.nextLong()) % (max-min));  // 100 + [0...899]  = [100...999]
    return String.valueOf(number);
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
   * convert a byte array into its HEX representation as String
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

	/**
	 * Print a tree structure in a pretty ASCII fromat.
	 *
	 * I LOVE stackoverflow :-)  https://stackoverflow.com/questions/4965335/how-to-print-binary-tree-diagram
	 *
	 * @param node The current node. Pass the root node of your tree in initial call.
	 * @param getChildrenFunc A {@link Function} that returns the children of a given node.
	 * @param <T> The type of your nodes. Anything that has a toString can be used.
	 */
	public static <T> void printTreeRec(T node, Function<T, List<T>> getChildrenFunc) {
		if (getChildrenFunc == null) throw new IllegalArgumentException("need getChildrenFunc to printTreeRec");
		if (node == null) return;
		BiConsumer printer = (prefix, n) -> System.out.println(prefix + n.toString());
		printTreeRec("", node, printer, getChildrenFunc, false);
	}

	/**
	 * Recursively print a tree structure
	 * @param indent the current indendation with branch graphics
	 * @param node the current node (of type T)
	 * @param printer This function is used to print. It will get two parameters: The prefix to print and the current node
	 * @param getChildrenFunc Must return  the list of child nodes for a give node
	 * @param isTail true if node is the last leaf of its parent
	 * @param <T> type of nodes. Nodes may be anything. The only requirement is that the printer function can print the node.
	 */
	public static <T> void printTreeRec(String indent, T node, BiConsumer<String, T> printer, Function<T, List<T>> getChildrenFunc, boolean isTail) {
		String nodeConnection = isTail ? "└─ " : "├─ ";
		printer.accept(indent + nodeConnection, node);		// print the current node with that prefix
		List<T> children = getChildrenFunc.apply(node);
		for (int i = 0; i < children.size(); i++) {
			String newPrefix = indent + (isTail ? "   " : "│  ");
			printTreeRec(newPrefix, children.get(i), printer, getChildrenFunc, i == children.size()-1);
		}
	}

	// We could also apply some FUNCTIONAL programming wizardry: Skip the first parameter "indent".  Instead curry the printer function in each recursion level *G*
}
