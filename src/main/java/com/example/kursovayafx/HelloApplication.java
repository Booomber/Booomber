package com.example.kursovayafx;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HelloApplication extends Application {

    // Определяем типы телефонов
    public enum PhoneType {
        WORK("Рабочий"),
        MOBILE("Мобильный"),
        HOME("Домашний"),
        FAX("Факс");

        private final String displayName;

        PhoneType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Контакт содержит имя и список телефонов
    public static class Contact implements Serializable {
        private String name;
        private List<Phone> phones;

        // Конструктор контакта
        public Contact(String name, List<Phone> phones) {
            this.name = name;
            this.phones = phones;
        }

        public String getName() {
            return name;
        }

        public List<Phone> getPhones() {
            return phones;
        }

        // Метод для добавления нового телефона
        public void addPhone(String phone, PhoneType type) {
            this.phones.add(new Phone(phone, type));
        }

        @Override
        public String toString() {
            StringBuilder contactInfo = new StringBuilder(name + ": ");
            for (Phone phone : phones) {
                contactInfo.append(phone.getPhone()).append(" (").append(phone.getType().getDisplayName()).append("), ");
            }
            return contactInfo.toString();
        }

        public static class Phone implements Serializable {
            private String phone;
            private PhoneType type;

            // Конструктор телефона
            public Phone(String phone, PhoneType type) {
                this.phone = phone;
                this.type = type;
            }

            public String getPhone() {
                return phone;
            }

            public PhoneType getType() {
                return type;
            }
        }
    }

    private List<Contact> contactsList = new ArrayList<>();
    private ObservableList<Contact> observableContactsList = FXCollections.observableArrayList();
    private static final String DATABASE_FILE = "contacts.dat";
    private double scaleFactor = 1.0;

    @Override
    public void start(Stage stage) throws IOException, ClassNotFoundException {
        stage.setTitle("Телефонный справочник");

        // Загружаем контакты из файла
        loadContactsFromFile();

        // Создание меню
        MenuBar menuBar = createMenuBar(stage);

        // Создание таблицы контактов
        TableView<Contact> contactsTableView = createContactsTable();

        // Поле для поиска контактов
        TextField searchField = createSearchField();

        VBox vbox = new VBox(menuBar, searchField, contactsTableView);
        ScrollPane scrollPane = new ScrollPane(vbox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        Scene scene = new Scene(scrollPane, 600, 800);
        stage.setScene(scene);
        stage.show();
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();
        Menu info = new Menu("Инфо");
        Menu contacts = new Menu("Контакты");
        Menu zoomMenu = new Menu("Масштаб");

        // Создаем элементы меню
        MenuItem author = new MenuItem("Автор");
        MenuItem addContact = new MenuItem("Добавить");
        MenuItem deleteContact = new MenuItem("Удалить");

        MenuItem zoomIn = new MenuItem("Увеличить");
        MenuItem zoomOut = new MenuItem("Уменьшить");

        // Добавляем пункты меню в соответствующие меню
        zoomMenu.getItems().addAll(zoomIn, zoomOut);
        info.getItems().add(author);
        contacts.getItems().add(addContact);
        contacts.getItems().add(deleteContact);
        menuBar.getMenus().add(contacts);
        menuBar.getMenus().add(info);
        menuBar.getMenus().add(zoomMenu);

        // Обработчики событий
        addContact.setOnAction(event -> showAddContactDialog(stage));
        deleteContact.setOnAction(event -> showDeleteContactDialog(stage));
        author.setOnAction(event -> showAuthorDialog(stage));

        zoomIn.setOnAction(event -> zoomIn(stage));
        zoomOut.setOnAction(event -> zoomOut(stage));

        return menuBar;
    }

    private TableView<Contact> createContactsTable() {
        TableView<Contact> contactsTableView = new TableView<>();
        contactsTableView.setItems(observableContactsList);

        // Создаем и добавляем колонку с именем
        TableColumn<Contact, String> nameColumn = createNameColumn();

        // Создаем и добавляем колонку с телефонами
        TableColumn<Contact, String> phoneColumn = createPhoneColumn();

        contactsTableView.getColumns().addAll(nameColumn, phoneColumn);
        contactsTableView.setPrefHeight(700);
        contactsTableView.setStyle("-fx-font-size: 14px;");

        return contactsTableView;
    }

    private TableColumn<Contact, String> createNameColumn() {
        TableColumn<Contact, String> nameColumn = new TableColumn<>("ФИО");
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        nameColumn.setMinWidth(100);
        nameColumn.setPrefWidth(270);
        nameColumn.setMaxWidth(500);
        nameColumn.setStyle("-fx-font-size: 14px;");
        nameColumn.setSortable(true);
        nameColumn.setSortType(TableColumn.SortType.ASCENDING);
        return nameColumn;
    }

    private TableColumn<Contact, String> createPhoneColumn() {
        TableColumn<Contact, String> phoneColumn = new TableColumn<>("Телефоны");
        phoneColumn.setCellValueFactory(cellData -> {
            // Формируем строку с номерами телефонов для вывода в таблицу
            List<String> phoneDisplay = cellData.getValue().getPhones().stream()
                    .map(phone -> phone.getPhone() + " (" + phone.getType().getDisplayName() + ")")
                    .collect(Collectors.toList());
            return new SimpleStringProperty(String.join(", ", phoneDisplay));
        });
        phoneColumn.setMinWidth(150);
        phoneColumn.setPrefWidth(325);
        phoneColumn.setMaxWidth(500);
        phoneColumn.setStyle("-fx-font-size: 14px;");
        return phoneColumn;
    }

    private TextField createSearchField() {
        TextField searchField = new TextField();
        searchField.setPromptText("Поиск по имени или номеру...");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterContacts(newValue));
        return searchField;
    }

    private void filterContacts(String query) {
        if (query == null || query.isEmpty()) {
            observableContactsList.setAll(contactsList);
        } else {
            // Фильтруем список контактов по имени или телефону
            List<Contact> filteredContacts = contactsList.stream()
                    .filter(contact -> contact.getName().toLowerCase().contains(query.toLowerCase()) ||
                            contact.getPhones().stream().anyMatch(phone -> phone.getPhone().contains(query)))
                    .collect(Collectors.toList());
            observableContactsList.setAll(filteredContacts);
        }
    }

    private void zoomIn(Stage stage) {
        scaleFactor *= 1.1;
        updateScale(stage);
    }

    private void zoomOut(Stage stage) {
        scaleFactor *= 0.9;
        updateScale(stage);
    }

    private void updateScale(Stage stage) {
        VBox vbox = (VBox) ((ScrollPane) ((Scene) stage.getScene()).getRoot()).getContent();
        vbox.setScaleX(1);
        vbox.setScaleY(1);
        updateFontSize(vbox);
    }

    private void updateFontSize(VBox vbox) {
        double baseFontSize = 14.0;
        double newFontSize = baseFontSize * scaleFactor;

        // Обновляем размер шрифта для всех элементов
        for (javafx.scene.Node node : vbox.getChildren()) {
            if (node instanceof Label) {
                ((Label) node).setStyle("-fx-font-size: " + newFontSize + "px;");
            } else if (node instanceof Button) {
                ((Button) node).setStyle("-fx-font-size: " + newFontSize + "px;");
            } else if (node instanceof TableView) {
                TableView<?> tableView = (TableView<?>) node;
                for (TableColumn<?, ?> column : tableView.getColumns()) {
                    column.setStyle("-fx-font-size: " + newFontSize + "px;");
                }
                tableView.setStyle("-fx-font-size: " + newFontSize + "px;");
                for (TableColumn<?, ?> column : tableView.getColumns()) {
                    column.setMinWidth(150 * scaleFactor);
                    column.setPrefWidth(330 * scaleFactor);
                    column.setMaxWidth(500 * scaleFactor);
                }
                tableView.setFixedCellSize(40 * scaleFactor);
            } else if (node instanceof TextField) {
                ((TextField) node).setStyle("-fx-font-size: " + newFontSize + "px;");
            } else if (node instanceof ComboBox) {
                ((ComboBox<?>) node).setStyle("-fx-font-size: " + newFontSize + "px;");
            } else if (node instanceof VBox) {
                updateFontSize((VBox) node);
            }
        }
    }

    private void showAddContactDialog(Stage stage) {
        Dialog<Contact> adddialog = new Dialog<>();
        adddialog.setTitle("Добавить контакт");

        TextField nameField = new TextField();
        nameField.setPromptText("ФИО");

        TextField phoneField = new TextField();
        phoneField.setPromptText("Телефон");

        // Создаем маску для ввода только цифр
        Pattern pattern = Pattern.compile("[0-9+]*");
        phoneField.setTextFormatter(new TextFormatter<>(change -> pattern.matcher(change.getText()).matches() ? change : null));

        ComboBox<PhoneType> phoneTypeComboBox = new ComboBox<>();
        phoneTypeComboBox.getItems().setAll(PhoneType.values());
        phoneTypeComboBox.setValue(PhoneType.MOBILE);

        Button addPhoneButton = new Button("Добавить номер");
        VBox phonesBox = new VBox(10, phoneField, phoneTypeComboBox, addPhoneButton);

        VBox vbox = new VBox(10, new Label("Имя:"), nameField, new Label("Телефон:"), phonesBox);
        adddialog.getDialogPane().setContent(vbox);

        ListView<String> addedPhonesListView = new ListView<>();
        addedPhonesListView.setEditable(false);
        VBox phoneListBox = new VBox(10, new Label("Добавленные номера:"), addedPhonesListView);

        ButtonType addButton = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeButton = new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE);
        adddialog.getDialogPane().getButtonTypes().addAll(addButton, closeButton);

        List<Contact.Phone> phoneNumbers = new ArrayList<>();

        // Добавление телефона в список
        addPhoneButton.setOnAction(e -> {
            String phone = phoneField.getText();
            PhoneType type = phoneTypeComboBox.getValue();
            if (!phone.isEmpty()) {
                phoneNumbers.add(new Contact.Phone(phone, type));
                addedPhonesListView.getItems().setAll(phoneNumbers.stream()
                        .map(p -> p.getPhone() + " (" + p.getType().getDisplayName() + ")")
                        .toList());
                phoneField.clear();
            }
        });

        // Добавление контакта в список
        adddialog.setResultConverter(button -> {
            if (button == addButton) {
                if (!nameField.getText().isEmpty() && !phoneNumbers.isEmpty()) {
                    Contact newContact = new Contact(nameField.getText(), phoneNumbers);
                    contactsList.add(newContact);
                    observableContactsList.setAll(contactsList);
                    saveContactsToFile();
                }
            }
            return null;
        });

        adddialog.showAndWait();
    }

    private void showDeleteContactDialog(Stage stage) {
        Dialog<Void> deldialog = new Dialog<>();
        deldialog.setTitle("Удалить контакт");

        ListView<Contact> contactsListView = new ListView<>();
        contactsListView.getItems().addAll(contactsList);

        contactsListView.setCellFactory(param -> new ListCell<Contact>() {
            @Override
            protected void updateItem(Contact contact, boolean empty) {
                super.updateItem(contact, empty);
                if (empty || contact == null) {
                    setText(null);
                } else {
                    setText(contact.toString());
                }
            }
        });

        ButtonType delButton = new ButtonType("Удалить", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeButton = new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE);
        deldialog.getDialogPane().getButtonTypes().addAll(delButton, closeButton);

        VBox vbox = new VBox(10, new Label("Выберите контакт для удаления:"), contactsListView);
        deldialog.getDialogPane().setContent(vbox);

        deldialog.setResultConverter(button -> {
            if (button == delButton) {
                Contact selectedContact = contactsListView.getSelectionModel().getSelectedItem();
                if (selectedContact != null) {
                    contactsList.remove(selectedContact);
                    observableContactsList.setAll(contactsList);
                    saveContactsToFile();
                }
            }
            return null;
        });

        deldialog.showAndWait();
    }

    private void showAuthorDialog(Stage stage) {
        Alert authorAlert = new Alert(Alert.AlertType.INFORMATION);
        authorAlert.setTitle("Информация об авторе");
        authorAlert.setHeaderText(null);
        authorAlert.setContentText("Это создал Абдуллаев Даниил из ИБС-31 !!!!!!!! Чтооооо жесть!");

        authorAlert.showAndWait();
    }

    private void loadContactsFromFile() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATABASE_FILE))) {
            contactsList = (List<Contact>) ois.readObject();
            observableContactsList.setAll(contactsList);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveContactsToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATABASE_FILE))) {
            oos.writeObject(contactsList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
