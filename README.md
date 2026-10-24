# Attendance Sheet Management System

A desktop attendance management application built with **Java + JavaFX (FXML)**.  
Final project for *Comprehensive Design of Data Structures*.

![Java](https://img.shields.io/badge/Java-17+-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)
![FXML](https://img.shields.io/badge/UI-FXML-green)

---

## Features

- Multiple course modules / subjects
- Student enrolment (ID + Name)
- 10-week attendance tracking (click cell to cycle: Unmarked → Present ✔ → Absent ✖)
- Automatic attendance percentage calculation with colour coding
- Real-time search (subjects & students)
- Dark / Light theme toggle
- English / Chinese language switch
- Student statistics view with Pie Chart (triple-click a row)
- CSV Import / Export
- Copy All / Paste All students between subjects

---

## Data Structures Used

| Data Structure     | Purpose                                      | Complexity      |
|--------------------|----------------------------------------------|-----------------|
| **ObservableList** | Subjects list & student lists (UI binding)   | O(1) add/remove |
| **HashMap**        | Subject→Teacher, Subject→Students, Week→State| O(1) average    |
| **FilteredList**   | Real-time search filtering                   | O(n)            |
| **ArrayList**      | Clipboard for Copy/Paste All                 | O(1) / O(n)     |

---

## Algorithm

**Attendance Percentage Calculation**

1. User clicks a week cell → state cycles `(state + 1) % 3`
2. `ChangeListener` on `IntegerProperty` is fired
3. Count how many weeks have state `1` (Present)
4. `percentage = (attended / totalWeeks) * 100`
5. Update `DoubleProperty` → TableView cell colour refreshes

**Time Complexity:** O(W) where W = number of weeks (fixed at 10)

---

## Project Structure
