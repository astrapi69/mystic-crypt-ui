package de.alpharogroup.mystic.crypt.panels.checksum;

import de.alpharogroup.crypto.algorithm.Algorithm;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ChecksumAlgorithm  implements Algorithm {

    /** The enum constant for MD2 algorithm. */
    MD2("MD2"),

    /** The enum constant for MD4 algorithm. */
    MD4("MD4"),

    /** The enum constant for MD5 algorithm. */
    MD5("MD5"),

    /** The enum constant for SHA-1 algorithm. */
    SHA_1("SHA-1"),

    /** The enum constant for SHA-256 algorithm. */
    SHA_256("SHA-256"),

    /** The enum constant for SHA-384 algorithm. */
    SHA_384("SHA-384"),

    /** The enum constant for SHA-512 algorithm. */
    SHA_512("SHA-512");

    String algorithm;

}
