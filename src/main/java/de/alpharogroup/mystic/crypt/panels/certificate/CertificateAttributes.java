package de.alpharogroup.mystic.crypt.panels.certificate;


import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CertificateAttributes {
    String commonName;
    String organisation;
    String organisationUnit;
    String countryCode;
    String state;
    String location;

    public String toRepresentableString(){
        StringBuilder stringBuilder = new StringBuilder();
        if(setCertificateValue(stringBuilder, "C", countryCode)){
            stringBuilder.append(", ");
        }
        if(setCertificateValue(stringBuilder, "ST", state)){
            stringBuilder.append(", ");
        }
        if(setCertificateValue(stringBuilder, "L", location)){
            stringBuilder.append(", ");
        }
        if(setCertificateValue(stringBuilder, "O", organisation)){
            stringBuilder.append(", ");
        }
        if(setCertificateValue(stringBuilder, "OU", organisationUnit)){
            stringBuilder.append(", ");
        }
        if(setCertificateValue(stringBuilder, "CN", commonName)){
            stringBuilder.append(", ");
        }
        String result = stringBuilder.toString();
        if(result.endsWith(", ")){
            result = result.substring(0, result.lastIndexOf(", "));
        }
        return result;
    }

    private boolean setCertificateValue(StringBuilder stringBuilder, String key, String certificateValue) {
        if(certificateValue!=null && !certificateValue.isEmpty()){
            stringBuilder.append(key).append("=").append(certificateValue);
            return true;
        }
        return false;
    }
}
