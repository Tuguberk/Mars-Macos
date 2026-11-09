package mars.venus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;

/**
 * Keeps the mars-mips skill in the folders that AI coding agents read, so
 * they know how to run MIPS programs with this copy of MARS. It only writes
 * for agents that are already on this computer.
 */
final class AgentSkill {
   private static final String RESOURCE = "skills/mars-mips/SKILL.md";
   private static final String COMMAND_IN_FILE = "/Applications/Mars-Macos.app/Contents/MacOS/Mars-Macos";
   private static final String OUR_FILE_MARK = "<!-- Installed by Mars-Macos -->";
   private static final String AUTO_KEY = "AgentSkillAuto";
   // Codex, Cursor, Copilot, and OpenCode read the shared ~/.agents folder.
   private static final String[] SHARED_FOLDER_USERS = {".agents", ".codex", ".cursor", ".copilot", ".config/opencode"};

   private AgentSkill() {
   }

   private static Preferences preferences() {
      return Preferences.userRoot().node("/mars");
   }

   static void addSettingsItem(JMenu settings) {
      if (runCommand() == null) {
         return;
      }
      JCheckBoxMenuItem auto = new JCheckBoxMenuItem("Install Skill for AI Agents",
            preferences().getBoolean(AUTO_KEY, true));
      auto.addActionListener(event -> {
         preferences().putBoolean(AUTO_KEY, auto.isSelected());
         if (auto.isSelected()) {
            installInBackground(mars.Globals.getGui());
         }
         else {
            remove();
         }
      });
      settings.add(auto);
   }

   static void installOnStart(VenusUI ui) {
      if (preferences().getBoolean(AUTO_KEY, true)) {
         installInBackground(ui);
      }
   }

   private static void installInBackground(VenusUI ui) {
      Thread thread = new Thread(() -> {
         List<File> added = install();
         if (!added.isEmpty()) {
            SwingUtilities.invokeLater(() -> ui.getMessagesPane().postMarsMessage(
                  "Added the mars-mips skill for AI agents in " + added
                  + ". You can turn this off in the Settings menu.\n"));
         }
      }, "Agent skill");
      thread.setDaemon(true);
      thread.start();
   }

   /** Returns the folders that did not have the skill before. */
   static List<File> install() {
      List<File> added = new ArrayList<File>();
      String command = runCommand();
      if (command == null) {
         return added;
      }
      try (InputStream in = AgentSkill.class.getClassLoader().getResourceAsStream(RESOURCE)) {
         if (in == null) {
            return added;
         }
         String text = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace(COMMAND_IN_FILE, command);
         for (File folder : skillFolders()) {
            File file = new File(folder, "SKILL.md");
            String old = file.isFile() ? new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8) : null;
            if (old != null && (old.equals(text) || !old.contains(OUR_FILE_MARK))) {
               continue; // It is current, or it is not ours.
            }
            folder.mkdirs();
            Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
            if (old == null) {
               added.add(folder);
            }
         }
      }
      catch (IOException e) {
         // The skill is extra. MARS works without it.
      }
      return added;
   }

   static void remove() {
      for (File folder : skillFolders()) {
         File file = new File(folder, "SKILL.md");
         try {
            if (file.isFile() && new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).contains(OUR_FILE_MARK)) {
               file.delete();
               folder.delete();
            }
         }
         catch (IOException e) {
            // Nothing else to try.
         }
      }
   }

   private static List<File> skillFolders() {
      File home = new File(System.getProperty("user.home"));
      List<File> folders = new ArrayList<File>();
      if (new File(home, ".claude").isDirectory()) {
         folders.add(new File(home, ".claude/skills/mars-mips"));
      }
      for (String user : SHARED_FOLDER_USERS) {
         if (new File(home, user).isDirectory()) {
            folders.add(new File(home, ".agents/skills/mars-mips"));
            break;
         }
      }
      return folders;
   }

   /** Returns null if this copy of MARS will not stay at this path. */
   private static String runCommand() {
      String app = System.getProperty("jpackage.app-path");
      if (app != null) {
         return app.startsWith("/Volumes/") || app.contains("/AppTranslocation/") ? null : app;
      }
      try {
         File location = new File(AgentSkill.class.getProtectionDomain().getCodeSource().getLocation().toURI());
         return location.getName().endsWith(".jar") ? "java -jar \"" + location.getPath() + "\"" : null;
      }
      catch (URISyntaxException | IllegalArgumentException e) {
         return null;
      }
   }
}
