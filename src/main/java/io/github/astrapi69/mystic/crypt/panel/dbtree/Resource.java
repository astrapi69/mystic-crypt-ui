package io.github.astrapi69.mystic.crypt.panel.dbtree;

import java.util.Date;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * The class {@link Resource}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Resource
{
	/** The checksum from this resource. */
	String checksum;
	/** The binary data from this resource. */
	byte[] content;
	/** The content type from this resource. */
	String contentType;
	/** The date when this resource is created in the database. */
	Date created;
	/** A description for this resource. */
	String description;
	/** The filename from this resource. */
	String filename;
	/** The filepath from this resource. */
	String filepath;
	/** The size from this resource. */
	long filesize;
}
