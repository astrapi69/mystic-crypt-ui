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
package io.github.astrapi69.mystic.crypt.keepass;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The facts a KDBX round trip is judged on, read out of the XML KeePassXC exports.
 * <p>
 * Only what the agreed scope names is read, and the two fields deliberately left out of it -
 * {@code UsageCount} and {@code LocationChanged} - are not read here either, so the test cannot
 * silently start asserting them.
 */
final class KdbxFacts
{

	/** A group as it appears in the tree: its path, its name and its icon index */
	record Group(String path, String name, String iconIndex, String identifier) {
	}

	/** One entry with every field the scope names */
	record Entry(String identifier, String iconIndex, String creationTime,
		String lastModificationTime, String lastAccessTime, String expiryTime, String expires,
		String title, String userName, String password, String notes, String url,
		Map<String, String> customProperties, Map<String, Boolean> customPropertyProtection,
		List<String> attachmentNames, int historyVersions, String groupPath) {

		/**
		 * The path keepassxc-cli addresses this entry by, which does NOT include the database's
		 * root group - the first segment of {@link #groupPath()} is that root and is dropped here
		 *
		 * @return the path as the tool expects it
		 */
		String keePassXcPath()
		{
			int firstSeparator = groupPath.indexOf('/');
			String withoutRoot = firstSeparator < 0 ? "" : groupPath.substring(firstSeparator + 1);
			return withoutRoot.isEmpty() ? title : withoutRoot + "/" + title;
		}
	}

	private final List<Group> groups = new ArrayList<>();
	private final List<Entry> entries = new ArrayList<>();

	private KdbxFacts()
	{
	}

	/**
	 * Reads the facts out of a KeePassXC XML export
	 *
	 * @param xml
	 *            the exported XML
	 * @return the facts
	 */
	static KdbxFacts of(final String xml)
	{
		KdbxFacts facts = new KdbxFacts();
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Element root = builder
				.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
				.getDocumentElement();
			Element rootGroup = firstChild(firstChild(root, "Root"), "Group");
			facts.readGroup(rootGroup, "");
		}
		catch (Exception exception)
		{
			throw new IllegalStateException("cannot read the KeePassXC export", exception);
		}
		return facts;
	}

	List<Group> groups()
	{
		return groups;
	}

	List<Entry> entries()
	{
		return entries;
	}

	/**
	 * The group paths in tree order, which is what an added level or a renamed group shows up in
	 */
	List<String> groupPaths()
	{
		return groups.stream().map(Group::path).toList();
	}

	/**
	 * The single entry of a database that has one, so a test reads like the thing it asserts
	 *
	 * @return that entry
	 */
	Entry onlyEntry()
	{
		if (entries.size() != 1)
		{
			throw new IllegalStateException(
				"expected exactly one entry, found " + entries.size() + " - " + entries);
		}
		return entries.get(0);
	}

	private void readGroup(final Element group, final String parentPath)
	{
		String name = textOf(group, "Name");
		String path = parentPath.isEmpty() ? name : parentPath + "/" + name;
		groups.add(new Group(path, name, textOf(group, "IconID"), textOf(group, "UUID")));
		for (Element child : childrenNamed(group, "Entry"))
		{
			entries.add(readEntry(child, path));
		}
		for (Element child : childrenNamed(group, "Group"))
		{
			readGroup(child, path);
		}
	}

	private Entry readEntry(final Element entry, final String groupPath)
	{
		Element times = firstChild(entry, "Times");
		Map<String, String> properties = new LinkedHashMap<>();
		Map<String, Boolean> protection = new LinkedHashMap<>();
		String notes = "";
		String url = "";
		String title = "";
		String userName = "";
		String password = "";
		for (Element string : childrenNamed(entry, "String"))
		{
			String key = textOf(string, "Key");
			Element value = firstChild(string, "Value");
			String text = value == null ? "" : value.getTextContent();
			boolean protectedValue = value != null
				&& "True".equals(value.getAttribute("ProtectInMemory"));
			switch (key)
			{
				case "Notes" -> notes = text;
				case "URL" -> url = text;
				// the standard fields are compared on their own, not as custom properties
				case "Title" -> title = text;
				case "UserName" -> userName = text;
				case "Password" -> password = text;
				default -> {
					properties.put(key, text);
					protection.put(key, protectedValue);
				}
			}
		}
		List<String> attachments = new ArrayList<>();
		for (Element binary : childrenNamed(entry, "Binary"))
		{
			attachments.add(textOf(binary, "Key"));
		}
		Element history = firstChild(entry, "History");
		int historyVersions = history == null ? 0 : childrenNamed(history, "Entry").size();
		return new Entry(textOf(entry, "UUID"), textOf(entry, "IconID"),
			asInstant(textOf(times, "CreationTime")),
			asInstant(textOf(times, "LastModificationTime")),
			asInstant(textOf(times, "LastAccessTime")), asInstant(textOf(times, "ExpiryTime")),
			textOf(times, "Expires"), title, userName, password, notes, url, properties, protection,
			attachments, historyVersions, groupPath);
	}

	/**
	 * KDBX 3.1 writes a timestamp as an ISO string and KDBX 4 as base64 seconds since year one.
	 * Both become the same ISO instant here, or a round trip that changed the file's version would
	 * read as a changed timestamp
	 *
	 * @param value
	 *            the raw element text
	 * @return the instant as text, or the raw value when it is neither shape
	 */
	private static String asInstant(final String value)
	{
		if (value == null || value.isBlank() || value.endsWith("Z") && value.contains("-"))
		{
			return value;
		}
		try
		{
			byte[] decoded = Base64.getDecoder().decode(value);
			long seconds = 0;
			for (int index = Math.min(8, decoded.length) - 1; index >= 0; index--)
			{
				seconds = seconds << 8 | decoded[index] & 0xFF;
			}
			return LocalDateTime.of(1, 1, 1, 0, 0).toInstant(ZoneOffset.UTC).plusSeconds(seconds)
				.toString();
		}
		catch (IllegalArgumentException exception)
		{
			return value;
		}
	}

	private static String textOf(final Element parent, final String childName)
	{
		Element child = firstChild(parent, childName);
		return child == null ? null : child.getTextContent();
	}

	private static Element firstChild(final Element parent, final String childName)
	{
		List<Element> found = childrenNamed(parent, childName);
		return found.isEmpty() ? null : found.get(0);
	}

	/** Direct children only - a nested group's Name must not be read as its parent's */
	private static List<Element> childrenNamed(final Element parent, final String childName)
	{
		List<Element> found = new ArrayList<>();
		if (parent == null)
		{
			return found;
		}
		NodeList children = parent.getChildNodes();
		for (int index = 0; index < children.getLength(); index++)
		{
			Node child = children.item(index);
			if (child.getNodeType() == Node.ELEMENT_NODE && childName.equals(child.getNodeName()))
			{
				found.add((Element)child);
			}
		}
		return found;
	}
}
