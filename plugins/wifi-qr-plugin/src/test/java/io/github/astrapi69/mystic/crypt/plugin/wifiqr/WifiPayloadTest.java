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
package io.github.astrapi69.mystic.crypt.plugin.wifiqr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The text of the Wi-Fi QR code (#1120), as the ZXing wiki "Barcode Contents" describes it: the
 * special characters escaped with a backslash, a hex-looking value in double quotes, no password
 * for an open network, H:true for a hidden one
 */
class WifiPayloadTest
{

	static Stream<Arguments> networks()
	{
		return Stream.of(
			Arguments.argumentSet("a plain network",
				new WifiNetwork("home", "secret phrase".toCharArray(), "WPA", false),
				"WIFI:T:WPA;S:home;P:secret phrase;;"),
			Arguments.argumentSet("each special character escaped",
				new WifiNetwork("a;b,c", "x\\y\"z:w".toCharArray(), "WPA", false),
				"WIFI:T:WPA;S:a\\;b\\,c;P:x\\\\y\\\"z\\:w;;"),
			Arguments.argumentSet("hex-looking name and password quoted",
				new WifiNetwork("CAFE", "0123abcd".toCharArray(), "WEP", false),
				"WIFI:T:WEP;S:\"CAFE\";P:\"0123abcd\";;"),
			Arguments.argumentSet("an open network carries no password",
				new WifiNetwork("cafe guest", new char[0], "nopass", false),
				"WIFI:T:nopass;S:cafe guest;;"),
			Arguments.argumentSet("a hidden network",
				new WifiNetwork("attic", "up the stairs".toCharArray(), "WPA", true),
				"WIFI:T:WPA;S:attic;P:up the stairs;H:true;;"));
	}

	@ParameterizedTest(name = "{argumentSetName}")
	@MethodSource("networks")
	@DisplayName("the payload of a network")
	void thePayload(final WifiNetwork network, final String expected)
	{
		assertEquals(expected, new String(WifiPayload.of(network)));
	}
}
