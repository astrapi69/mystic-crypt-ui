/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.net.url;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

/**
 * The class {@linkplain URLExtensions} provides utility methods for {@link URL}.
 */
public class URLExtensions
{

	/**
	 * Gets the filename from the given url object.
	 * 
	 * @param url
	 *            the url
	 * @return the filename
	 * @throws UnsupportedEncodingException
	 *             the unsupported encoding exception
	 */
	public static String getFilename(final URL url) throws UnsupportedEncodingException
	{
		if (isJar(url) || isEar(url))
		{
			String fileName = URLDecoder.decode(url.getFile(), "UTF-8");
			fileName = fileName.substring(5, fileName.indexOf("!"));
			return fileName;
		}
		return URLDecoder.decode(url.getFile(), "UTF-8");
	}

	/**
	 * Checks if is ear.
	 * 
	 * @param url
	 *            the url
	 * @return true, if is ear
	 */
	public static boolean isEar(final URL url)
	{
		return url.getProtocol().equals(Protocol.EAR.getProtocol());
	}

	/**
	 * Checks if is jar.
	 * 
	 * @param url
	 *            the url
	 * @return true, if is jar
	 */
	public static boolean isJar(final URL url)
	{
		return url.getProtocol().equals(Protocol.JAR.getProtocol());
	}

	/**
	 * Checks if is war.
	 * 
	 * @param url
	 *            the url
	 * @return true, if is ear
	 */
	public static boolean isWar(final URL url)
	{
		return url.getProtocol().equals(Protocol.WAR.getProtocol());
	}

	/**
	 * Checks if the given {@link URL} is reachable
	 *
	 * @param url
	 *            the url
	 * @return true, if the given {@link URL} is reachable
	 */
	public static boolean isReachable(URL url) {
		try {
			final URLConnection conn = url.openConnection();
			conn.connect();
			conn.getInputStream().close();
		} catch (MalformedURLException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		return true;
	}

}
