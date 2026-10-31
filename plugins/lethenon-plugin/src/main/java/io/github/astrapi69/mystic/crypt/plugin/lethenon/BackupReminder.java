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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * When the overview reminds the user to check the backup again: once the date of the last
 * successful check, the property {@code lethenon.backupVerified}, is more than 180 days before
 * today (decided on 31 October 2026, docs/decisions/lethenon-wallet-in-vault-2026-10-30.md).
 */
final class BackupReminder
{

	/** The date of the last successful backup check, ISO 8601, a plain property */
	static final String VERIFIED = "lethenon.backupVerified";

	/** The days after a check within which no reminder appears */
	static final int INTERVAL_DAYS = 180;

	private BackupReminder()
	{
	}

	/**
	 * The backup's state on a day
	 *
	 * @param entry
	 *            the wallet's entry
	 * @param today
	 *            the day to judge by
	 * @return not checked while no readable date is recorded, due after the interval, else current
	 */
	static BackupState stateOf(final WalletEntry entry, final LocalDate today)
	{
		if (!entry.hasProperty(VERIFIED))
		{
			return BackupState.NOT_CHECKED;
		}
		LocalDate checked;
		try
		{
			checked = LocalDate.parse(entry.textOf(VERIFIED));
		}
		catch (final DateTimeParseException unreadable)
		{
			// a date nobody can read proves no check: the notice, not a silent "current"
			return BackupState.NOT_CHECKED;
		}
		return checked.plusDays(INTERVAL_DAYS).isBefore(today)
			? BackupState.DUE
			: BackupState.CURRENT;
	}
}
