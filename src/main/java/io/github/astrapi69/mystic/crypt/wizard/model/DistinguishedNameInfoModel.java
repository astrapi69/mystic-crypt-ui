package io.github.astrapi69.mystic.crypt.wizard.model;

import io.github.astrapi69.crypt.data.model.DistinguishedNameInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DistinguishedNameInfoModel
{
	private String commonName;
	private String countryCode;
	private String location;
	private String organisation;
	private String organisationUnit;
	private String state;

	public static DistinguishedNameInfo toDistinguishedNameInfo(
		final DistinguishedNameInfoModel distinguishedNameInfoModel)
	{
		return DistinguishedNameInfo.builder()
			.commonName(distinguishedNameInfoModel.getCommonName())
			.countryCode(distinguishedNameInfoModel.getCountryCode())
			.location(distinguishedNameInfoModel.getLocation())
			.organisation(distinguishedNameInfoModel.getOrganisation())
			.organisationUnit(distinguishedNameInfoModel.getOrganisationUnit())
			.state(distinguishedNameInfoModel.getState()).build();
	}
}
