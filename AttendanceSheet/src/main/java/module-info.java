module com.example.attendancesheet {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens com.example.attendancesheet to javafx.fxml;
    opens com.example.attendancesheet.controller to javafx.fxml;
    opens com.example.attendancesheet.model to javafx.base;

    exports com.example.attendancesheet;
}