/*
 * Copyright (c) 2019 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved.
 */

package bank.gui;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bank.Account;
import bank.Bank;
import bank.BankDriver;
import bank.BankDriver2;
import bank.InactiveException;
import bank.OverdrawException;
import bank.gui.tests.BankTest;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * Class ClientFX is used to start the client side of the bank application. As a
 * runtime parameter the name of the class which implements the
 * <code>BankDriver</code> interface has to be specified. This class is then
 * loaded and used to access the bank. This class needs a public constructor.
 *
 * <pre>
 * Usage: java bank.ClientFX &lt;classname&gt;
 * </pre>
 *
 * E.g. start the application with one of the following commands. The additional
 * runtime arguments are passed to the connect method of the BankDriver
 * implementation.
 *
 * <pre>
 * java bank.ClientFX bank.local.Driver
 * java bank.ClientFX bank.sockets.Driver localhost 1234
 * </pre>
 *
 * If this program is started without arguments, then a list with runtime arguments is shown.
 *
 * @see BankDriver
 * @author Dominik Gruntz
 * @version 3.0
 */
public class Client {

	public static void main(String args[]) {
		new JFXPanel(); // this will prepare JavaFX toolkit and environment
		var client = new Client();
		if (args.length > 0) {
			Platform.runLater(() -> {
				client.startBank(new Stage(), args);
			});
		} else {
			client.startDriverSelector();
		}
	}

	// Primary object store
	private ObservableMap<String, Account> accounts = FXCollections.observableHashMap();

	// Presentation Model
	private IntegerProperty accountsSize = new SimpleIntegerProperty(0);

	private StringProperty selectedNumberProp = new SimpleStringProperty(null);
	private StringProperty otherSelectedNumberProp = new SimpleStringProperty(null);
	// Account details
	private StringProperty ownerProp = new SimpleStringProperty(null);
	private ObjectProperty<Double> balanceProp = new SimpleObjectProperty<>(null);

	// Accounts overview
	private ObservableList<String> accountNumbers = FXCollections.observableArrayList();
	private SortedList<String> sortedAccountNumbers = accountNumbers.sorted();
	private FilteredList<String> otherNumbers = sortedAccountNumbers.filtered(a -> !a.equals(selectedNumberProp.getValue()));

	private ObjectProperty<Double> amountProp = new SimpleObjectProperty<>(null);

	private void startDriverSelector() {

		Platform.runLater(() -> {
			final Stage stage = new Stage();
			stage.setTitle("Choose Bank Driver");

			ObservableList<String> data = FXCollections.observableArrayList();
			try {
				URI uri = this.getClass().getResource("/Servers.txt").toURI();
				Files.lines(Paths.get(uri)).forEach(line -> data.add(line));
			} catch (Exception e) {
				e.printStackTrace();
			}

			final ListView<String> listView = new ListView<>(data);
			listView.setOnMouseClicked(e -> {
				if (e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2) {
					String configStr = listView.getSelectionModel().getSelectedItem();
					startBank(stage, configStr.split("\\s"));
				}
			});

			StackPane root = new StackPane(listView);
			stage.setScene(new Scene(root, 500, 600));
			stage.show();
		});


	}

	private BankDriver driver = null;
	private Bank bank = null;

