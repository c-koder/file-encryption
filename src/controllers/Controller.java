package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
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
    private int fileCount = 0, folderCount = 0;

    @FXML
    public void initialize() {
        mode = (RadioButton) modeGroup.getSelectedToggle();
        modeGroup.selectedToggleProperty().addListener((o, ov, nv) -> mode = (RadioButton) modeGroup.getSelectedToggle());

        fileFolderVBox.setManaged(false);
        fileFolderVBox.setVisible(false);

        beginBtn.setDisable(true);
        outputPathBtn.setDisable(true);
        overwriteCheckBox.setSelected(true);

        outputLocationLabel.setManaged(false);
        outputLocationLabel.setVisible(false);

        statusLabel.setManaged(false);
        statusLabel.setVisible(false);

        overwriteCheckBox.selectedProperty().addListener((o, ov, nv) -> {
            if (nv) {
                outputLocation = null;
                outputLocationLabel.setText("");
                outputLocationLabel.setManaged(false);
                outputLocationLabel.setVisible(false);
            }
            outputPathBtn.setDisable(nv);
        });

        keyField.textProperty().addListener((o, ov, nv) -> beginBtn.setDisable(nv.isEmpty()));

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
            fileFolderDetailsLabel.setText(chosenFileOrFolder.getAbsolutePath() + "\n" + fileCount + " File(s) and " + folderCount + " Folder(s)");
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
        keyField.setText("");
        fileFolderVBox.setManaged(false);
        fileFolderVBox.setVisible(false);
        outputPathBtn.setDisable(true);
        beginBtn.setDisable(true);
        overwriteCheckBox.setSelected(true);
    }

    @FXML
    private void handleSpecifyOutputPath() {
        DirectoryChooser chooser = new DirectoryChooser();
        outputLocation = chooser.showDialog(outputPathBtn.getScene().getWindow());

        if (outputLocation != null) {
            outputLocationLabel.setManaged(true);
            outputLocationLabel.setVisible(true);
            outputLocationLabel.setText("Saved at: " + outputLocation.getAbsolutePath());
        }
    }

    private void encryptOrDecrypt(SecretKey key, String mode, File file) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(mode.equals("E") ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key);
        CipherInputStream cipherInputStream = new CipherInputStream(
                new FileInputStream(file), cipher);
        handleWriteToFile(file, cipherInputStream);
    }

    @FXML
    private void handleEncryptOrDecrypt() {
        try {
            byte[] key = keyField.getText().getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");


            for (File f : files) {
                encryptOrDecrypt(secretKeySpec, mode.getText().equals("Encrypt") ? "E" : "D", f);
            }

//            if (cipt != null) {
//                keyField.setDisable(true);
//                beginBtn.setDisable(true);
//                statusLabel.setText("Image encrypted successfully,\nplease proceed to download the file.");
//            } else {
//                keyField.setText("");
//                keyField.setDisable(false);
//                beginBtn.setDisable(false);
//                statusLabel.setText("Encryption failed, please try again.");
//            }
        } catch (Exception e) {
            if (e.getMessage().contains("BadPaddingException"))
                e.printStackTrace();

            keyField.setText("");
            keyField.setDisable(false);
            beginBtn.setDisable(false);
            statusLabel.setText("Encryption failed, please try again.");
            e.printStackTrace();
        }
    }

    private void handleWriteToFile(File file, CipherInputStream cipt) {
        if (file != null) {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(
                        file.toPath().toString());
                int i;
                while ((i = cipt.read()) != -1) {
                    fileOutputStream.write(i);
                }
                fileOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
