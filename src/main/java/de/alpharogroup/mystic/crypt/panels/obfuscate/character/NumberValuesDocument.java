package de.alpharogroup.mystic.crypt.panels.obfuscate.character;

import lombok.NonNull;

/**
 * The class {@link NumberValuesDocument} can take any character that is specified in the given regular expression
 */
public class NumberValuesDocument extends RegularExpressionDocument
{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The Constant for the default regular expression. */
	public static final String DEFAULT_REGEX = "^[0-9,;]+$";

	/**
	 * Instantiates a new {@link NumberValuesDocument} object with the default regular expression
	 */
	public NumberValuesDocument()
	{
		this(NumberValuesDocument.DEFAULT_REGEX);
	}

	/**
	 * Instantiates a new {@link NumberValuesDocument} object
	 *
	 * @param regex the regular expression
	 */
	public NumberValuesDocument(@NonNull String regex)
	{
		super(regex);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String validate(String proposedValue) throws IllegalArgumentException
	{
		if(proposedValue.isEmpty()) {
			return proposedValue;
		}
		return super.validate(proposedValue);
	}

}