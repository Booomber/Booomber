module com.example.kursovayafx {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.kursovayafx to javafx.fxml;
    exports com.example.kursovayafx;
}