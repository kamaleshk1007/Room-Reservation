// scripts/MakeUser.java
// Local utility run by scripts/make-user-sql.cmd.
// Generates a users table INSERT with a freshly generated BCrypt hash.
// The password is read with hidden input when launched from a real console and
// is never written to disk, logged, or echoed.

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Console;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public class MakeUser {

    private static final String[] VALID_ROLES = {"STAFF", "HOD", "LAB_INCHARGE"};

    public static void main(String[] args) {
        Console console = System.console();
        Scanner stdin = console == null ? new Scanner(System.in) : null;

        if (console == null) {
            System.err.println("note: hidden input is unavailable when stdin is not a console; input will echo.");
        }

        String staffId = checkNonEmpty(prompt(console, stdin, "Staff ID (portal_staff_id): "), "Staff ID");
        String role = promptRole(console, stdin);
        String name = checkNonEmpty(prompt(console, stdin, "Display name: "), "Display name");
        String email = checkNonEmpty(prompt(console, stdin, "Email: "), "Email");

        String password = readHidden(console, stdin, "Password: ");
        String confirm = readHidden(console, stdin, "Confirm password: ");
        if (!password.equals(confirm)) {
            System.err.println("Passwords do not match.");
            System.exit(1);
        }
        if (password.length() < 8) {
            System.err.println("Password must be at least 8 characters.");
            System.exit(1);
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password); // matches the $2a$10$ style used by the seeds

        System.out.println();
        System.out.println("-- INSERT into the users table of the target MySQL database:");
        System.out.println("INSERT INTO users (id, portal_staff_id, name, email, password_hash, role, is_active, created_at, updated_at)");
        System.out.println("VALUES (UUID(), '" + sqlEscape(staffId) + "', '" + sqlEscape(name) + "', '" + sqlEscape(email) + "',");
        System.out.println("        '" + hash + "', '" + role + "', 1, NOW(6), NOW(6));");
        System.out.println();
        System.out.println("Note: the hash above is a real bcrypt hash. The password you entered was not stored anywhere.");
    }

    private static String prompt(Console console, Scanner stdin, String label) {
        if (console != null) {
            return console.readLine(label);
        }
        System.out.print(label);
        return stdin.nextLine();
    }

    private static String readHidden(Console console, Scanner stdin, String label) {
        if (console != null) {
            char[] entered = console.readPassword(label);
            if (entered == null) {
                System.exit(1);
            }
            String value = new String(entered);
            Arrays.fill(entered, ' ');
            return value;
        }
        System.out.print(label);
        return stdin.nextLine();
    }

    private static String promptRole(Console console, Scanner stdin) {
        System.out.println("Role (STAFF / HOD / LAB_INCHARGE):");
        while (true) {
            String role = prompt(console, stdin, "Role: ").trim().toUpperCase(Locale.ROOT);
            for (String valid : VALID_ROLES) {
                if (valid.equals(role)) {
                    return role;
                }
            }
            System.err.println("Invalid role. Choose one of: STAFF, HOD, LAB_INCHARGE.");
        }
    }

    private static String checkNonEmpty(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            System.err.println(field + " must not be empty.");
            System.exit(1);
        }
        return trimmed;
    }

    private static String sqlEscape(String value) {
        return value.replace("\\", "\\\\").replace("'", "''");
    }
}
