package mars.venus;

import java.awt.Desktop;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import mars.Globals;

/**
 * Connects MARS with the macOS menu bar and Finder.
 * Every public method does nothing special on other systems.
 */
public final class MacIntegration {
   private static final boolean MAC =
         System.getProperty("os.name", "").toLowerCase().startsWith("mac");

   // Finder can send files before the main window exists. Use these on the event thread only.
   private static final List<File> pendingFiles = new ArrayList<File>();
   private static VenusUI mainUI;
   private static Action openAction;
   private static Action exitAction;

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
      MacTheme.prepareBeforeGui();
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
         Workspace.restoreSession(ui);
         Workspace.startWatchingFiles(ui);
         AgentSkill.installOnStart(ui);
         return;
      }
      mainUI = ui;
      openAction = open;
      exitAction = exit;
      ui.getRootPane().putClientProperty("apple.awt.transparentTitleBar", Boolean.TRUE);
      MacTheme.startWatching();
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
            if (exit.isEnabled() && saveSessionAndCloseAll()) {
               response.performQuit();
            }
            else {
               response.cancelQuit();
            }
         }));
      }
      Workspace.restoreSession(ui);
      List<File> files = new ArrayList<File>(pendingFiles);
      pendingFiles.clear();
      openFiles(files);
      Workspace.startWatchingFiles(ui);
      MacUpdater.checkOnStart(ui);
      AgentSkill.installOnStart(ui);
   }

   /** False if a program runs or the user keeps unsaved work open. */
   static boolean saveAndCloseForUpdate() {
      return exitAction.isEnabled() && saveSessionAndCloseAll();
   }

   private static boolean saveSessionAndCloseAll() {
      Workspace.saveSession(mainUI);
      return mainUI.getEditor().closeAll();
   }

   /** A native Mac menu looks best without small pictures next to the items. */
   static void removeMenuIcons(JMenuBar menuBar) {
      if (!MAC) {
         return;
      }
      for (int m = 0; m < menuBar.getMenuCount(); m++) {
         JMenu menu = menuBar.getMenu(m);
         for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item != null) {
               item.setIcon(null);
            }
         }
      }
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
