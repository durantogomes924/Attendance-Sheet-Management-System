package com.example.attendancesheet.controller;

import com.example.attendancesheet.model.StudentAttendance;
import javafx.beans.property.IntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;

public class AttendanceController {

    // ==================== FXML Components ====================
    @FXML private BorderPane root;
    @FXML private VBox sidebar;
    @FXML private Label sidebarTitle;
    @FXML private TextField subjectSearchField;
    @FXML private ListView<String> classList;
    @FXML private TextField newSubjectField;
    @FXML private TextField teacherNameField;
    @FXML private Button addSubjectBtn;

    @FXML private HBox topBar;
    @FXML private Label enrollLabel;
    @FXML private TextField idField;
    @FXML private TextField nameField;
    @FXML private Button addStudentBtn;
    @FXML private TextField searchField;
    @FXML private Button zoomResetBtn;
    @FXML private Button modeToggleBtn;
    @FXML private Button translateBtn;

    @FXML private VBox centerContainer;
    @FXML private Label middleSubjectHeader;
    @FXML private Label middleTeacherHeader;
    @FXML private TableView<StudentAttendance> table;
    @FXML private Button copyAllBtn;
    @FXML private Button pasteAllBtn;

    // ==================== Data ====================
    private final ObservableList<String> subjects = FXCollections.observableArrayList(
            "Comprehensive Design of Data Structures"
    );
    private final Map<String, String> subjectTeacherMap = new HashMap<>();
    private final Map<String, ObservableList<StudentAttendance>> subjectDataMap = new HashMap<>();
    private final Map<String, FilteredList<StudentAttendance>> subjectFilteredMap = new HashMap<>();

    private FilteredList<String> filteredSubjects;
    private boolean isDarkMode = false;
    private boolean isChinese = false;
    private final int totalConfiguredWeeks = 10;
    private final String customAccentColor = "#3A5A40";

    private Scale scaleTransform;
    private Stage mainStage;

    // Clipboard for copy/paste
    private static class CopiedStudentData {
        String id, name;
        Map<Integer, Integer> weekStates;
        CopiedStudentData(String id, String name, Map<Integer, Integer> weekStates) {
            this.id = id; this.name = name; this.weekStates = weekStates;
        }
    }
    private List<CopiedStudentData> clipboardData = null;

