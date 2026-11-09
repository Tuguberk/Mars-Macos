package mars.venus;

import java.io.IOException;
import java.util.prefs.Preferences;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import mars.Globals;
import mars.Settings;

/** Chooses the light or dark theme on a Mac and follows changes in macOS. */
public final class MacTheme {
   private enum Choice {
      SYSTEM("Follow macOS"), LIGHT("Light"), DARK("Dark");

      final String label;

      Choice(String label) {
         this.label = label;
      }
   }

   private static final String PREFERENCE_KEY = "MacTheme";
   private static final long CHECK_EVERY_MILLIS = 2000;

   // Use these on the event thread after startup.
   private static Choice choice = Choice.SYSTEM;
   private static boolean systemDark;

   private MacTheme() {
   }

   /** Call this before the first Swing window. */
   static void prepareBeforeGui() {
      // The app does not use FlatLaf window features that need its native library.
      System.setProperty("flatlaf.useNativeLibrary", "false");
      choice = readChoice();
      systemDark = readSystemDark();
      Globals.getSettings().setBooleanSettingNonPersistent(Settings.DARK_MODE, isDark());
      // macOS reads this one time. "system" lets the title bar follow macOS.
      System.setProperty("apple.awt.application.appearance", titleBarAppearance());
   }

   /** Call this on the event thread before the main window is made. */
   public static void applyStartTheme() {
      DarkTheme.apply(isDark());
   }

   /** Starts a background check for theme changes in macOS. */
   static void startWatching() {
      Thread watcher = new Thread(() -> {
         boolean last = readSystemDark();
         while (true) {
            try {
               Thread.sleep(CHECK_EVERY_MILLIS);
            }
            catch (InterruptedException e) {
               return;
            }
            boolean now = readSystemDark();
            if (now != last) {
               last = now;
               final boolean dark = now;
               SwingUtilities.invokeLater(() -> systemThemeChanged(dark));
            }
         }
      }, "macOS theme watcher");
      watcher.setDaemon(true);
      watcher.start();
   }

   /** Makes the Theme submenu for the Settings menu. */
   public static JMenu createMenu() {
      JMenu menu = new JMenu("Theme");
      ButtonGroup group = new ButtonGroup();
      for (Choice item : Choice.values()) {
         JRadioButtonMenuItem button = new JRadioButtonMenuItem(item.label, item == choice);
         button.addActionListener(event -> choose(item));
         group.add(button);
         menu.add(button);
      }
      return menu;
   }

   private static void choose(Choice newChoice) {
      choice = newChoice;
      Preferences.userNodeForPackage(Settings.class).put(PREFERENCE_KEY, newChoice.name());
      applyCurrentTheme();
   }

   private static void systemThemeChanged(boolean dark) {
      systemDark = dark;
      if (choice == Choice.SYSTEM) {
         applyCurrentTheme();
      }
   }

   private static void applyCurrentTheme() {
      Globals.getSettings().setBooleanSettingNonPersistent(Settings.DARK_MODE, isDark());
      DarkTheme.apply(isDark());
   }

   private static boolean isDark() {
      return choice == Choice.DARK || (choice == Choice.SYSTEM && systemDark);
   }

   private static String titleBarAppearance() {
      switch (choice) {
         case DARK:
            return "NSAppearanceNameDarkAqua";
         case LIGHT:
            return "NSAppearanceNameAqua";
         default:
            return "system";
      }
   }

   private static Choice readChoice() {
      String saved = Preferences.userNodeForPackage(Settings.class).get(PREFERENCE_KEY, Choice.SYSTEM.name());
      try {
         return Choice.valueOf(saved);
      }
      catch (IllegalArgumentException e) {
         return Choice.SYSTEM;
      }
   }

   private static boolean readSystemDark() {
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
}
