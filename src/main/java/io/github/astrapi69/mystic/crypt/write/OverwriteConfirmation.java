package io.github.astrapi69.mystic.crypt.write;

import java.awt.Component;
import java.io.File;

import javax.swing.JOptionPane;

/**
 * The one place that asks before a file that already exists is replaced (#300).
 * <p>
 * Before this, 23 call sites answered that question on their own: seven refused through five
 * separately written guards whose messages had already drifted apart, two asked, and fourteen
 * overwrote in silence - two of those fourteen replaced a vault, which is data loss on an ordinary
 * menu item.
 * <p>
 * The caller says WHAT it is writing, because that is what a caller knows; this decides and
 * phrases. Nothing here refuses on its own: a refusal the user cannot override is a different
 * decision, and where one is wanted the caller makes it before asking.
 */
public final class OverwriteConfirmation
{

	private OverwriteConfirmation()
	{
	}

	/**
	 * Whether the target may be written, asking first when something is already there
	 *
	 * @param parent
	 *            the component the question belongs to
	 * @param target
	 *            the file about to be written
	 * @param dataClass
	 *            what the file holds, which decides how the question is put
	 * @return true when writing may go ahead
	 */
	public static boolean allowsWriting(final Component parent, final File target,
		final DataClass dataClass)
	{
		if (!OverwriteDecision.needsAnAnswer(target))
		{
			return true;
		}
		int option = JOptionPane.showConfirmDialog(parent,
			OverwriteDecision.messageFor(dataClass, target.getName()),
			OverwriteDecision.titleFor(dataClass), JOptionPane.YES_NO_OPTION,
			DataClass.IRREPLACEABLE.equals(dataClass)
				? JOptionPane.WARNING_MESSAGE
				: JOptionPane.QUESTION_MESSAGE);
		return option == JOptionPane.YES_OPTION;
	}
}