	private void startBank(Stage stage, String[] config) {
		stage.setTitle("ClientBank Application");

		driver = DriverFactory.getDriver(config);
		bank = driver.getBank();

		Label accountsLabel = new Label("Accounts");
		accountsLabel.setStyle("-fx-font-size: 18px;");
		ListView<String> numbersList = new ListView<>(sortedAccountNumbers);
		VBox left = new VBox(10, accountsLabel, numbersList);
		left.setPadding(new Insets(10));

		GridPane details = new GridPane();
		details.setHgap(10);
		details.setVgap(10);
		details.setPadding(new Insets(10));

		Label numberLabel = new Label("Account Nr:");
		TextField number = new TextField();
		number.setEditable(false);
		details.add(numberLabel, 0, 0);
		details.add(number, 1, 0);

		Label ownerLabel = new Label("Owner:");
		TextField owner = new TextField();
		owner.setEditable(false);
		details.add(ownerLabel, 0, 1);
		details.add(owner, 1, 1);

		Label balanceLabel = new Label("Balance:");
		TextField balance = new TextField("");
		balance.setEditable(false);
		details.add(balanceLabel, 0, 2);
		details.add(balance, 1, 2);

		Label actionsLabel = new Label("Actions");
		details.add(actionsLabel, 0, 3);

		Button btnDeposit = new Button("Deposit");
		Button btnWithdraw = new Button("Withdraw");
		HBox actions = new HBox(btnDeposit, btnWithdraw);
		actions.setSpacing(10);
		TextField amount = new TextField("");
		amount.setTextFormatter(new TextFormatter<Double>(new StringDoubleConverter(), 0d, new DoubleFilter()));

		details.add(actions, 0, 4);
		details.add(amount, 1, 4);

		Button btnTransfer = new Button("Transfer to");

		ChoiceBox<String> otherNumbersList = new ChoiceBox<>(otherNumbers);
		otherNumbersList.setMaxWidth(Double.POSITIVE_INFINITY);
		details.add(otherNumbersList, 1, 5);
		details.add(btnTransfer, 0, 5);

		Button btnClose = new Button("Close");
		details.add(btnClose, 0, 6);

		Label header = new Label("Current Account");
		header.setStyle("-fx-font-size: 18px;");

		VBox center = new VBox(10, header, details);
		center.setPadding(new Insets(10));

		////////////////////// Bindings
		accounts.addListener(new MapChangeListener<String, Account> (){
			@Override
			public void onChanged(Change<? extends String, ? extends Account> change) {
				if (change.wasAdded()) {
					accountNumbers.add(change.getKey());
				} else if (change.wasRemoved()) {
					accountNumbers.remove(change.getKey());
				}
				accountsSize.set(change.getMap().size());
			}
		});

		selectedNumberProp.addListener((obs, oldV, newV) -> {
			Account selectedAccount = accounts.getOrDefault(newV, null);
			// Update detail view
			if (selectedAccount != null) {
				try {
					ownerProp.set(selectedAccount.getOwner());
					balanceProp.set(selectedAccount.getBalance());
					numbersList.getSelectionModel().select(newV);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} else {
				ownerProp.set(null);
				balanceProp.set(null);
			}
			// Update otherNumbers https://bugs.openjdk.java.net/browse/JDK-8090770
			otherNumbers.setPredicate(a -> !a.equals(selectedNumberProp.getValue()));
			amountProp.set(null);
			// Remove transfer to account
			otherSelectedNumberProp.set(null);
		});

		otherSelectedNumberProp.addListener((obs, oldV, newV) -> otherNumbersList.getSelectionModel().select(newV));

		numbersList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> selectedNumberProp.set(newV));
		otherNumbersList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> otherSelectedNumberProp.set(newV));

		number.textProperty().bind(selectedNumberProp);
		owner.textProperty().bind(ownerProp);
		//
		balance.textProperty().bindBidirectional(balanceProp, new StringDoubleConverter());
		amount.textProperty().bindBidirectional(amountProp, new StringDoubleConverter());

		btnWithdraw.setOnAction(e -> withdraw(selectedNumberProp.get(), amountProp.get()));
		btnWithdraw.disableProperty().bind(selectedNumberProp.isNull().or(amountProp.isNull()));

		btnDeposit.setOnAction(e -> deposit(selectedNumberProp.get(), amountProp.get()));
		btnDeposit.disableProperty().bind(selectedNumberProp.isNull().or(amountProp.isNull()));

		btnClose.setOnAction(e -> closeAccount(selectedNumberProp.get()));
		btnClose.disableProperty().bind(selectedNumberProp.isNull());

		btnTransfer.setOnAction(e ->
			transfer(selectedNumberProp.get(), otherNumbersList.getSelectionModel().getSelectedItem(), amountProp.get()));
		btnTransfer.disableProperty().bind(otherSelectedNumberProp.isNull().or(amountProp.isNull()));


