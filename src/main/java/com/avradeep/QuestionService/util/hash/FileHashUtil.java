package com.avradeep.QuestionService.util.hash;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHashUtil {

    private FileHashUtil() {
    }

    public static String calculateSHA256(MultipartFile file)
            throws IOException {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] fileBytes = file.getBytes();

            byte[] hashBytes =
                    digest.digest(fileBytes);

            StringBuilder hexString =
                    new StringBuilder();

            for (byte b : hashBytes) {

                String hex =
                        Integer.toHexString(0xff & b);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 algorithm not available.",
                    e
            );
        }
    }
}
