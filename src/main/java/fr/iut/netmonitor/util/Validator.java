package fr.iut.netmonitor.util;

import fr.iut.netmonitor.exception.ValidationException;

public final class Validator {

    private Validator() {
    }

    public static String requireNotBlank(String value, String field) throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Le champ « " + field + " » ne peut pas être vide.");
        }
        return value.trim();
    }

    /** Vérifie une adresse IPv4 (4 octets 0-255, sans zéros superflus). */
    public static boolean isValidIpv4(String ip) {
        if (ip == null) {
            return false;
        }
        String[] parts = ip.trim().split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3 || !part.chars().allMatch(Character::isDigit)) {
                return false;
            }
            if (part.length() > 1 && part.charAt(0) == '0') {
                return false;
            }
            if (Integer.parseInt(part) > 255) {
                return false;
            }
        }
        return true;
    }

    public static String requireIpv4(String ip) throws ValidationException {
        if (!isValidIpv4(ip)) {
            throw new ValidationException("Adresse IP invalide : " + ip);
        }
        return ip.trim();
    }
}
