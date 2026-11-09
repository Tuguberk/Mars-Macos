package mars.venus;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import mars.Globals;
import mars.Settings;

/** Remembers open files, reloads files that change on disk, and installs the mars command. */
public final class Workspace {
   private static final String FILES_KEY = "MacSessionFiles";
   private static final String ACTIVE_KEY = "MacSessionActive";
   private static final int CHECK_EVERY_MILLIS = 1000;
   private static final String COMMAND_MARKER = "# Mars-Macos command";

   private static final class DiskState {
      long modified;
      String text;
   }

   private static final Map<EditPane, DiskState> diskStates = new WeakHashMap<EditPane, DiskState>();
   private static boolean asking;

   private Workspace() {
   }

   // ---- Session ----

   /** Remembers the open files. Call this before the tabs close. */
   static void saveSession(VenusUI ui) {
      EditTabbedPane tabs = tabs(ui);
      StringBuilder files = new StringBuilder();
      for (int i = 0; i < tabs.getTabCount(); i++) {
         EditPane pane = (EditPane) tabs.getComponentAt(i);
         String next = pane.getPathname() + "\n";
         if (!pane.isNew() && files.length() + next.length() <= Preferences.MAX_VALUE_LENGTH) {
            files.append(next);
         }
      }
      Preferences preferences = preferences();
      preferences.put(FILES_KEY, files.toString());
      EditPane active = tabs.getCurrentEditTab();
      if (active != null && !active.isNew() && active.getPathname().length() <= Preferences.MAX_VALUE_LENGTH) {
         preferences.put(ACTIVE_KEY, active.getPathname());
      }
      else {
         preferences.remove(ACTIVE_KEY);
      }
   }

   /** Opens the files from the last session. Files that do not exist now are skipped. */
   static void restoreSession(VenusUI ui) {
      EditTabbedPane tabs = tabs(ui);
      for (String path : preferences().get(FILES_KEY, "").split("\n")) {
         if (!path.isEmpty() && new File(path).isFile()) {
            tabs.openFile(new File(path));
         }
      }
      String active = preferences().get(ACTIVE_KEY, null);
      EditPane pane = active == null ? null : tabs.getEditPaneForFile(active);
      if (pane != null) {
         tabs.setCurrentEditTab(pane);
      }
   }

   // ---- Reload files that change on disk ----

   static void startWatchingFiles(VenusUI ui) {
      new Timer(CHECK_EVERY_MILLIS, event -> checkFiles(ui)).start();
   }

   private static void checkFiles(VenusUI ui) {
      if (asking || FileStatus.get() == FileStatus.RUNNING) {
         return;
      }
      EditTabbedPane tabs = tabs(ui);
      for (int i = 0; i < tabs.getTabCount(); i++) {
         checkFile(ui, tabs, (EditPane) tabs.getComponentAt(i));
      }
   }

