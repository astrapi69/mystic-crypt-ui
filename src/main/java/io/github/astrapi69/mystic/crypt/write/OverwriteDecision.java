package io.github.astrapi69.mystic.crypt.write;

import java.io.File;

/**
 * Whether writing a file needs an answer first, and what that answer is asked for (#300).
 * <p>
 * Display-free on purpose: the question this decides is the same one in a Swing action, in a plugin
 * panel and on the command line, and putting it behind a dialog would make it untestable in the
 * place where it matters. The dialog lives in {@link OverwriteConfirmation}.
 */
public final class OverwriteDecision
{

	private OverwriteDecision()
	{
	}

	/**
	 * Whether the target needs an answer before it is written
	 *
	 * @param target
	 *            the file about to be written
	 * @return true when something is already there and would be replaced
	 */
	public static boolean needsAnAnswer(final File target)
	{
		return target != null && target.isFile();
	}

	/**
	 * The question to put to the user, in the words that fit what the file holds
	 *
	 * @param dataClass
	 *            what the file holds
	 * @param fileName
	 *            the name of the file that would be replaced
	 * @return the message, ready to show
	 */
	public static String messageFor(final DataClass dataClass, final String fileName)
	{
		if (DataClass.IRREPLACEABLE.equals(dataClass))
		{
			return "<html><body>" + "<div>'" + fileName + "' already exists.</div>"
				+ "<div>Replacing it destroys what it holds, and nothing can produce it again.</div>"
				+ "<div>Replace it?</div>" + "</body></html>";
		}
		return "<html><body>" + "<div>'" + fileName + "' already exists.</div>"
			+ "<div>Replace it?</div>" + "</body></html>";
	}

	/**
	 * The title of the question, which says what is being decided rather than repeating the file
	 * name
	 *
	 * @param dataClass
	 *            what the file holds
	 * @return the dialog title
	 */
	public static String titleFor(final DataClass dataClass)
	{
		return DataClass.IRREPLACEABLE.equals(dataClass)
			? "Replace this database?"
			: "Replace the existing file?";
	}
}