    // ==================== Initialize ====================
    @FXML
    public void initialize() {
        subjectTeacherMap.put("Comprehensive Design of Data Structures", "Prof. Lord");

        for (String subject : subjects) {
            ObservableList<StudentAttendance> dataList = FXCollections.observableArrayList();
            subjectDataMap.put(subject, dataList);
            subjectFilteredMap.put(subject, new FilteredList<>(dataList, p -> true));
        }

        filteredSubjects = new FilteredList<>(subjects, s -> true);
        classList.setItems(filteredSubjects);

        // Subject search
        subjectSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredSubjects.setPredicate(subject -> {
                if (newVal == null || newVal.isEmpty()) return true;       // ← Correct
                return subject.toLowerCase().contains(newVal.toLowerCase());
            });
        });

        // Student search
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String selected = classList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                subjectFilteredMap.get(selected).setPredicate(student -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    String lower = newVal.toLowerCase();
                    return student.getStudentName().toLowerCase().contains(lower)
                            || student.getStudentId().toLowerCase().contains(lower);
                });
            }
        });

        // Subject selection
        classList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                middleSubjectHeader.setText(isChinese && selected.equals("Comprehensive Design of Data Structures")
                        ? "数据结构综合课程设计" : selected);
                middleTeacherHeader.setText(tr("Instructor: ", "讲师: ") + subjectTeacherMap.getOrDefault(selected, "TBA"));
                searchField.clear();
                table.setItems(subjectFilteredMap.get(selected));
                rebuildTableColumns();
            }
        });

        // Context menu for subjects
        ContextMenu subjectMenu = new ContextMenu();
        MenuItem editSubject = new MenuItem("✏ Rename Subject");
        MenuItem deleteSubject = new MenuItem("🗑 Delete Subject");
        subjectMenu.getItems().addAll(editSubject, deleteSubject);

        classList.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(isChinese && item.equals("Comprehensive Design of Data Structures")
                                ? "数据结构综合课程设计" : item);
                    }
                }
            };
            cell.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
                cell.setContextMenu(isEmpty ? null : subjectMenu);
            });
            return cell;
        });

        editSubject.setOnAction(e -> {
            String selected = classList.getSelectionModel().getSelectedItem();
            if (selected != null) editSubject(selected);
        });

        deleteSubject.setOnAction(e -> {
            String selected = classList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (subjects.size() <= 1) {
                    showAlert(tr("You must keep at least one active subject.", "您必须保留至少一个活跃科目。"));
                    return;
                }
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        tr("Delete subject '", "删除科目 '") + selected + "'?", ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        subjects.remove(selected);
                        subjectDataMap.remove(selected);
                        subjectFilteredMap.remove(selected);
                        subjectTeacherMap.remove(selected);
                        if (!subjects.isEmpty()) classList.getSelectionModel().select(0);
                    }
                });
            }
        });

        // Zoom transform
        scaleTransform = new Scale(1.0, 1.0);
        root.getTransforms().add(scaleTransform);

        // Select first subject
        if (!subjects.isEmpty()) {
            classList.getSelectionModel().select(0);
        }

        applyTheme(false);
        rebuildTableColumns();
    }

    // ==================== Helper ====================
    private String tr(String en, String zh) {
        return isChinese ? zh : en;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.showAndWait();
    }

    // ==================== Add Subject ====================
    @FXML
    private void handleAddSubject() {
        String subjectName = newSubjectField.getText().trim();
        String teacherName = teacherNameField.getText().trim();
        if (!subjectName.isEmpty() && !teacherName.isEmpty() && !subjects.contains(subjectName)) {
            subjects.add(subjectName);
            subjectTeacherMap.put(subjectName, teacherName);
            subjectDataMap.put(subjectName, FXCollections.observableArrayList());
            subjectFilteredMap.put(subjectName, new FilteredList<>(subjectDataMap.get(subjectName), p -> true));
            newSubjectField.clear();
            teacherNameField.clear();
            classList.getSelectionModel().select(subjectName);
        } else {
            showAlert(tr("Please enter both Subject Name and Teacher Name.", "请输入科目名称和教师姓名。"));
        }
    }

    // ==================== Add Student ====================
    @FXML
    private void handleAddStudent() {
        String id = idField.getText().trim();
        String name = nameField.getText().trim();
        String selected = classList.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(tr("Please select a subject first.", "请先选择一个科目。"));
            return;
        }
        if (id.isEmpty() || name.isEmpty()) {
            showAlert(tr("Please enter Student ID and Name.", "请输入学生编号和姓名。"));
            return;
        }

        ObservableList<StudentAttendance> list = subjectDataMap.get(selected);
        boolean exists = list.stream().anyMatch(s -> s.getStudentId().equalsIgnoreCase(id));
        if (exists) {
            showAlert(tr("Student ID already exists.", "学生编号已存在。"));
            return;
        }

        list.add(new StudentAttendance(id, name.toUpperCase(), totalConfiguredWeeks));
        idField.clear();
        nameField.clear();
    }

    // ==================== Table Columns ====================
    private void rebuildTableColumns() {
        table.getColumns().clear();

        TableColumn<StudentAttendance, String> nameCol = new TableColumn<>(tr("Student Name", "学生姓名"));
        nameCol.setCellValueFactory(cell -> cell.getValue().studentNameProperty());
        nameCol.setPrefWidth(140);

        TableColumn<StudentAttendance, String> idCol = new TableColumn<>(tr("Student ID", "学生编号"));
        idCol.setCellValueFactory(cell -> cell.getValue().studentIdProperty());
        idCol.setPrefWidth(100);

        TableColumn<StudentAttendance, Number> percentCol = new TableColumn<>(tr("Attendance %", "出勤率 %"));
        percentCol.setCellValueFactory(cell -> cell.getValue().attendancePercentageProperty());
        percentCol.setPrefWidth(105);
        percentCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Number percentage, boolean empty) {
                super.updateItem(percentage, empty);
                if (empty || percentage == null) {
                    setText(null);
                    setStyle("");
                } else {
                    double val = percentage.doubleValue();
                    String color = val >= 75 ? (isDarkMode ? "#34d399" : "#047857")
                            : (val >= 50 ? (isDarkMode ? "#fbbf24" : "#b45309")
                            : (isDarkMode ? "#f87171" : "#b91c1c"));
                    setText(String.format("%.1f%%", val));
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
                }
            }
        });

        table.getColumns().addAll(nameCol, idCol, percentCol);

        for (int i = 1; i <= totalConfiguredWeeks; i++) {
            final int weekIndex = i;
            TableColumn<StudentAttendance, Number> weekCol = new TableColumn<>(tr("W", "第") + i + tr("", "周"));
            weekCol.setCellValueFactory(cell -> cell.getValue().weekProperty(weekIndex));
            weekCol.setPrefWidth(55);
            weekCol.setCellFactory(tc -> new TableCell<>() {
                @Override
                protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        int state = item.intValue();
                        String text;
                        String color;
                        if (state == 1) {
                            text = "✔";
                            color = isDarkMode ? "#34d399" : "#047857";
                        } else if (state == 2) {
                            text = "✖";
                            color = isDarkMode ? "#f87171" : "#b91c1c";
                        } else {
                            text = "-";
                            color = isDarkMode ? "#6b7280" : "#475569";
                        }
                        setText(text);
                        setStyle("-fx-alignment: CENTER; -fx-text-fill: " + color + "; -fx-font-weight: bold;");

                        setOnMouseClicked(e -> {
                            StudentAttendance student = getTableView().getItems().get(getIndex());
                            IntegerProperty prop = student.weekProperty(weekIndex);
                            prop.set((prop.get() + 1) % 3);
                        });
                    }
                }
            });
            table.getColumns().add(weekCol);
        }

        // Row context menu + triple click for stats
        ContextMenu studentMenu = new ContextMenu();
        MenuItem editStudent = new MenuItem(tr("✏ Edit Student Info", "✏ 编辑学生信息"));
        MenuItem deleteStudent = new MenuItem(tr("🗑 Remove Student", "🗑 移除学生"));
        studentMenu.getItems().addAll(editStudent, deleteStudent);

        table.setRowFactory(tv -> {
            TableRow<StudentAttendance> row = new TableRow<>();
            row.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
                row.setContextMenu(isEmpty ? null : studentMenu);
            });
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 3 && !row.isEmpty()) {
                    showStudentStatistics(row.getItem());
                }
            });
            return row;
        });

        editStudent.setOnAction(e -> {
            StudentAttendance selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) editStudent(selected);
        });

        deleteStudent.setOnAction(e -> {
            StudentAttendance selected = table.getSelectionModel().getSelectedItem();
            String current = classList.getSelectionModel().getSelectedItem();
            if (selected != null && current != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        tr("Remove ", "移除 ") + selected.getStudentName() + "?", ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        subjectDataMap.get(current).remove(selected);
                    }
                });
            }
        });
    }

    // ==================== Edit Methods ====================
    private void editSubject(String oldName) {
        TextInputDialog dialog = new TextInputDialog(oldName);
        dialog.setTitle(tr("Edit Subject Name", "编辑科目名称"));
        dialog.setHeaderText(tr("Rename subject:", "重命名科目:"));
        dialog.showAndWait().ifPresent(newName -> {
            newName = newName.trim();
            if (!newName.isEmpty() && !subjects.contains(newName)) {
                int idx = subjects.indexOf(oldName);
                subjects.set(idx, newName);
                String teacher = subjectTeacherMap.remove(oldName);
                subjectTeacherMap.put(newName, teacher != null ? teacher : "TBA");
                ObservableList<StudentAttendance> data = subjectDataMap.remove(oldName);
                subjectDataMap.put(newName, data);
                FilteredList<StudentAttendance> filtered = subjectFilteredMap.remove(oldName);
                subjectFilteredMap.put(newName, filtered);
                classList.getSelectionModel().select(newName);
            }
        });
    }

    private void editStudent(StudentAttendance student) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(tr("Edit Student", "编辑学生"));
        dialog.setHeaderText(tr("Modify Student Information", "修改学生信息"));

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        TextField idTf = new TextField(student.getStudentId());
        TextField nameTf = new TextField(student.getStudentName());
        content.getChildren().addAll(
                new Label(tr("Student ID:", "学生编号:")), idTf,
                new Label(tr("Student Name:", "学生姓名:")), nameTf
        );
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                student.setStudentId(idTf.getText().trim());
                student.setStudentName(nameTf.getText().trim().toUpperCase());
                table.refresh();
            }
            return null;
        });
        dialog.showAndWait();
    }

    // ==================== Statistics ====================
    private void showStudentStatistics(StudentAttendance student) {
        sidebar.getChildren().clear();

        Label statsTitle = new Label(tr("STUDENT STATISTICS", "学生统计"));
        statsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        int present = 0, absent = 0;
        for (int i = 1; i <= totalConfiguredWeeks; i++) {
            int state = student.weekProperty(i).get();
            if (state == 1) present++;
            else if (state == 2) absent++;
        }
        int unmarked = totalConfiguredWeeks - present - absent;

        VBox infoCard = new VBox(6);
        infoCard.setPadding(new Insets(10));
        infoCard.setStyle("-fx-background-color: " + (isDarkMode ? "#334155" : "#ffffff") + "; -fx-background-radius: 8;");
        infoCard.getChildren().addAll(
                new Label(tr("Name: ", "姓名: ") + student.getStudentName()),
                new Label(tr("ID: ", "编号: ") + student.getStudentId()),
                new Label(tr("Present: ", "出勤: ") + present),
                new Label(tr("Absent: ", "缺勤: ") + absent),
                new Label(tr("Unmarked: ", "未标记: ") + unmarked),
                new Label(tr("Percentage: ", "出勤率: ") + String.format("%.1f%%", student.getAttendancePercentage()))
        );

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data(tr("Present", "出勤") + " (" + present + ")", present),
                new PieChart.Data(tr("Absent", "缺勤") + " (" + absent + ")", absent),
                new PieChart.Data(tr("Unmarked", "未标记") + " (" + unmarked + ")", Math.max(0, unmarked))
        );
        PieChart pie = new PieChart(pieData);
        pie.setPrefSize(220, 200);
        pie.setLabelsVisible(false);

        Button backBtn = new Button(tr("← Back to Course Modules", "← 返回课程模块"));
        backBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setOnAction(e -> restoreSidebar());

        sidebar.getChildren().addAll(statsTitle, infoCard, pie, backBtn);
    }

    private void restoreSidebar() {
        sidebar.getChildren().clear();
        sidebar.getChildren().addAll(sidebarTitle, subjectSearchField, classList,
                newSubjectField, teacherNameField, addSubjectBtn);
    }

    // ==================== Theme & Language ====================
    @FXML
    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        modeToggleBtn.setText(isDarkMode ? tr("☀️ Light Mode", "☀️ 浅色模式") : tr("🌙 Dark Mode", "🌙 深色模式"));
        applyTheme(isDarkMode);
        rebuildTableColumns();
        table.refresh();
    }

    @FXML
    private void toggleLanguage() {
        isChinese = !isChinese;
        translateBtn.setText(isChinese ? "🌐 English" : "🌐 中文");
        refreshLanguage();
        rebuildTableColumns();
    }

    private void applyTheme(boolean dark) {
        String bg = dark ? "#0f172a" : "#D8E2DC";
        String panel = dark ? "#1e293b" : "#E5ECE9";
        String text = dark ? "#ffffff" : "#111827";

        root.setStyle("-fx-background-color: " + bg + ";");
        sidebar.setStyle("-fx-background-color: " + panel + ";");
        topBar.setStyle("-fx-background-color: " + panel + ";");
        centerContainer.setStyle("-fx-background-color: " + bg + ";");
        middleSubjectHeader.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + text + ";");
        middleTeacherHeader.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (dark ? "#9ca3af" : "#334155") + ";");
        sidebarTitle.setStyle("-fx-text-fill: " + text + "; -fx-font-weight: bold; -fx-font-size: 11px;");
        enrollLabel.setStyle("-fx-text-fill: " + text + "; -fx-font-weight: bold; -fx-font-size: 11px;");
    }

    private void refreshLanguage() {
        sidebarTitle.setText(tr("COURSE MODULES", "课程模块"));
        subjectSearchField.setPromptText(tr("🔍 Filter subjects...", "🔍 筛选科目..."));
        newSubjectField.setPromptText(tr("New subject name...", "新科目名称..."));
        teacherNameField.setPromptText(tr("Instructor / Teacher name...", "授课教师姓名..."));
        addSubjectBtn.setText(tr("＋ Create Subject", "＋ 创建科目"));
        enrollLabel.setText(tr("ENROLL:", "注册:"));
        idField.setPromptText(tr("Student ID", "学生编号"));
        nameField.setPromptText(tr("Student Full Name", "学生全名"));
        addStudentBtn.setText(tr("＋ Add Student", "＋ 添加学生"));
        searchField.setPromptText(tr("🔍 Quick search record...", "🔍 快速搜索记录..."));
        modeToggleBtn.setText(isDarkMode ? tr("☀️ Light Mode", "☀️ 浅色模式") : tr("🌙 Dark Mode", "🌙 深色模式"));
        copyAllBtn.setText(tr("📋 Copy All", "📋 复制全部"));
        pasteAllBtn.setText(tr("📋 Paste All", "📋 粘贴全部"));

        String selected = classList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            middleSubjectHeader.setText(isChinese && selected.equals("Comprehensive Design of Data Structures")
                    ? "数据结构综合课程设计" : selected);
            middleTeacherHeader.setText(tr("Instructor: ", "讲师: ") + subjectTeacherMap.getOrDefault(selected, "TBA"));
        }
        classList.refresh();
    }

    // ==================== Zoom ====================
    @FXML
    private void zoomOut() {
        double scale = scaleTransform.getX();
        if (scale > 0.5) {
            scale = Math.max(0.5, scale - 0.1);
            scaleTransform.setX(scale);
            scaleTransform.setY(scale);
            zoomResetBtn.setText(Math.round(scale * 100) + "%");
        }
    }

    @FXML
    private void zoomIn() {
        double scale = scaleTransform.getX();
        if (scale < 1.8) {
            scale = Math.min(1.8, scale + 0.1);
            scaleTransform.setX(scale);
            scaleTransform.setY(scale);
            zoomResetBtn.setText(Math.round(scale * 100) + "%");
        }
    }

    @FXML
    private void zoomReset() {
        scaleTransform.setX(1.0);
        scaleTransform.setY(1.0);
        zoomResetBtn.setText("100%");
    }

    // ==================== Copy / Paste ====================
    @FXML
    private void handleCopyAll() {
        String selected = classList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        ObservableList<StudentAttendance> list = subjectDataMap.get(selected);
        clipboardData = new ArrayList<>();
        for (StudentAttendance s : list) {
            Map<Integer, Integer> weeks = new HashMap<>();
            for (int i = 1; i <= totalConfiguredWeeks; i++) {
                weeks.put(i, s.weekProperty(i).get());
            }
            clipboardData.add(new CopiedStudentData(s.getStudentId(), s.getStudentName(), weeks));
        }
        showAlert(tr("Copied " + clipboardData.size() + " students.", "已复制 " + clipboardData.size() + " 名学生。"));
    }

    @FXML
    private void handlePasteAll() {
        String selected = classList.getSelectionModel().getSelectedItem();
        if (selected == null || clipboardData == null || clipboardData.isEmpty()) {
            showAlert(tr("Nothing to paste.", "没有可粘贴的内容。"));
            return;
        }
        ObservableList<StudentAttendance> list = subjectDataMap.get(selected);
        int added = 0;
        for (CopiedStudentData data : clipboardData) {
            boolean exists = list.stream().anyMatch(s -> s.getStudentId().equalsIgnoreCase(data.id));
            if (!exists) {
                StudentAttendance s = new StudentAttendance(data.id, data.name, totalConfiguredWeeks);
                for (Map.Entry<Integer, Integer> e : data.weekStates.entrySet()) {
                    s.weekProperty(e.getKey()).set(e.getValue());
                }
                list.add(s);
                added++;
            }
        }
        showAlert(tr("Pasted " + added + " students.", "已粘贴 " + added + " 名学生。"));
    }

    // ==================== Import / Export ====================
    @FXML private void importWord() { importData("Word"); }
    @FXML private void importExcel() { importData("Excel"); }
    @FXML private void importCsv() { importData("CSV"); }
    @FXML private void importPdf() { importData("PDF"); }

    @FXML private void exportWord() { exportData("Word"); }
    @FXML private void exportExcel() { exportData("Excel"); }
    @FXML private void exportCsv() { exportData("CSV"); }
    @FXML private void exportPdf() { exportData("PDF"); }

    private void exportData(String type) {
        String selected = classList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(tr("Please select a subject.", "请选择科目。"));
            return;
        }
        ObservableList<StudentAttendance> students = subjectDataMap.get(selected);
        if (students.isEmpty()) {
            showAlert(tr("No data to export.", "没有可导出的数据。"));
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export " + type);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName(selected.replaceAll("[^a-zA-Z0-9]", "_") + "_attendance.csv");
        File file = chooser.showSaveDialog(root.getScene().getWindow());
        if (file == null) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            StringBuilder header = new StringBuilder("Student Name,Student ID,Attendance %");
            for (int i = 1; i <= totalConfiguredWeeks; i++) header.append(",W").append(i);
            writer.write(header.toString());
            writer.newLine();

            for (StudentAttendance s : students) {
                StringBuilder row = new StringBuilder();
                row.append("\"").append(s.getStudentName()).append("\",");
                row.append("\"").append(s.getStudentId()).append("\",");
                row.append(String.format("%.2f", s.getAttendancePercentage()));
                for (int i = 1; i <= totalConfiguredWeeks; i++) {
                    row.append(",").append(s.weekProperty(i).get());
                }
                writer.write(row.toString());
                writer.newLine();
            }
            showAlert(tr("Exported successfully!", "导出成功！"));
        } catch (IOException e) {
            showAlert("Error: " + e.getMessage());
        }
    }

    private void importData(String type) {
        String selected = classList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(tr("Please select a subject first.", "请先选择科目。"));
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import " + type);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV / Text", "*.csv", "*.txt"));
        File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) return;

        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); // skip header
            ObservableList<StudentAttendance> list = subjectDataMap.get(selected);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                if (tokens.length >= 2) {
                    String name = tokens[0].replaceAll("^\"|\"$", "").trim();
                    String id = tokens[1].replaceAll("^\"|\"$", "").trim();
                    if (!id.isEmpty() && !name.isEmpty()) {
                        boolean exists = list.stream().anyMatch(s -> s.getStudentId().equalsIgnoreCase(id));
                        if (!exists) {
                            StudentAttendance s = new StudentAttendance(id, name.toUpperCase(), totalConfiguredWeeks);
                            for (int i = 1; i <= totalConfiguredWeeks; i++) {
                                int idx = 2 + i;
                                if (idx < tokens.length) {
                                    try {
                                        int val = Integer.parseInt(tokens[idx].replaceAll("^\"|\"$", "").trim());
                                        if (val >= 0 && val <= 2) s.weekProperty(i).set(val);
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                            list.add(s);
                            count++;
                        }
                    }
                }
            }
            showAlert(tr("Imported " + count + " students.", "成功导入 " + count + " 名学生。"));
        } catch (IOException e) {
            showAlert("Error: " + e.getMessage());
        }
    }
}