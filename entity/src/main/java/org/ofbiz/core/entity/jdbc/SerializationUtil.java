package org.ofbiz.core.entity.jdbc;

import org.apache.commons.codec.binary.Base64;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Factors out serialization and Base64 encoding logic.
 *
 * @since v1.1
 */
public class SerializationUtil {
    private SerializationUtil() {
        throw new Error("static-only");
    }


    @Nullable
    public static byte[] serialize(@Nullable final Object obj) {
        if (obj == null) {
            return null;
        }

        final ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        try {
            final ObjectOutputStream out = new ObjectOutputStream(os);
            try {
                out.writeObject(obj);
            } finally {
                out.close();
            }
            return os.toByteArray();
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Trying to persist an object that can't be serialized", ioe);
        }
    }

    @Nullable
    public static Object deserialize(@Nullable final byte[] binaryInput) {
        return (binaryInput != null) ? deserialize(new ByteArrayInputStream(binaryInput)) : null;
    }

    @Nullable
    public static Object deserialize(@Nullable final InputStream binaryInput) {
        if (null == binaryInput) {
            return null;
        }
        try {
            ObjectInputStream in = new ObjectInputStream(binaryInput);
            try {
                return in.readObject();
            } finally {
                in.close();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read in persisted object", ex);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Unable to load class for persisted object", ex);
        }
    }

    @Nullable
    public static String encodeBase64(@Nullable byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        final byte[] encoded = Base64.encodeBase64(bytes, true);
        try {
            return new String(encoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is always supported
            throw new Error(e);
        }
    }

}