		Node bottom = null;
		if (driver instanceof BankDriver2) {
			try {
				((BankDriver2) driver).registerUpdateHandler(id -> Platform.runLater(this::refreshPresentationModel));
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
		} else {
			Button refresh = new Button("Refresh");
			refresh.setMaxWidth(Double.MAX_VALUE);
			refresh.setOnAction(e -> refreshPresentationModel());
			bottom = refresh;
		}

		MenuBar menuBar = new MenuBar(createFileMenu(), createTestMenu(), createHelpMenu());
		BorderPane root = new BorderPane(center, menuBar, null, bottom, left);

		Scene scene = new Scene(root, 640, 480);
		stage.setScene(scene);
		stage.show();

		refreshPresentationModel();
	}

	public void stop() {
		try {
			if(driver != null) driver.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Menu createFileMenu() {
		MenuItem openProjectMenuItem = new MenuItem("New Account...");
		openProjectMenuItem.setOnAction(e -> newAccount());
		MenuItem generateAccounts = new MenuItem("GenerateAccounts");
		generateAccounts.setOnAction(e -> createAccounts());
		MenuItem closeAccount = new MenuItem("Close Account");
		closeAccount.disableProperty().bind(selectedNumberProp.isNull());
		closeAccount.setOnAction(e -> closeAccount(selectedNumberProp.get()));
		MenuItem exitMenuItem = new MenuItem("Exit");
		exitMenuItem.setOnAction(e -> Platform.exit());
		return new Menu("File", null, openProjectMenuItem, generateAccounts, closeAccount, new SeparatorMenuItem(), exitMenuItem);
	}

	private Menu createTestMenu() {
		List<BankTest> tests = Stream.of(
			"bank.gui.tests.EfficiencyTestDS",
			"bank.gui.tests.EfficiencyTestCONPR",
			"bank.gui.tests.WarmUp",
			"bank.gui.tests.ThreadingTest",
			"bank.gui.tests.FunctionalityTest",
			"bank.gui.tests.TransferTest",
			"bank.gui.tests.ConcurrentReads",
			"bank.gui.tests.PerformanceTest"
		).map(Client::loadTest)
		 .filter(t -> t != null)
		 .collect(Collectors.toList());

		Menu menu = new Menu("Test");

		for (final BankTest t : tests) {
			MenuItem m = new MenuItem(t.getName());
			menu.getItems().add(m);

			m.disableProperty().bind(Bindings.createBooleanBinding(() -> t.isEnabled(accountsSize.get()), accountsSize).not());

			m.setOnAction(e -> {
				try {
					System.out.println("run test " + t.getName());
					String msg = t.runTests(bank, selectedNumberProp.get());
					if (msg != null) {
						showInfo("Test Results", msg);
					}
					refreshPresentationModel();
				} catch (Exception ex) {
					showError("Exception in Test", ex);
				}
			});
		}

		return menu;
	}

	private static BankTest loadTest(String name) {
		try {
			return (BankTest) Class.forName(name).getConstructor().newInstance();
		} catch (Exception e) {
			return null;
		}
	}

	Menu createHelpMenu() {
		Menu menu = new Menu("Help");
		MenuItem about = new MenuItem("About...");
		menu.getItems().add(about);
		about.setOnAction(e ->
			showInfo("About Bank Client", "Distributed Systems BankClient\n\n© D. Gruntz & D. Kröni, 2019")
		);
		return menu;
	}

	private void showInfo(String title, String content) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	private void showError(String content) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	public void showError(String title, Exception ex) {
		ex.printStackTrace(System.err);

		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle(title);
		alert.setHeaderText(ex.getMessage());

		// Create expandable Exception.
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		String exceptionText = sw.toString();

		Label label = new Label("The exception stacktrace was:");

		TextArea textArea = new TextArea(exceptionText);
		textArea.setEditable(false);
		textArea.setWrapText(true);

		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);

		GridPane expContent = new GridPane();
		expContent.setMaxWidth(Double.MAX_VALUE);
		expContent.add(label, 0, 0);
		expContent.add(textArea, 0, 1);

		// Set expandable Exception into the dialog pane.
		alert.getDialogPane().setExpandableContent(expContent);

		alert.showAndWait();
	}

	private void newAccount() {
		TextInputDialog dialog = new TextInputDialog("");
		dialog.setTitle("Create Account");
		dialog.setHeaderText(null);
		dialog.setContentText("Please enter your name:");

		Optional<String> result = dialog.showAndWait();
		result.ifPresent(name -> createAccount(name, 0.0));
	}

	private void createAccount(String name, double initialBalance) {
		try {
			String number = bank.createAccount(name);
			if (number == null) {
				showError("Account could not be created for " + name);
			} else {
				Account a = bank.getAccount(number);
				accounts.put(number, a);
				if(initialBalance > 0) {
					deposit(number, initialBalance);
				}
				selectedNumberProp.set(number);
			}
		} catch (IOException ex) {
			showError("Could not create an account for " + name, ex);
		}
	}

	private void createAccounts() {
		double amount = 200;
		for (int i = 0; i < 5; i++) {
			createAccount("Testaccount " + i, amount);
		}
	}

	private void closeAccount(String number) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Close Account");
		alert.setHeaderText(null);
		alert.setContentText("Do you really want to close the account " + number + "?");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK) {
			try {
				boolean done = bank.closeAccount(number);
				if (done) {
					accounts.remove(number);
					if(accounts.size() >0) {
						selectedNumberProp.set(accountNumbers.get(0));
					}
				} else {
					showError("Account could not be closed");
				}
			} catch (Exception e) {
				showError("Account could not be closed", e);
			}
		}
	}

	private void deposit(String number, double amount) {
		try {
			Account a = accounts.get(number);
			a.deposit(amount);
			balanceProp.set(a.getBalance());
		} catch (NumberFormatException e) {
			showError("Illegal Value");
		} catch (IllegalArgumentException e) {
			showError("Illegal Argument");
		} catch (InactiveException e) {
			showError("Account is inactive");
		} catch (Exception e) {
			showError("Amount could not be deposited", e);
		}
	}

	public void withdraw(String number, double amount) {
		try {
			Account a = accounts.get(number);
			a.withdraw(amount);
			balanceProp.set(a.getBalance());
		} catch (IllegalArgumentException e) {
			showError("Illegal Argument");
		} catch (InactiveException e) {
			showError("Account is inactive");
		} catch (OverdrawException e) {
			showError("Account must not be overdrawn");
		} catch (Exception e) {
			showError("Amount could not be deposited", e);
		}
	}

	public void transfer(String fromNumber, String toNumber, double amount) {
		try {
			Account from = accounts.get(fromNumber);
			Account to = accounts.get(toNumber);
			bank.transfer(from, to, amount);
			balanceProp.set(from.getBalance());
		} catch (IllegalArgumentException e) {
			showError("Illegal Argument");
		} catch (InactiveException e) {
			showError("Account is inactive");
		} catch (OverdrawException e) {
			showError("Account must not be overdrawn");
		} catch (Exception e) {
			showError("Amount could not be deposited", e);
		}
	}

	private void refreshPresentationModel() {
		// Sync local account number store with bank internal data
		try {
			Set<String> s = bank.getAccountNumbers();
			for (String key : s) {
				if (!accounts.containsKey(key)) {
					accounts.put(key, bank.getAccount(key));
				}
			}
			accounts.keySet().removeIf(id -> !s.contains(id));
		} catch (IOException e) {
			showError("Account numbers could not be accessed", e);
		}

		// Update selection
		String selectedAccount = selectedNumberProp.get();

		if (!accounts.containsKey(selectedAccount)) {
			selectedAccount = null;
		}

		if (selectedAccount == null && accounts.size() > 0) {
			selectedAccount = accountNumbers.get(0);
		}

		selectedNumberProp.set(selectedAccount);
		if(selectedAccount != null) {
			try {
				balanceProp.set(accounts.get(selectedAccount).getBalance());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}


	static private final class DoubleFilter implements UnaryOperator<Change> {
		@Override
		public Change apply(Change change) {
			String newText = change.getControlNewText();
		    if(newText.trim().isEmpty()) {
		    		change.setText("");
		    		return change;
		    }
		    try {
		    		Double.parseDouble(newText);
		    		return change;
		    } catch (NumberFormatException e) {
			    return null;
			}
		}
	}

	static private final class StringDoubleConverter extends StringConverter<Double> {
		@Override
		public Double fromString(String s) {
			return s.trim().isEmpty() ? null: Double.parseDouble(s);
		}

		@Override
		public String toString(Double d) {
			return d == null ? "" : Double.toString(d);
		}
	}

}