# soderer-swing

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE.txt)
[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/)

**Ready-to-use Swing dialogs and components for Java desktop applications.**

`soderer-swing` provides the dialogs every desktop application needs again and again — questions, text input,
selections, credentials, tables, error reports and progress display — plus `DropDown`, an autocomplete combo box
with search, validation and drag & drop reordering. All dialogs are modal, return their result directly from
`open()` and come with built-in texts in English, German, French, Spanish and Italian.

---

## Features

| | |
|---|---|
| **Question dialog** | `QuestionDialog` with any number of answer buttons and an optional checkbox ("Don't ask again") |
| **Input dialogs** | `SimpleInputDialog` (one field, optional input pattern), `MultiInputDialog` (several labeled fields) |
| **Selection** | `ComboSelectionDialog` with an autocomplete drop-down |
| **Credentials** | `CredentialsDialog` for username and/or password, optional "remember credentials" checkbox |
| **Tables** | `TableDialog` to show data, `SelectableTableDialog` to let the user pick rows |
| **Text display** | `ShowDataDialog` for longer texts, optionally monospaced and resizable |
| **Error report** | `ErrorDialog` with expandable stack trace and a button to send the error via the standard email client |
| **Progress** | `ProgressDialog` and `DualProgressDialog` (overall + current item) for background workers, with cancel button and ETA |
| **Preferences** | `SecurePreferencesDialog` (encrypted keystore) and `UnsecurePreferencesDialog` (plain file) to manage stored entries |
| **Autocomplete combo** | `DropDown` — see below |

## Requirements

- Java 17 or newer
- [soderer-utilities](https://github.com/hudeany/Utilities) (language texts, worker classes, data stores)

## Installation

### Maven

```xml
<dependency>
	<groupId>de.soderer</groupId>
	<artifactId>soderer-swing</artifactId>
	<version>VERSION</version>
</dependency>
```

### Gradle

```groovy
implementation "de.soderer:soderer-swing:VERSION"
```

### Plain jar

Download `soderer-swing-VERSION.jar` from the releases page and put it on the classpath together with `soderer-utilities`.

## Usage

All dialogs extend `ModalDialog<T>`: create the dialog, configure it, and call `open()`, which blocks until
the dialog is closed and returns its result. The first constructor parameter is the owner window (may be `null`).

### Asking a question

```java
Integer answer = new QuestionDialog(frame, "Delete", "Really delete 3 files?",
		LangResources.get("yes"), LangResources.get("no"))
		.setCheckboxText("Don't ask again")
		.open();

if (answer != null && answer == 0) {
	// "Yes" was clicked (index of the button)
}
```

### Text input

```java
String name = new SimpleInputDialog(frame, "New project", "Project name:")
		.setDefaultText("MyProject")
		.open(); // null if canceled
```

```java
MultiInputDialog dialog = new MultiInputDialog(frame, "Server", "Connection data:", "Host", "Port");
dialog.setDefaultTexts(new String[] { "localhost", "8080" });
List<String> values = dialog.open(); // null if canceled
```

### Selection

```java
String language = new ComboSelectionDialog(frame, "Language", "Please choose:", "Deutsch", "English", "Français")
		.open(); // null if canceled
```

### Credentials

```java
Credentials credentials = new CredentialsDialog(frame, "Login", "Please log in", true, true)
		.setRememberCredentialsText("Remember me")
		.open();
```

### Showing an error

```java
try {
	// ...
} catch (Exception e) {
	new ErrorDialog(frame, "My Application", "1.2.0", "support@example.com", e).open();
}
```

The dialog shows the error message, expands to the full stack trace on demand and opens a prepared email
in the user's standard email client. Without a recipient, the email button is hidden.

### Progress of a background worker

```java
MyWorker worker = new MyWorker(); // a WorkerSimple from soderer-utilities
Result result = new ProgressDialog<>(frame, "Import", "{0} of {1} records ({2} %)", worker).open();

if (result == Result.OK) {
	// Worker finished successfully
}
```

The dialog starts the worker, shows its progress with speed and estimated end time, and offers a cancel
button. In the comment text, `{0}` is replaced by the items done, `{1}` by the items to do and `{2}` by the percentage. The result is `OK`, `CANCELED` or `ERROR`.

## DropDown

`DropDown` is an autocomplete combo box that can be used in any Swing layout.

```java
DropDown dropDown = new DropDown()
		.withItems("Berlin", "Bremen", "Hamburg", "München")
		.withMatchMode(DropDown.MatchMode.CONTAINS)
		.withCaseSensitive(false)
		.withAllowCustomValues(false)
		.withMessage("City");

dropDown.addActionListener(e -> System.out.println("Selected: " + dropDown.getText()));
dropDown.addValidationListener(valid -> okButton.setEnabled(valid));
```

- Search by `STARTS_WITH` or `CONTAINS`, case-sensitive or not
- The best match is suggested in light gray, Tab or Enter accept it
- Arrow keys, Page up/down navigate through several matches
- Free input can be allowed or forbidden; invalid input is shown in red
- `ActionEvent` only fires when a value was actually accepted, the validation listener whenever the valid state changes
- Optional: reorder entries by drag & drop (`withReorderable`), remove them with the Delete key (`withDeletable`)

## Language texts

Button labels and messages come from `LangResources` of soderer-utilities, which has built-in texts in
English, German, French, Spanish and Italian (other languages fall back to English). The language follows
the JVM's default locale.

An application can override any text by adding its own `LanguageProperties*.properties` files to the
classpath root, e.g. `LanguageProperties_de.properties`:

```properties
ok=Übernehmen
cancel=Verwerfen
```

## Building

The project is built with [Apache Ant](https://ant.apache.org/). Dependencies are downloaded automatically.

```sh
ant build_jar
```

This compiles the sources, runs the tests and creates the following files in `build/`:

- `soderer-swing-VERSION.jar`
- `soderer-swing-VERSION-sources.jar`
- `soderer-swing-VERSION-javadoc.jar`

The version is read from the file `build.version`.

## License

Released under the [MIT License](LICENSE.txt).
