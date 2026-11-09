package mars.venus;

import com.formdev.flatlaf.util.SystemFileChooser;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Shows the Open and Save windows of the operating system. Both methods
 * return null if the user cancels. The Save window asks before it replaces a file.
 */
public final class FilePicker {
   // The Open window can select only files with these extensions.
   private static final String[] TEXT_EXTENSIONS = {"asm", "s", "mips", "inc", "txt", "text"};
   private static final List<String> TEXT_EXTENSION_LIST = Arrays.asList(TEXT_EXTENSIONS);

   private FilePicker() {
   }

   public static File open(Frame parent, String directory, File selected) {
      if (MacIntegration.isMac()) {
         return showOnMac(parent, "Open", FileDialog.LOAD, directory,
               selected == null ? null : selected.getName());
      }
      SystemFileChooser chooser = newChooser("Open", directory);
      if (selected != null) {
         chooser.setSelectedFile(selected);
      }
      chooser.setAcceptAllFileFilterUsed(false);
      chooser.setFileFilter(new SystemFileChooser.FileNameExtensionFilter(
            "Assembly and text files", TEXT_EXTENSIONS));
      return chooser.showOpenDialog(parent) == SystemFileChooser.APPROVE_OPTION
            ? chooser.getSelectedFile() : null;
   }

   public static File save(Frame parent, String title, String directory, String name) {
      if (MacIntegration.isMac()) {
         return showOnMac(parent, title, FileDialog.SAVE, directory, name);
      }
      SystemFileChooser chooser = newChooser(title, directory);
      if (name != null) {
         chooser.setSelectedFile(new File(directory, name));
      }
      return chooser.showSaveDialog(parent) == SystemFileChooser.APPROVE_OPTION
            ? chooser.getSelectedFile() : null;
   }

   private static SystemFileChooser newChooser(String title, String directory) {
      SystemFileChooser chooser = new SystemFileChooser(directory);
      chooser.setDialogTitle(title);
      return chooser;
   }

   private static File showOnMac(Frame parent, String title, int mode, String directory, String name) {
      FileDialog dialog = new FileDialog(parent, title, mode);
      dialog.setDirectory(directory);
      dialog.setFile(name);
      if (mode == FileDialog.LOAD) {
         dialog.setFilenameFilter((folder, file) -> hasTextExtension(file));
      }
      dialog.setVisible(true);
      String chosen = dialog.getFile();
      return chosen == null ? null : new File(dialog.getDirectory(), chosen);
   }

   private static boolean hasTextExtension(String fileName) {
      int dot = fileName.lastIndexOf('.');
      return dot >= 0 && TEXT_EXTENSION_LIST.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
   }
}
