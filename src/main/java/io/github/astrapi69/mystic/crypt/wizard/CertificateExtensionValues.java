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
package io.github.astrapi69.mystic.crypt.wizard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.util.encoders.Hex;

/**
 * Turns what someone types into a certificate extension that is actually valid.
 * <p>
 * An extension is not a piece of text: its value is DER, and a certificate carrying a "value" that
 * is merely the letters someone typed is malformed - other tools reject it, and the fields it was
 * supposed to carry are simply not there. This class knows the three extensions that come up in
 * practice and builds them properly:
 *
 * <pre>
 * Basic Constraints        2.5.29.19   "CA:true", "CA:true,pathlen:1", "CA:false"
 * Key Usage                2.5.29.15   "digitalSignature,keyCertSign,cRLSign"
 * Subject Alternative Name 2.5.29.17   "DNS:example.com,IP:10.0.0.1,email:me@example.com"
 * </pre>
 *
 * For any other extension the value has to be given as hex, because there is no way to guess how an
 * unknown extension wants to be encoded. Anything that is neither of those is refused with a
 * message that says so, instead of being written into the certificate as rubbish.
 */
public final class CertificateExtensionValues
{

	/** The object id of the basic constraints extension */
	public static final String BASIC_CONSTRAINTS = Extension.basicConstraints.getId();

	/** The object id of the key usage extension */
	public static final String KEY_USAGE = Extension.keyUsage.getId();

	/** The object id of the subject alternative name extension */
	public static final String SUBJECT_ALTERNATIVE_NAME = Extension.subjectAlternativeName.getId();

	private CertificateExtensionValues()
	{
	}

	/**
	 * The extensions this class can build from readable text, with the name to show for each
	 *
	 * @return the object id of every extension that can be typed in words
	 */
	public static List<String> understoodExtensionIds()
	{
		return List.of(BASIC_CONSTRAINTS, KEY_USAGE, SUBJECT_ALTERNATIVE_NAME);
	}

	/**
	 * A short name for an extension, for a user interface that should not have to show bare object
	 * ids
	 *
	 * @param extensionId
	 *            the object id
	 * @return the name, or the object id itself when it is not one of the understood ones
	 */
	public static String displayName(final String extensionId)
	{
		if (BASIC_CONSTRAINTS.equals(extensionId))
		{
			return "Basic Constraints";
		}
		if (KEY_USAGE.equals(extensionId))
		{
			return "Key Usage";
		}
		if (SUBJECT_ALTERNATIVE_NAME.equals(extensionId))
		{
			return "Subject Alternative Name";
		}
		return extensionId;
	}

	/**
	 * An example of what the value of an extension looks like, to be shown next to the input
	 *
	 * @param extensionId
	 *            the object id
	 * @return the example, or a note that hex is expected
	 */
	public static String valueHint(final String extensionId)
	{
		if (BASIC_CONSTRAINTS.equals(extensionId))
		{
			return "CA:true or CA:true,pathlen:1 or CA:false";
		}
		if (KEY_USAGE.equals(extensionId))
		{
			return "digitalSignature,keyCertSign,cRLSign";
		}
		if (SUBJECT_ALTERNATIVE_NAME.equals(extensionId))
		{
			return "DNS:example.com,IP:10.0.0.1,email:me@example.com";
		}
		return "the DER value as hex, for instance 30030101ff";
	}

	/**
	 * Builds an extension from an object id and a value in readable form
	 *
	 * @param extensionId
	 *            the object id of the extension
	 * @param critical
	 *            whether the extension is critical
	 * @param value
	 *            the value, in the form {@link #valueHint(String)} describes
	 * @return the extension
	 * @throws IllegalArgumentException
	 *             if the object id or the value cannot be made sense of
	 */
	public static Extension toExtension(final String extensionId, final boolean critical,
		final String value)
	{
		String trimmedId = extensionId == null ? "" : extensionId.trim();
		if (trimmedId.isEmpty())
		{
			throw new IllegalArgumentException("an extension needs an object id");
		}
		String trimmedValue = value == null ? "" : value.trim();
		try
		{
			if (BASIC_CONSTRAINTS.equals(trimmedId))
			{
				return new Extension(Extension.basicConstraints, critical,
					new DEROctetString(basicConstraints(trimmedValue)));
			}
			if (KEY_USAGE.equals(trimmedId))
			{
				return new Extension(Extension.keyUsage, critical,
					new DEROctetString(keyUsage(trimmedValue)));
			}
			if (SUBJECT_ALTERNATIVE_NAME.equals(trimmedId))
			{
				return new Extension(Extension.subjectAlternativeName, critical,
					new DEROctetString(subjectAlternativeName(trimmedValue)));
			}
			return new Extension(new ASN1ObjectIdentifier(trimmedId), critical,
				new DEROctetString(hex(trimmedValue)));
		}
		catch (IOException | IllegalArgumentException exception)
		{
			throw new IllegalArgumentException("'" + trimmedValue + "' is not a usable value for "
				+ displayName(trimmedId) + " - expected " + valueHint(trimmedId), exception);
		}
	}

