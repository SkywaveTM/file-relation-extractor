package gui;

import collect.GitRevisionCollector;
import collect.RevisionCollector;
import com.sun.javafx.collections.ObservableListWrapper;
import command.CommandExecutor;
import command.CommandOptions;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.apache.tools.ant.types.Commandline;
import utils.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class CommandBuilderController {
    private CommandExecutor executor;

    /* UI */
    public GridPane optionGrid;
    public BorderPane root;
    public TitledPane gitSettings;
    public Button extractButton;
    public Button buildButton;
    public ListView<String> outputList;
    public TextField argumentsField;
    public CheckBox debug;

    /* COLLECT */
    public DatePicker fromDate;
    public DatePicker toDate;
    public TextField svnUrl;
    public TextField gitTempDir;
    public CheckBox preserveTempDir;
    public TextField allowedExtensions;
    public ComboBox<String> branchComboBox;

    /* MERGE */
    public RadioButton mergingWindow;
    public RadioButton mergingDuplicatedWindow;
    public RadioButton mergingDistance;
    public RadioButton mergingNone;
    public TextField commitLimit;
    public TextField windowSize;
    public CheckBox ignoreSingleFile;
    public TextField maxDistinctPackage;

    /* EXPORT */
    public RadioButton exportCsv;
    public RadioButton exportSqlite;
    public RadioButton namingNumbering;
    public RadioButton namingOverriding;
    public RadioButton namingError;
    public TextField exportFileName;


    private boolean running = false;
    private boolean gitLocal = false;
    private CommandOptions.UrlType urlType = CommandOptions.UrlType.INVALID;

    public CommandBuilderController() {
        // Prints log messages to the list view.
        Logger.INSTANCE.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                addMessageBackground(Logger.formatLogRecord(record));
            }

            @Override
            public void flush() {

            }

            @Override
            public void close() throws SecurityException {

            }
        });

        executor = new CommandExecutor();
    }

    @FXML
    public void initialize() {
        svnUrl.textProperty().addListener((observable) -> {
            setUrlType(CommandOptions.UrlType.INVALID);
        });

        updateUi();
    }

    public void handleBuildButtonAction() {
        StringBuilder arguments = new StringBuilder();

        if (namingNumbering.isSelected()) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_DUPLICATED_FILE_HANDLING);
            appendArgument(arguments, CommandOptions.DuplicatedHandlingMethod.Numbering.name());
        } else if (namingOverriding.isSelected()) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_DUPLICATED_FILE_HANDLING);
            appendArgument(arguments, CommandOptions.DuplicatedHandlingMethod.Override.name());
        } else if (namingError.isSelected()) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_DUPLICATED_FILE_HANDLING);
            appendArgument(arguments, CommandOptions.DuplicatedHandlingMethod.Error.name());
        }

        if (mergingWindow.isSelected()) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_MERGE_METHOD);
            appendArgument(arguments, CommandOptions.MergeMethod.Window.name());
        } else if (mergingDuplicatedWindow.isSelected()) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_MERGE_METHOD);
            appendArgument(arguments, CommandOptions.MergeMethod.DuplicatedWindow.name());
        } else if (mergingDistance.isSelected()) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_MERGE_METHOD);
            appendArgument(arguments, CommandOptions.MergeMethod.Distance.name());
        } else if (mergingNone.isSelected()) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_MERGE_METHOD);
            appendArgument(arguments, CommandOptions.MergeMethod.NoMerge.name());
        }

        if (fromDate.getValue() != null) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_FROM);
            appendArgument(arguments, String.format("%s 00:00:01", fromDate.getValue().toString()));
        }

        if (toDate.getValue() != null) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_TO);
            appendArgument(arguments, String.format("%s 23:59:59", toDate.getValue().toString()));
        }

        if (!commitLimit.getText().isEmpty()) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_LIMIT);
            appendArgument(arguments, commitLimit.getText());
        }

        if (!windowSize.getText().isEmpty()) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_SLIDING_WINDOW_SIZE);
            appendArgument(arguments, windowSize.getText());
        }

        if (!maxDistinctPackage.getText().isEmpty()) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_MAX_DISTINCT_PACKAGES);
            appendArgument(arguments, maxDistinctPackage.getText());
        }

        if (!allowedExtensions.getText().isEmpty()) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_ALLOWED_EXTENSIONS);
            for (String extension : allowedExtensions.getText().split(" ")) {
                appendArgument(arguments, extension);
            }
        }

        if (!exportFileName.getText().isEmpty()) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_EXPORT_FILE_NAME);
            appendArgument(arguments, exportFileName.getText());
        }

        if (exportCsv.isSelected()) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_EXPORT_TYPE);
            appendArgument(arguments, CommandOptions.ExportType.CSV.name());
        } else if (exportSqlite.isSelected()) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_EXPORT_TYPE);
            appendArgument(arguments, CommandOptions.ExportType.SQLITE.name());
        }

        if (ignoreSingleFile.isSelected()) {
            appendArgument(arguments, CommandOptions.PARAMETER_SHORT_IGNORE_SINGLE_FILE_GROUP);
        }

        if (urlType == CommandOptions.UrlType.GIT) {
            if (branchComboBox.getValue() != null) {
                appendArgument(arguments, CommandOptions.PARAMETER_SHORT_BRANCH);
                appendArgument(arguments, branchComboBox.getValue());
            }

            if (!gitLocal) {
                if (!gitTempDir.getText().isEmpty()) {
                    appendArgument(arguments, CommandOptions.PARAMETER_SHORT_GIT_TEMP_DIR);
                    appendArgument(arguments, gitTempDir.getText());
                }

                if (preserveTempDir.isSelected()) {
                    appendArgument(arguments, CommandOptions.PARAMETER_SHORT_PRESERVE_GIT_TEMP);
                }
            }
        }

        if (debug.isSelected()) {
            appendArgument(arguments, CommandOptions.PARAMETER_FULL_DEBUG);
        }

        appendArgument(arguments, svnUrl.getText());

        argumentsField.setText(arguments.toString());
    }

    public void handleExtractButtonAction() {
        new Thread(() -> {

            clearMessageBackground();

            setRunning(true);
            try {
                executor.run(Commandline.translateCommandline(argumentsField.getText()));
            } catch (Exception e) {
                Logger.INSTANCE.severe(e.getMessage());
                e.printStackTrace();
            }
            setRunning(false);
        }).start();
    }

    public void handleCheckButtonAction() {
        clearMessageBackground();

        URL url;

        try {
            url = new URL(svnUrl.getText());
        } catch (MalformedURLException e) {
            Logger.INSTANCE.severe("Invalid URL format.");
            setUrlType(CommandOptions.UrlType.INVALID);
            return;
        }

        CommandOptions options = new CommandOptions();
        options.setTargetUrl(url);
        RevisionCollector collector = options.generateRevisionCollector();

        setUrlType(options.getUrlType());

        if (urlType == CommandOptions.UrlType.GIT) {
            GitRevisionCollector revisionCollector = (GitRevisionCollector) collector;

            setGitLocal(revisionCollector.isLocalFile());
            setBranchList(Arrays.asList(revisionCollector.getBranchList()));
        }
    }

    private void setGitLocal(boolean isLocal) {
        this.gitLocal = isLocal;
        updateUi();
    }

    private void setRunning(boolean isRunning) {
        this.running = isRunning;
        updateUi();
    }

    private void setUrlType(CommandOptions.UrlType type) {
        this.urlType = type;
        updateUi();
    }

    private void setBranchList(List<String> branchList) {
        branchComboBox.setItems(new ObservableListWrapper<>(branchList));
    }

    private void updateUi() {
        extractButton.setDisable(running);
        gitSettings.setDisable(urlType != CommandOptions.UrlType.GIT);
        buildButton.setDisable(urlType == CommandOptions.UrlType.INVALID);

        gitTempDir.setDisable(gitLocal);
        preserveTempDir.setDisable(gitLocal);
    }

    private void clearMessageBackground() {
        // UI related background jobs should be done with runLater method.
        Platform.runLater(() -> outputList.getItems().clear());
    }

    private void addMessageBackground(String message) {
        Platform.runLater(() -> {
            outputList.getItems().add(message.trim());
            outputList.scrollTo(outputList.getItems().size());
        });
    }

    private void appendArgument(StringBuilder builder, String string) {
        if (builder.length() > 0) {
            builder.append(' ');
        }

        if (string.contains(" ")) {
            string = '"' + string + '"';
        }

        builder.append(string);
    }

}
