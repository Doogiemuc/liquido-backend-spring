package org.doogie.liquido.util;

import org.springframework.http.HttpMethod;
import org.springframework.util.StreamUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This is similar to {@link org.springframework.web.util.ContentCachingRequestWrapper}
 * but my wrapper can read the requests body and POST request parameters
 * BEFORE the request is sent.
 */
public class BufferedRequestWrapper extends HttpServletRequestWrapper {

	private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
	BufferedServletInputStream bufIs;

	/**
	 * Copy the request body into a cache.
	 * If this is a POST request, then write RequestParameters to that cache (once)
	 * @param request
	 * @throws IOException
	 */
	public BufferedRequestWrapper(HttpServletRequest request) throws IOException {
		super(request);
		bufIs = new BufferedServletInputStream(request.getInputStream());
		if (request.getContentType() != null && request.getContentType().contains(FORM_CONTENT_TYPE) &&
				HttpMethod.POST.matches(request.getMethod()) &&
				bufIs.getCachedContent().length == 0)
		{
			writeRequestParametersToCachedContent();
		}
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return bufIs;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new InputStreamReader(bufIs));
	}

	/**
	 * Get the cached content. You can call this even before sending the request
	 * @return the cached request body (including POST parameters)
	 */
	public byte[] getBufferedContent() {
		return bufIs.getCachedContent();
	}

	/**
	 * If this a POST request with content-type: application/x-www-form-urlencoded
	 * then write the form parameters to the cached content (once only).
	 */
	private void writeRequestParametersToCachedContent() {
		try {
			if (bufIs.getCachedContent().length == 0) {
				String requestEncoding = getCharacterEncoding();
				Map<String, String[]> form = super.getParameterMap();
				for (Iterator<String> nameIterator = form.keySet().iterator(); nameIterator.hasNext(); ) {
					String name = nameIterator.next();
					List<String> values = Arrays.asList(form.get(name));
					for (Iterator<String> valueIterator = values.iterator(); valueIterator.hasNext(); ) {
						String value = valueIterator.next();
						bufIs.writeToCachedContent(URLEncoder.encode(name, requestEncoding).getBytes());
						if (value != null) {
							bufIs.writeToCachedContent("=".getBytes());
							bufIs.writeToCachedContent(URLEncoder.encode(value, requestEncoding).getBytes());
							if (valueIterator.hasNext()) {
								bufIs.writeToCachedContent("&".getBytes());
							}
						}
					}
					if (nameIterator.hasNext()) {
						bufIs.writeToCachedContent("&".getBytes());
					}
				}
			}
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to write request parameters to cached content", ex);
		}
	}

	private class BufferedServletInputStream extends ServletInputStream {
		ByteArrayInputStream cachedInputStream = null;
		byte[] cachedContent;

		public BufferedServletInputStream(InputStream is) throws IOException {
			super();
			if (is.available() == 0) {
				//System.out.println("skipping empty InputStream");
				this.cachedContent = new byte[0];
				return;
			}
			// copy from InputStream into cachedContent
			this.cachedContent = StreamUtils.copyToByteArray(is);
			// builder a new InputStream from that ByteArray that we can read from.
			this.cachedInputStream = new ByteArrayInputStream(this.cachedContent);
		}

		public byte[] getCachedContent() {
			return this.cachedContent;
		}

		public void writeToCachedContent(byte[] bytes) {
			if (cachedContent == null) cachedContent = new byte[0];
			byte[] newArray = new byte[cachedContent.length+bytes.length];
			System.arraycopy(this.cachedContent, 0, newArray, 0, this.cachedContent.length);
			System.arraycopy(bytes, 0, newArray, this.cachedContent.length, bytes.length);
			this.cachedContent = newArray;
		}

		@Override
		public boolean isFinished() {
			return cachedInputStream == null || cachedInputStream.available() == 0;
		}

		@Override
		public boolean isReady() {
			return false;
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			throw new UnsupportedOperationException("I do not support a ReadListener");
		}

		@Override
		public int read() {
			if (cachedInputStream == null) {
				return -1;
			}
			int ch = cachedInputStream.read();
			return ch;
		}
	}
}

