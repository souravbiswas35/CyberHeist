module com.cyberheist {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.sql;

    // Export your packages
    exports com.cyberheist;
    exports com.cyberheist.Tools;

    // Open packages for FXML reflection
    opens com.cyberheist to javafx.fxml, javafx.base;
    opens com.cyberheist.Tools to javafx.fxml, javafx.base;
}