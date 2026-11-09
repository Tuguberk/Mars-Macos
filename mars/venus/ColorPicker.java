package mars.venus;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.swing.JColorChooser;
import javax.swing.SwingUtilities;

/**
 * Asks the user for a color. A Mac shows its own color panel. Other systems
 * do not have one that Java can open, so they use the themed Swing window.
 */
public final class ColorPicker {
   private ColorPicker() {
   }

   /** Returns null if the user cancels. */
   public static Color choose(Component parent, String title, Color initial) {
      if (MacIntegration.isMac()) {
         try {
            return chooseOnMac(parent, initial == null ? Color.WHITE : initial);
         }
         catch (IOException e) {
            // osascript did not start. The Swing window still works.
         }
      }
      return JColorChooser.showDialog(parent, title, initial);
   }

   private static Color chooseOnMac(Component parent, Color initial) throws IOException {
      String script = "choose color default color {" + initial.getRed() * 257 + ", "
            + initial.getGreen() * 257 + ", " + initial.getBlue() * 257 + "}";
      Process process = new ProcessBuilder("/usr/bin/osascript", "-e", "activate", "-e", script)
            .redirectError(ProcessBuilder.Redirect.DISCARD).start();
      EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
      SecondaryLoop loop = queue.createSecondaryLoop();
      Color[] result = new Color[1];
      Thread reader = new Thread(() -> {
         try {
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.waitFor() == 0) {
               result[0] = parse(output);
            }
         }
         catch (IOException | InterruptedException e) {
            // Same result as Cancel.
         }
         finally {
            loop.exit();
         }
      }, "Mac color panel");
      reader.setDaemon(true);

      // The window stays painted while the panel is open, but it does not take clicks.
      Window window = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
      if (window != null) {
         window.setEnabled(false);
      }
      reader.start();
      loop.enter();
      if (window != null) {
         window.setEnabled(true);
         window.toFront();
      }
      if (Desktop.getDesktop().isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)) {
         Desktop.getDesktop().requestForeground(true);
      }
      return result[0];
   }

   /** @param output osascript output, for example "65535, 0, 32768" */
   static Color parse(String output) {
      String[] parts = output.trim().split("\\s*,\\s*");
      if (parts.length != 3) {
         return null;
      }
      try {
         return new Color(toEightBits(parts[0]), toEightBits(parts[1]), toEightBits(parts[2]));
      }
      catch (NumberFormatException e) {
         return null;
      }
   }

   private static int toEightBits(String value) {
      return Math.max(0, Math.min(255, Math.round(Integer.parseInt(value) / 257f)));
   }
}
