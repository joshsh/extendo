package net.fortytwo.extendo;


import net.fortytwo.extendo.util.properties.PropertyException;
import net.fortytwo.extendo.util.properties.TypedProperties;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class Extendo {
    public static final String DEFAULT_BASEURI = "http://example.org/replaceThisBaseURI#";
    public static final String MOB_ONTOLOGY_FILE = "myotherbrain.owl";

    // Configuration properties.
    public static final String
            BASE_URI = "net.fortytwo.extendo.baseURI",
            NAME = "net.fortytwo.extendo.name",
            ACTIVITY_LOG = "net.fortytwo.extendo.activityLog",
            VERSION = "net.fortytwo.extendo.version",
            REVISION = "net.fortytwo.extendo.revision";

    public static final String
            ALIAS = "alias",
            CREATED = "created",
            FIRST = "first",
            NOTE = "note",
            NOTES = "notes",
            PRIORITY = "priority",
            REST = "rest",
            SHARABILITY = "sharability",
            VALUE = "value",
            WEIGHT = "weight";

    public static final int KEY_DIGITS = 7;

    private static final char[] HEX_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static final Random RANDOM = new Random();

    private static final String CONFIG_PROPERTIES_FILE = "extendo.properties";
    private static final String VERSION_PROPERTIES_FILE = "version.properties";
    private static final TypedProperties VERSION_PROPERTIES;
    private static final Logger LOGGER;

    private static TypedProperties CONFIGURATION;

    static {
        try {
            {
                InputStream in = Extendo.class.getResourceAsStream("logging.properties");
                try {
                    LogManager.getLogManager().reset();
                    LogManager.getLogManager().readConfiguration(in);
                } finally {
                    in.close();
                }
            }

            LOGGER = getLogger(Extendo.class);

            CONFIGURATION = new TypedProperties();
            VERSION_PROPERTIES = new TypedProperties();

            CONFIGURATION.load(Extendo.class.getResourceAsStream(CONFIG_PROPERTIES_FILE));
            VERSION_PROPERTIES.load(Extendo.class.getResourceAsStream(VERSION_PROPERTIES_FILE));

            // Attempt to load additional properties from a user-provided file in the current directory
            File f = new File(CONFIG_PROPERTIES_FILE);
            if (f.exists()) {
                LOGGER.info("loading Extendo configuration at " + f);
                InputStream in = new FileInputStream(f);
                try {
                    CONFIGURATION.load(in);
                } finally {
                    in.close();
                }
            } else {
                LOGGER.info("using default Extendo configuration");
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final MessageDigest SHA1_DIGEST;

    static {
        try {
            SHA1_DIGEST = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Logger getLogger(final Class c) {
        return Logger.getLogger(c.getName());
    }

    public static TypedProperties getConfiguration() {
        return CONFIGURATION;
    }

    public static String getVersionInfo() {
        String name;
        String version;
        int revision;

        try {
            name = VERSION_PROPERTIES.getString(NAME);
            version = VERSION_PROPERTIES.getString(VERSION);
            revision = VERSION_PROPERTIES.getInt(REVISION);
        } catch (PropertyException e) {
            throw new IllegalStateException(e);
        }

        return name + " " + version + ", revision #" + revision;
    }

    ////////////////////////////////////////////////////////////////////////////
    // temporary stuff /////////////////////////////////////////////////////////

    public static URI toURI(final QName q) {
        try {
            return new URI(q.getNamespaceURI() + q.getLocalPart());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static final Random random = new Random();

    public static String sha1SumOf(final String key) {
        SHA1_DIGEST.update(key.getBytes());
        String hash = "";
        byte[] digest = SHA1_DIGEST.digest();
        for (byte b : digest) {
            String hex = Integer.toHexString(b);
            if (hex.length() == 1)
                hex = "0" + hex;
            hex = hex.substring(hex.length() - 2);
            hash = hash + hex;
        }
        return hash;
    }

    /*
       For 5-digit numbers of base 64, expect a collision after 32768 trials (on average).
       There are 1,073,741,824 possibilities.

       int base = 64;
       int length = 5;
       BigDecimal poss = new BigDecimal(base).pow(length);
       BigDecimal trials = new BigDecimal(Math.sqrt((double) base)).pow(length);
       System.out.println("For " + length + "-digit numbers of base " + base + ", expect a collision after "
               + trials + " trials (on average).  There are " + poss + " possibilities.");
    */
    public static String createRandomKey() {
        byte[] bytes = new byte[KEY_DIGITS];
        for (int i = 0; i < KEY_DIGITS; i++) {
            int n = RANDOM.nextInt(64);
            int b = n < 26
                    ? 'A' + n
                    : n < 52
                    ? 'a' + n - 26
                    : n < 62
                    ? '0' + n - 52
                    : n < 63
                    ? '@' : '&';
            bytes[i] = (byte) b;
        }

        return new String(bytes);
    }

    // Note: escapes both high and low (whitespace < 0x20) characters.
    public static String unicodeEscape(final String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 32 || (c >> 7) > 0) {
                sb.append("\\u");
                sb.append(HEX_CHARS[(c >> 12) & 0xF]);
                sb.append(HEX_CHARS[(c >> 8) & 0xF]);
                sb.append(HEX_CHARS[(c >> 4) & 0xF]);
                sb.append(HEX_CHARS[c & 0xF]);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
