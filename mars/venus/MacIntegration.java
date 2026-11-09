package mars.venus;

import java.awt.Desktop;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import mars.Globals;
import mars.Settings;

/**
 * Connects MARS with the macOS menu bar, Finder, and native file dialogs.
 * Every public method does nothing special on other systems.
 */
public final class MacIntegration {
   private static final boolean MAC =
         System.getProperty("os.name", "").toLowerCase().startsWith("mac");

   // Finder can send files before the main window exists. Use these on the event thread only.
   private static final List<File> pendingFiles = new ArrayList<File>();
   private static VenusUI mainUI;
   private static Action openAction;

   private MacIntegration() {
   }

   public static boolean isMac() {
      return MAC;
   }

   /** Call this before the first Swing window. */
   public static void prepareBeforeGui() {
      if (!MAC) {
         return;
      }
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      System.setProperty("apple.awt.application.name", "Mars-Macos");
      useSystemThemeUntilUserChooses();
      if (Globals.getSettings().getBooleanSetting(Settings.DARK_MODE)) {
         // macOS reads this one time, so the title bar changes at the next start.
         System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
      }
      Desktop desktop = Desktop.getDesktop();
      if (desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) {
         desktop.setOpenFileHandler(event -> {
            List<File> files = new ArrayList<File>(event.getFiles());
            SwingUtilities.invokeLater(() -> openFiles(files));
         });
      }
   }

   /** Call this on the event thread when the main window is ready. */
   public static void connect(VenusUI ui, Action open, Action about, Action settings, Action exit) {
      if (!MAC) {
         return;
      }
      mainUI = ui;
      openAction = open;
      Desktop desktop = Desktop.getDesktop();
      if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
         desktop.setAboutHandler(event -> SwingUtilities.invokeLater(() -> run(about)));
      }
      if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) {
         desktop.setPreferencesHandler(event -> SwingUtilities.invokeLater(() -> run(settings)));
      }
      if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
         desktop.setQuitHandler((event, response) -> SwingUtilities.invokeLater(() -> {
            // Exit is disabled while a program runs. Quit must follow the same rule.
            if (exit.isEnabled() && mainUI.getEditor().closeAll()) {
               response.performQuit();
            }
            else {
               response.cancelQuit();
            }
         }));
      }
      List<File> files = new ArrayList<File>(pendingFiles);
      pendingFiles.clear();
      openFiles(files);
   }

   /** Shows the native Open dialog. Returns null if the user cancels. */
   public static File chooseFileToOpen(Frame parent, String directory, File selected) {
      return choose(parent, "Open", FileDialog.LOAD, directory,
            selected == null ? null : selected.getName());
   }

   /** Shows the native Save dialog. It asks before it replaces a file. */
   public static File chooseFileToSave(Frame parent, String directory, String name) {
      return choose(parent, "Save As", FileDialog.SAVE, directory, name);
   }

   // The Dark mode menu item saves the choice. After that, the saved choice wins.
   private static void useSystemThemeUntilUserChooses() {
      if (Preferences.userNodeForPackage(Settings.class).get("DarkMode", null) == null) {
         Globals.getSettings().setBooleanSettingNonPersistent(Settings.DARK_MODE, isSystemDark());
      }
   }

   private static boolean isSystemDark() {
      try {
         Process process = new ProcessBuilder("/usr/bin/defaults", "read", "-g", "AppleInterfaceStyle")
               .redirectErrorStream(true).start();
         String output = new String(process.getInputStream().readAllBytes()).trim();
         return process.waitFor() == 0 && output.equalsIgnoreCase("Dark");
      }
      catch (IOException e) {
         return false;
      }
      catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         return false;
      }
   }

   private static File choose(Frame parent, String title, int mode, String directory, String name) {
      FileDialog dialog = new FileDialog(parent, title, mode);
      dialog.setDirectory(directory);
      dialog.setFile(name);
      dialog.setVisible(true);
      String chosen = dialog.getFile();
      return chosen == null ? null : new File(dialog.getDirectory(), chosen);
   }

   private static void openFiles(List<File> files) {
      if (files.isEmpty()) {
         return;
      }
      if (mainUI == null) {
         pendingFiles.addAll(files);
         return;
      }
      // File > Open is disabled while a program runs. Finder must follow the same rule.
      if (!openAction.isEnabled()) {
         Toolkit.getDefaultToolkit().beep();
         return;
      }
      mainUI.setState(Frame.NORMAL);
      mainUI.toFront();
      EditTabbedPane tabs = (EditTabbedPane) mainUI.getMainPane().getEditTabbedPane();
      File last = null;
      for (File file : files) {
         if (tabs.openFile(file)) {
            last = file;
         }
      }
      if (last != null && last.canRead()) {
         mainUI.getEditor().setCurrentOpenDirectory(last.getParent());
         if (Globals.getSettings().getAssembleOnOpenEnabled()) {
            mainUI.getRunAssembleAction().actionPerformed(null);
         }
      }
   }

   private static void run(Action action) {
      if (action.isEnabled()) {
         action.actionPerformed(new ActionEvent(mainUI, ActionEvent.ACTION_PERFORMED, "mac-menu"));
      }
   }
}
