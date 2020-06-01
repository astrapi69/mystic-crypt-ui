package de.alpharogroup.mystic.crypt.panels.checksum;

import java.io.File;
import java.io.Serializable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChecksumBean implements Serializable
{
	private static final long serialVersionUID = 1L;

	File selectedFile;
	String selectedFilename;
	ChecksumAlgorithm selectedAlgorithm;

}
