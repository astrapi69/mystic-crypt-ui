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
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.security.Security;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Writes a vault with a release older than 8.6, through that release's own save path, the way the
 * release builds a new vault: a root group and a "mykeys" group from its own tree factory, and one
 * entry in each group with every field that release has. Reflection only, so one source runs
 * against both API generations (7.x: swing.tree, 8.x: swing.renderer.tree and swing.tree.factory)
 * and against String and char[] fields alike. It wrote the vaults of 7.2 and 7.3 in
 * {@code src/testSupport/resources/compat/vaults} (#624), through {@code write-vaults-of-7x.sh}.
 * <p>
 * Usage: {@code java -cp <release app jar> WriteWithAnOldRelease.java <vault file>}, the password
 * on standard input. Nothing of this checkout is on the class path.
 */
public class WriteWithAnOldRelease
{

	private static final PrintStream OUT = new PrintStream(
		new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);

	private static final OffsetDateTime WHEN = OffsetDateTime.of(2024, 5, 6, 7, 8, 9, 0,
		ZoneOffset.ofHours(2));

	public static void main(String[] arguments) throws Exception
	{
		Security.addProvider((Provider)Class
			.forName("org.bouncycastle.jce.provider.BouncyCastleProvider").getDeclaredConstructor()
			.newInstance());
		File vault = new File(arguments[0]);
		char[] password = new String(System.in.readAllBytes(), StandardCharsets.UTF_8).strip()
			.toCharArray();

		Class<?> modelType = Class.forName("io.github.astrapi69.mystic.crypt.ApplicationModelBean");
		Class<?> entryType = Class
			.forName("io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean");
		Class<?> credentialsType = Class
			.forName("io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean");

		Class<?> elementType = firstClass("io.github.astrapi69.swing.renderer.tree.GenericTreeElement",
			"io.github.astrapi69.swing.tree.GenericTreeElement");
		Class<?> factoryType = firstClass("io.github.astrapi69.swing.tree.factory.BaseTreeNodeFactory",
			"io.github.astrapi69.swing.tree.BaseTreeNodeFactory");
		Class<?> transformerType = Class
			.forName("io.github.astrapi69.gen.tree.convert.BaseTreeNodeTransformer");
		Object idGenerator = Class.forName("io.github.astrapi69.id.generate.LongIdGenerator")
			.getMethod("of", Long.class).invoke(null, 0L);

		// the entries live in their group's element, as the release keeps them: its tree panel
		// reads and writes the element's default content, and the model's dataOfNodes is a field
		// no code of 7.x or 8.x fills (measured on the 7.3 application jar: only
		// ApplicationModelBean itself names it)
		Object first = newEntry(entryType, "the bank", "account holder", "written by the release",
			"https://bank.example.org", "Grüße, with umlauts", true);
		Object second = newEntry(entryType, "the mail", null, "also written by the release", null,
			null, false);
		Object rootElement = treeElement(elementType, "root", "io/github/astrapi69/silk/icons/book.png",
			first);
		Object keysElement = treeElement(elementType, "mykeys",
			"io/github/astrapi69/silk/icons/folder.png", second);
		Method initialize = method(factoryType, "initializeTreeNodeWithTreeElement", 3);
		Object rootNode = initialize.invoke(null, rootElement, null, idGenerator);
		Object keysNode = initialize.invoke(null, keysElement, rootNode, idGenerator);
		Map<?, ?> treeAsMap = (Map<?, ?>)method(transformerType, "toKeyMap", 1).invoke(null, rootNode);
		Long keysId = (Long)keysNode.getClass().getMethod("getId").invoke(keysNode);

		Object credentials = credentialsType.getDeclaredConstructor().newInstance();
		Class<?> fileInfoType = credentialsType.getDeclaredField("applicationFileInfo").getType();
		set(credentials, "applicationFileInfo",
			fileInfoType.getMethod("toFileInfo", File.class).invoke(null, vault));
		set(credentials, "selectedApplicationFilePath", vault.getAbsolutePath());
		set(credentials, "masterPw", password);
		set(credentials, "withMasterPw", true);
		set(credentials, "withKeyFile", false);

		Object model = modelType.getDeclaredConstructor().newInstance();
		set(model, "rootTreeAsMap", treeAsMap);
		set(model, "lastId", keysId);
		set(model, "masterPwFileModelBean", credentials);

		Class.forName("io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileStoreWorker")
			.getMethod("saveToFileWithPassword", modelType).invoke(null, model);
		OUT.println("GROUPS " + treeAsMap.size() + " ENTRIES 2 FIELDS " + fieldsOf(entryType));
		OUT.println("WRITTEN");
	}

	private static Object treeElement(final Class<?> elementType, final String name,
		final String iconPath, final Object entry) throws Exception
	{
		Object builder = elementType.getMethod("builder").invoke(null);
		builder.getClass().getMethod("name", String.class).invoke(builder, name);
		builder.getClass().getMethod("iconPath", String.class).invoke(builder, iconPath);
		builder.getClass().getMethod("withText", boolean.class).invoke(builder, true);
		Object element = builder.getClass().getMethod("build").invoke(builder);
		method(elementType, "setDefaultContent", 1).invoke(element, new ArrayList<>(List.of(entry)));
		return element;
	}

	private static Object newEntry(final Class<?> entryType, final String title,
		final String userName, final String password, final String url, final String notes,
		final boolean everything) throws Exception
	{
		Object entry = entryType.getDeclaredConstructor().newInstance();
		set(entry, "id", UUID.randomUUID());
		set(entry, "title", title);
		set(entry, "userName", userName);
		set(entry, "password", password);
		set(entry, "url", url);
		set(entry, "notes", notes);
		if (!everything)
		{
			return entry;
		}
		set(entry, "expires", LocalDate.of(2030, 1, 2));
		setIfPresent(entry, "preciseExpiryTime", WHEN);
		setIfPresent(entry, "creationTime", WHEN);
		setIfPresent(entry, "lastAccessTime", WHEN);
		setIfPresent(entry, "lastModificationTime", WHEN);
		setIfPresent(entry, "keePassIconIndex", 57);

		Object attachment = elementOf(entryType, "resources").getDeclaredConstructor().newInstance();
		set(attachment, "name", "codes.txt");
		set(attachment, "content", "the recovery codes".getBytes(StandardCharsets.UTF_8));
		set(entry, "resources", new ArrayList<>(List.of(attachment)));

		Object property = elementOf(entryType, "properties").getDeclaredConstructor().newInstance();
		set(property, "key", "TOTP seed");
		set(property, "value", "JBSWY3DPEHPK3PXP");
		set(entry, "properties", new ArrayList<>(List.of(property)));

		Field modifications = fieldOrNull(entryType, "dateTimesOfModification");
		if (modifications != null)
		{
			Object pair = modifications.getType().getDeclaredConstructor().newInstance();
			set(pair, "key", "title");
			set(pair, "values", new LinkedHashSet<>(List.of(WHEN, WHEN.plusDays(1))));
			set(entry, "dateTimesOfModification", pair);
		}
		return entry;
	}

	private static Class<?> elementOf(final Class<?> owner, final String listField)
		throws Exception
	{
		ParameterizedType listType = (ParameterizedType)owner.getDeclaredField(listField)
			.getGenericType();
		Object argument = listType.getActualTypeArguments()[0];
		return (Class<?>)(argument instanceof ParameterizedType parameterized
			? parameterized.getRawType()
			: argument);
	}

	private static Class<?> firstClass(final String... names) throws ClassNotFoundException
	{
		for (String name : names)
		{
			try
			{
				return Class.forName(name);
			}
			catch (ClassNotFoundException absent)
			{
				// the other API generation
			}
		}
		throw new ClassNotFoundException(String.join(" or ", names));
	}

	private static Method method(final Class<?> type, final String name, final int parameters)
		throws NoSuchMethodException
	{
		for (Method candidate : type.getMethods())
		{
			if (candidate.getName().equals(name) && candidate.getParameterCount() == parameters)
			{
				return candidate;
			}
		}
		throw new NoSuchMethodException(type.getName() + "." + name + "/" + parameters);
	}

	private static Field fieldOrNull(final Class<?> type, final String name)
	{
		for (Class<?> current = type; current != null; current = current.getSuperclass())
		{
			try
			{
				Field field = current.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			}
			catch (NoSuchFieldException absent)
			{
				// look in the superclass
			}
		}
		return null;
	}

	private static void setIfPresent(final Object target, final String name, final Object value)
		throws Exception
	{
		if (fieldOrNull(target.getClass(), name) != null)
		{
			set(target, name, value);
		}
	}

	private static void set(final Object target, final String name, final Object value)
		throws Exception
	{
		Field field = fieldOrNull(target.getClass(), name);
		if (field == null)
		{
			throw new NoSuchFieldException(target.getClass().getName() + "." + name);
		}
		Object converted = value;
		if (value instanceof String text && field.getType() == char[].class)
		{
			converted = text.toCharArray();
		}
		else if (value instanceof char[] characters && field.getType() == String.class)
		{
			converted = new String(characters);
		}
		field.set(target, converted);
	}

	private static String fieldsOf(final Class<?> type)
	{
		List<String> names = new ArrayList<>();
		for (Field field : type.getDeclaredFields())
		{
			if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()))
			{
				names.add(field.getName());
			}
		}
		return String.join(",", names);
	}
}