   private static void checkFile(VenusUI ui, EditTabbedPane tabs, EditPane pane) {
      if (pane.isNew()) {
         return;
      }
      File file = new File(pane.getPathname());
      long modified = file.lastModified();
      DiskState state = diskStates.get(pane);
      if (modified == 0 || (state != null && state.modified == modified)) {
         return;
      }
      String text;
      try {
         text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).replace("\r\n", "\n");
      }
      catch (IOException e) {
         return;
      }
      if (state == null) {
         state = new DiskState();
         state.modified = modified;
         state.text = text;
         diskStates.put(pane, state);
         return;
      }
      boolean newContent = !sameText(text, state.text);
      state.modified = modified;
      state.text = text;
      if (newContent && !sameText(text, pane.getSource()) && userAcceptsReload(ui, pane)) {
         reload(ui, tabs, pane, text);
      }
   }

   private static boolean userAcceptsReload(VenusUI ui, EditPane pane) {
      if (!pane.hasUnsavedEdits()) {
         return true;
      }
      Object[] options = {"Reload", "Keep My Changes"};
      asking = true;
      try {
         return JOptionPane.showOptionDialog(ui,
               pane.getFilename() + " changed on disk, and you have unsaved changes in MARS.",
               "File Changed", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
               null, options, options[1]) == 0;
      }
      finally {
         asking = false;
      }
   }

   private static void reload(VenusUI ui, EditTabbedPane tabs, EditPane pane, String text) {
      pane.replaceSourceKeepingCaret(text.endsWith("\n") ? text : text + "\n");
      pane.setFileStatus(FileStatus.NOT_EDITED);
      if (pane == tabs.getCurrentEditTab()) {
         FileStatus.setEdited(false);
         FileStatus.set(FileStatus.NOT_EDITED);
         ui.getEditor().setTitle(pane.getPathname(), pane.getFilename(), FileStatus.NOT_EDITED);
         // The old assembled program does not match the new source.
         ui.getMainPane().getExecutePane().clearPane();
      }
      tabs.setTitleAt(tabs.indexOfComponent(pane), pane.getFilename());
   }

   private static boolean sameText(String first, String second) {
      return stripEnd(first).equals(stripEnd(second));
   }

   private static String stripEnd(String text) {
      return text.replaceAll("\\s+$", "");
   }

   // ---- Terminal command ----

   /** Adds the install item. The item needs the packaged app. */
   static void addMenuItems(JMenu menu) {
      String app = System.getProperty("jpackage.app-path");
      if (app == null) {
         return;
      }
      JMenuItem item = new JMenuItem("Install mars Terminal Command...");
      item.addActionListener(event -> installCommand(app));
      menu.addSeparator();
      menu.add(item);
   }

   private static void installCommand(String app) {
      File directory = commandDirectory();
      try {
         File command = writeCommand(directory, app);
         JOptionPane.showMessageDialog(Globals.getGui(), "Installed " + command.getPath()
               + "\n\nRun a program with:  mars file.asm"
               + (isOnPath(directory) ? "" : "\n\nAdd " + directory.getPath() + " to your PATH to use it."));
      }
      catch (IOException e) {
         JOptionPane.showMessageDialog(Globals.getGui(), e.getMessage(),
               "Could Not Install Command", JOptionPane.ERROR_MESSAGE);
      }
   }

   private static File commandDirectory() {
      List<File> candidates = new ArrayList<File>();
      candidates.add(new File("/opt/homebrew/bin"));
      candidates.add(new File("/usr/local/bin"));
      for (File candidate : candidates) {
         if (candidate.isDirectory() && candidate.canWrite()) {
            return candidate;
         }
      }
      return new File(System.getProperty("user.home"), ".local/bin");
   }

   private static boolean isOnPath(File directory) {
      String path = System.getenv("PATH");
      // Apps from Finder get a short PATH, so check the usual shell folder too.
      return (path != null && path.contains(directory.getPath()))
            || directory.getPath().equals("/opt/homebrew/bin") || directory.getPath().equals("/usr/local/bin");
   }

   private static File writeCommand(File directory, String app) throws IOException {
      Files.createDirectories(directory.toPath());
      File command = new File(directory, "mars");
      if (command.exists() && !new String(Files.readAllBytes(command.toPath()), StandardCharsets.UTF_8).contains(COMMAND_MARKER)) {
         throw new IOException(command.getPath() + " already exists and is not from Mars-Macos.");
      }
      String script = "#!/bin/sh\n" + COMMAND_MARKER + "\n"
            + "app='" + app.replace("'", "'\\''") + "'\n"
            + "if [ \"$#\" -eq 0 ]; then exec open \"${app%/Contents/MacOS/*}\"; fi\n"
            + "if [ \"$#\" -eq 1 ] && [ \"$1\" = h ]; then exec \"$app\" h; fi\n"
            + "exec \"$app\" nc \"$@\"\n";
      Files.write(command.toPath(), script.getBytes(StandardCharsets.UTF_8));
      if (!command.setExecutable(true, false)) {
         throw new IOException("Could not make " + command.getPath() + " executable.");
      }
      return command;
   }

   private static EditTabbedPane tabs(VenusUI ui) {
      return (EditTabbedPane) ui.getMainPane().getEditTabbedPane();
   }

   private static Preferences preferences() {
      return Preferences.userNodeForPackage(Settings.class);
   }
}
