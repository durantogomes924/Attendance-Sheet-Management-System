package com.example.attendancesheet.model;

import javafx.beans.property.*;
import java.util.HashMap;
import java.util.Map;

public class StudentAttendance {
    private final StringProperty studentId;
    private final StringProperty studentName;
    private final Map<Integer, IntegerProperty> weekMap = new HashMap<>();
    private final DoubleProperty attendancePercentage;
    private final int totalWeeks;

    public StudentAttendance(String studentId, String studentName, int totalWeeks) {
        this.studentId = new SimpleStringProperty(studentId);
        this.studentName = new SimpleStringProperty(studentName);
        this.totalWeeks = totalWeeks;
        this.attendancePercentage = new SimpleDoubleProperty(0.0);

        javafx.beans.value.ChangeListener<Number> listener = (obs, oldVal, newVal) -> calculatePercentage();
        for (int i = 1; i <= totalWeeks; i++) {
            IntegerProperty prop = new SimpleIntegerProperty(0);
            prop.addListener(listener);
            weekMap.put(i, prop);
        }
    }

    private void calculatePercentage() {
        int attended = 0;
        for (int i = 1; i <= totalWeeks; i++) {
            if (weekMap.get(i).get() == 1) attended++;
        }
        attendancePercentage.set((attended / (double) totalWeeks) * 100.0);
    }

    public String getStudentId() { return studentId.get(); }
    public void setStudentId(String id) { this.studentId.set(id); }
    public StringProperty studentIdProperty() { return studentId; }

    public String getStudentName() { return studentName.get(); }
    public void setStudentName(String name) { this.studentName.set(name); }
    public StringProperty studentNameProperty() { return studentName; }

    public IntegerProperty weekProperty(int week) { return weekMap.get(week); }

    public double getAttendancePercentage() { return attendancePercentage.get(); }
    public DoubleProperty attendancePercentageProperty() { return attendancePercentage; }

    public int getTotalWeeks() { return totalWeeks; }
}