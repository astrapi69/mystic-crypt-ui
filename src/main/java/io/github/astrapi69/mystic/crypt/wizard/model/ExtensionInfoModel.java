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
package io.github.astrapi69.mystic.crypt.wizard.model;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.Extension;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * Data class representing extension information
 */
@Data
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtensionInfoModel
{

	/**
	 * The identifier of the extension
	 */
	String extensionId;

	/**
	 * Indicates whether the extension is critical
	 */
	boolean critical;

	/**
	 * The value of the extension
	 */
	String value;

	/**
	 * Converts an {@link ExtensionInfoModel} object to a {@link Extension} object
	 *
	 * @param extensionInfoModel
	 *            the {@link ExtensionInfoModel} object to convert
	 * @return the corresponding {@link Extension} object
	 */
	public static Extension toExtension(final ExtensionInfoModel extensionInfoModel)
	{
		ASN1ObjectIdentifier oid = new ASN1ObjectIdentifier(extensionInfoModel.getExtensionId());
		byte[] extensionValue = extensionInfoModel.getValue().getBytes();
		ASN1OctetString value = new DEROctetString(extensionValue);
		return new Extension(oid, extensionInfoModel.isCritical(), value);
	}

	/**
	 * Transforms an array of {@link ExtensionInfoModel} objects to an array of {@link Extension}
	 * objects
	 *
	 * @param extensionInfoModels
	 *            the array of {@link ExtensionInfoModel} objects
	 * @return an array of {@link Extension} objects
	 */
	public static Extension[] toExtensions(final ExtensionInfoModel[] extensionInfoModels)
	{
		Extension[] extensions = new Extension[extensionInfoModels.length];
		int i = 0;
		for (ExtensionInfoModel extensionInfoModel : extensionInfoModels)
		{
			extensions[i++] = ExtensionInfoModel.toExtension(extensionInfoModel);
		}
		return extensions;
	}

	/**
	 * Factory method to create an {@link ExtensionInfoModel} object from a {@link Extension} object
	 *
	 * @param extension
	 *            the {@link Extension} object to convert
	 * @return the corresponding {@link ExtensionInfoModel} object
	 */
	public static ExtensionInfoModel fromExtension(final Extension extension)
	{
		String extensionId = extension.getExtnId().getId();
		boolean critical = extension.isCritical();
		String value = new String(extension.getExtnValue().getOctets());

		return ExtensionInfoModel.builder().extensionId(extensionId).critical(critical).value(value)
			.build();
	}

	/**
	 * Extracts extension-infos from an {@link X509Certificate} and returns them as a list of
	 * {@link ExtensionInfoModel} objects
	 *
	 * @param certificate
	 *            the {@link X509Certificate} to extract extensions from
	 * @return a list of {@link ExtensionInfoModel} objects
	 */
	public static List<ExtensionInfoModel> extractExtensionInfos(X509Certificate certificate)
	{
		List<ExtensionInfoModel> extensions = new ArrayList<>();

		Set<String> criticalOIDs = certificate.getCriticalExtensionOIDs();
		if (criticalOIDs != null)
		{
			for (String oid : criticalOIDs)
			{
				byte[] extensionValue = certificate.getExtensionValue(oid);
				Extension extension = new Extension(new ASN1ObjectIdentifier(oid), true,
					new DEROctetString(extensionValue));
				extensions.add(ExtensionInfoModel.fromExtension(extension));
			}
		}

		Set<String> nonCriticalOIDs = certificate.getNonCriticalExtensionOIDs();
		if (nonCriticalOIDs != null)
		{
			for (String oid : nonCriticalOIDs)
			{
				byte[] extensionValue = certificate.getExtensionValue(oid);
				Extension extension = new Extension(new ASN1ObjectIdentifier(oid), false,
					new DEROctetString(extensionValue));
				extensions.add(ExtensionInfoModel.fromExtension(extension));
			}
		}

		return extensions;
	}

	/**
	 * Extracts extension-infos from an {@link X509Certificate} and returns them as an array of
	 * {@link ExtensionInfoModel} objects
	 *
	 * @param certificate
	 *            the {@link X509Certificate} to extract extensions from
	 * @return an array of {@link ExtensionInfoModel} objects
	 */
	public static ExtensionInfoModel[] extractToExtensionInfoArray(X509Certificate certificate)
	{
		return extractExtensionInfos(certificate).toArray(new ExtensionInfoModel[0]);
	}
}
