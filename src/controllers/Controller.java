package controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class Controller {
    @FXML
    private TextField keyField;
    @FXML
    private ToggleGroup modeGroup;
    @FXML
    private Label locationLabel;
    @FXML
    private Label outputLocationLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label timeElapsedLabel;
    @FXML
    private Label fileFolderDetailsLabel;
    @FXML
    private VBox fileFolderVBox;
    @FXML
    private Button beginBtn;
    @FXML
    private Button outputPathBtn;
    @FXML
    private CheckBox overwriteCheckBox;

    private RadioButton mode;
    private File chosenFileOrFolder = null;
    private File outputLocation = null;
    private List<File> files;
    private int fileCount = 0;
    private int folderCount = 0;
    private int completedCount = 0;

    private static boolean beginEncryptOrDecrypt(int cipherMode, String key, File inputFile,
            File outputFile) {
        try {
            byte[] decodedKey = key.getBytes(StandardCharsets.UTF_8);
            SecretKey secretKey = new SecretKeySpec(Arrays.copyOf(decodedKey, 16), "AES");

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(cipherMode, secretKey);

            FileInputStream inputStream = new FileInputStream(inputFile);
            byte[] inputBytes = new byte[(int) inputFile.length()];
            inputStream.read(inputBytes);

            byte[] outputBytes = cipher.doFinal(inputBytes);

            if (!outputFile.exists()) {
                new File(outputFile.getParent()).mkdirs();
                outputFile.createNewFile();
            }

            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(outputBytes);

            inputStream.close();
            outputStream.close();

            return true;
        } catch (NoSuchPaddingException | NoSuchAlgorithmException
                | InvalidKeyException | BadPaddingException
                | IllegalBlockSizeException | IOException ex) {
            ex.printStackTrace();
            System.out.println("Error encrypting/decrypting file");
            return false;
        }
    }

    @FXML
    public void initialize() {
        mode = (RadioButton) modeGroup.getSelectedToggle();
        modeGroup.selectedToggleProperty()
                .addListener((o, ov, nv) -> mode = (RadioButton) modeGroup.getSelectedToggle());

        fileFolderVBox.setManaged(false);
        fileFolderVBox.setVisible(false);

        beginBtn.setDisable(true);
        outputPathBtn.setDisable(true);
        overwriteCheckBox.setSelected(true);

        outputLocationLabel.setManaged(false);
        outputLocationLabel.setVisible(false);

        statusLabel.setManaged(false);
        statusLabel.setVisible(false);

        timeElapsedLabel.setManaged(false);
        timeElapsedLabel.setVisible(false);

        overwriteCheckBox.selectedProperty().addListener((o, ov, nv) -> {
            if (nv) {
                outputLocation = null;
                outputLocationLabel.setText("");
                outputLocationLabel.setManaged(false);
                outputLocationLabel.setVisible(false);
                beginBtn.setDisable(keyField.getText().isEmpty());
            } else {
                beginBtn.setDisable(outputLocation == null);
            }
            outputPathBtn.setDisable(nv);
        });

        keyField.textProperty().addListener((o, ov, nv) -> beginBtn.setDisable(
                nv.isEmpty() || files.isEmpty() || (!overwriteCheckBox.isSelected() && outputLocation == null)));

        files = new ArrayList<>();
    }

    @FXML
    private void handleSelectFile() {
        FileChooser chooser = new FileChooser();
        chosenFileOrFolder = chooser.showOpenDialog(fileFolderVBox.getScene().getWindow());

        if (chosenFileOrFolder != null) {
            fileFolderVBox.setManaged(true);
            fileFolderVBox.setVisible(true);
            locationLabel.setText("File Chosen: ");
            fileFolderDetailsLabel.setText(chosenFileOrFolder.getAbsolutePath());
            files.add(chosenFileOrFolder);
        }
    }

    @FXML
    private void handleSelectFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chosenFileOrFolder = chooser.showDialog(fileFolderVBox.getScene().getWindow());

        if (chosenFileOrFolder != null) {
            fileFolderVBox.setManaged(true);
            fileFolderVBox.setVisible(true);
            listAllFiles(chosenFileOrFolder.getPath());
            locationLabel.setText("Folder chosen: ");
            fileFolderDetailsLabel.setText(chosenFileOrFolder.getAbsolutePath() + "\n" + fileCount + " File(s) and "
                    + folderCount + " Folder(s)");
        }
    }

    @FXML
    private void handleSpecifyOutputPath() {
        DirectoryChooser chooser = new DirectoryChooser();
        outputLocation = chooser.showDialog(outputPathBtn.getScene().getWindow());

        if (outputLocation != null) {
            outputLocationLabel.setManaged(true);
            outputLocationLabel.setVisible(true);
            outputLocationLabel.setText("Saved at: " + outputLocation.getAbsolutePath());
            beginBtn.setDisable(keyField.getText().isEmpty());
        }
    }

    public void listAllFiles(String directoryName) {
        File directory = new File(directoryName);

        File[] fList = directory.listFiles();
        if (fList != null)
            for (File file : fList) {
                if (file.isFile()) {
                    fileCount++;
                    files.add(file);
                } else if (file.isDirectory()) {
                    folderCount++;
                    listAllFiles(file.getAbsolutePath());
                }
            }
    }

    @FXML
    private void handleReset() {
        chosenFileOrFolder = null;
        files.clear();
        fileCount = 0;
        folderCount = 0;
        completedCount = 0;
        keyField.setText("");
        fileFolderVBox.setManaged(false);
        fileFolderVBox.setVisible(false);
        outputPathBtn.setDisable(true);
        beginBtn.setDisable(true);
        overwriteCheckBox.setSelected(true);
    }

    @FXML
    private void handleEncryptOrDecrypt() {

        String key = keyField.getText();

        statusLabel.setManaged(true);
        statusLabel.setVisible(true);

        Task<Void> task = new Task<>() {
            @Override
            public Void call() {
                int temp = fileCount == 0 ? 1 : fileCount;
                long startTime = System.nanoTime();

                for (File f : files) {

                    File o = f;
                    if (outputLocation != null && files.size() > 1) {
                        o = new File(f.getPath().replace(chosenFileOrFolder.getAbsolutePath(),
                                outputLocation.getAbsolutePath()));
                    }
                    if (beginEncryptOrDecrypt(mode.getText().equals("Encrypt") ? 1 : 2, key, f, o)) {
                        completedCount++;
                        Platform.runLater(() -> statusLabel.setText(completedCount + " of " + temp + " ("
                                + (completedCount * 100 / temp) + "%)" + " File(s) "
                                + (mode.getText().equals("Encrypt") ? "Encrypted" : "Decrypted")));
                    }
                }

                long endTime = System.nanoTime();
                long duration = (endTime - startTime) / 1000000;

                Platform.runLater(() -> {
                    handleReset();
                    timeElapsedLabel.setManaged(true);
                    timeElapsedLabel.setVisible(true);
                    timeElapsedLabel.setText("Time elapsed: " + getTimeString(duration));
                });

                return null;
            }
        };
        new Thread(task).start();
    }

    private String getTimeString(long millis) {
        int minutes = (int) (millis / (1000 * 60));
        int seconds = (int) ((millis / 1000) % 60);
        int milliseconds = (int) (millis % 1000);
        return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds);
    }
}
