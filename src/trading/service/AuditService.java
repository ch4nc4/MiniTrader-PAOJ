package trading.service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditService {
    private static AuditService instance;
    private static final String AUDIT_FILE = "audit.csv";

    private AuditService() {}

    public static synchronized AuditService getInstance() {
        if (instance == null) {
            instance = new AuditService();
        }
        return instance;
    }

    public void log(String actionName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        try (PrintWriter pw = new PrintWriter(new FileWriter(AUDIT_FILE, true))) {
            pw.println(actionName + "," + timestamp);
        } catch (IOException e) {
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }
}