	/** Builds the basic constraints extension from something like {@code CA:true,pathlen:1} */
	static BasicConstraints basicConstraints(final String value)
	{
		boolean certificateAuthority = false;
		Integer pathLength = null;
		for (String part : value.split(","))
		{
			String[] keyAndValue = part.trim().split(":", 2);
			if (keyAndValue.length != 2)
			{
				throw new IllegalArgumentException("expected name:value, was '" + part + "'");
			}
			String name = keyAndValue[0].trim().toLowerCase(Locale.ROOT);
			String setting = keyAndValue[1].trim();
			if ("ca".equals(name))
			{
				certificateAuthority = Boolean.parseBoolean(setting);
			}
			else if ("pathlen".equals(name))
			{
				pathLength = Integer.valueOf(setting);
			}
			else
			{
				throw new IllegalArgumentException("unknown setting '" + name + "'");
			}
		}
		if (!certificateAuthority)
		{
			// a path length only means something for a certificate authority
			return new BasicConstraints(false);
		}
		return pathLength != null ? new BasicConstraints(pathLength) : new BasicConstraints(true);
	}

	/** Builds the key usage extension from a list of usage names */
	static KeyUsage keyUsage(final String value)
	{
		int usage = 0;
		for (String part : value.split(","))
		{
			usage |= singleKeyUsage(part.trim());
		}
		if (usage == 0)
		{
			throw new IllegalArgumentException("no usage was named");
		}
		return new KeyUsage(usage);
	}

	private static int singleKeyUsage(final String name)
	{
		return switch (name.toLowerCase(Locale.ROOT))
		{
			case "digitalsignature" -> KeyUsage.digitalSignature;
			case "nonrepudiation", "contentcommitment" -> KeyUsage.nonRepudiation;
			case "keyencipherment" -> KeyUsage.keyEncipherment;
			case "dataencipherment" -> KeyUsage.dataEncipherment;
			case "keyagreement" -> KeyUsage.keyAgreement;
			case "keycertsign" -> KeyUsage.keyCertSign;
			case "crlsign" -> KeyUsage.cRLSign;
			case "encipheronly" -> KeyUsage.encipherOnly;
			case "decipheronly" -> KeyUsage.decipherOnly;
			default -> throw new IllegalArgumentException("unknown key usage '" + name + "'");
		};
	}

	/** Builds the subject alternative name extension from a list of typed names */
	static GeneralNames subjectAlternativeName(final String value)
	{
		List<GeneralName> names = new ArrayList<>();
		for (String part : value.split(","))
		{
			String[] typeAndName = part.trim().split(":", 2);
			if (typeAndName.length != 2)
			{
				throw new IllegalArgumentException("expected type:name, was '" + part + "'");
			}
			names.add(
				new GeneralName(generalNameType(typeAndName[0].trim()), typeAndName[1].trim()));
		}
		if (names.isEmpty())
		{
			throw new IllegalArgumentException("no name was given");
		}
		return new GeneralNames(names.toArray(new GeneralName[0]));
	}

	private static int generalNameType(final String type)
	{
		return switch (type.toLowerCase(Locale.ROOT))
		{
			case "dns" -> GeneralName.dNSName;
			case "ip" -> GeneralName.iPAddress;
			case "email", "rfc822" -> GeneralName.rfc822Name;
			case "uri", "url" -> GeneralName.uniformResourceIdentifier;
			case "dn", "directory" -> GeneralName.directoryName;
			default -> throw new IllegalArgumentException("unknown name type '" + type + "'");
		};
	}

	private static byte[] hex(final String value)
	{
		String cleaned = value.replace("0x", "").replace(" ", "").replace(":", "");
		if (cleaned.isEmpty() || cleaned.length() % 2 != 0 || !cleaned.matches("[0-9a-fA-F]+"))
		{
			throw new IllegalArgumentException("not hex");
		}
		return Hex.decode(cleaned);
	}
}
