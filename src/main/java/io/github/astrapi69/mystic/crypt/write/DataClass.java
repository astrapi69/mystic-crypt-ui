package io.github.astrapi69.mystic.crypt.write;

/**
 * What a file about to be overwritten holds, which is what decides how hard the question is (#300).
 * <p>
 * The application answered "this file already exists" in three different ways across 23 call sites,
 * from five separately written guards to fourteen silent overwrites, and the guards' messages had
 * already drifted apart. The classification is what a caller knows and a helper cannot guess, so
 * the caller names it and the helper decides and phrases.
 */
public enum DataClass
{

	/**
	 * Something that can be produced again: a checksum file, an exported PEM, a converted key, a
	 * generated rule file. Recomputing it after the source changed is the ordinary case, so the
	 * question is a confirmation rather than a warning.
	 */
	DERIVED,

	/**
	 * A vault or key material: the original may be the only copy, and replacing it has no undo. The
	 * question names the file and says what is lost, and a mis-click on the wrong button costs
	 * somebody their database.
	 */
	IRREPLACEABLE
}
