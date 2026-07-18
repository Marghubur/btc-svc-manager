package bt.conference.service;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.TimeZone;

@Service
public class UtilService {
    public static Date toUtc(Date date) {
        // Get the instant in milliseconds
        long time = date.getTime();

        // Offset of default timezone in milliseconds
        TimeZone tz = TimeZone.getDefault();
        int offset = tz.getOffset(time);

        // Subtract offset to get UTC
        return new Date(time - offset);
    }

    public static long extractEmployeeId(String empCode, String employeeCodePrefix) throws Exception {
        if (empCode == null || empCode.trim().isEmpty())
            throw new RuntimeException("Invalid employee code");

        if (employeeCodePrefix != null && !employeeCodePrefix.trim().isEmpty()) {
            if (!empCode.toLowerCase().startsWith(employeeCodePrefix.toLowerCase())) {
                throw new RuntimeException("Here is a mismatch in the employee code. Please contact the admin.");
            }

            empCode = empCode.substring(employeeCodePrefix.length());
        }

        int employeeId;
        try {
            employeeId = Integer.parseInt(empCode);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Could not parse employee ID from '" + empCode + "'.");
        }

        return employeeId;
    }
}
