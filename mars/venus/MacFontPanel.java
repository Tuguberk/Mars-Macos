package mars.venus;

import java.awt.Font;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.swing.SwingUtilities;

/**
 * Shows the macOS Fonts panel. Java cannot open this panel, so a small script
 * opens it in its own process. The script prints one line for each change.
 */
final class MacFontPanel {
   interface Listener {
      void fontChosen(String family, int style, int size);
   }

   private static final String SCRIPT = String.join("\n",
         "ObjC.import('Cocoa');",
         "function run(argv) {",
         "  var app = $.NSApplication.sharedApplication;",
         "  app.setActivationPolicy($.NSApplicationActivationPolicyAccessory);",
         "  var manager = $.NSFontManager.sharedFontManager;",
         "  var current = manager.fontWithFamilyTraitsWeightSize($(argv[0]), Number(argv[1]), 5, Number(argv[2]));",
         "  if (current.isNil()) { current = $.NSFont.userFixedPitchFontOfSize(Number(argv[2])); }",
         "  function report() {",
         "    var line = current.familyName.js + '\\t' + manager.traitsOfFont(current) + '\\t' + Math.round(current.pointSize) + '\\n';",
         "    $.NSFileHandle.fileHandleWithStandardOutput.writeData($(line).dataUsingEncoding($.NSUTF8StringEncoding));",
         "  }",
         "  ObjC.registerSubclass({ name: 'MarsFontTarget', methods: {",
         "    'changeFont:': { types: ['void', ['id']], implementation: function (sender) { current = manager.convertFont(current); report(); } },",
         "    'panelClosed:': { types: ['void', ['id']], implementation: function (note) { app.terminate(null); } }",
         "  } });",
         "  var target = $.MarsFontTarget.alloc.init;",
         "  manager.setTarget(target);",
         "  manager.setAction('changeFont:');",
         "  manager.setSelectedFontIsMultiple(current, false);",
         "  var panel = $.NSFontPanel.sharedFontPanel;",
         "  panel.setHidesOnDeactivate(false);",
         "  panel.setFloatingPanel(true);",
         "  $.NSNotificationCenter.defaultCenter.addObserverSelectorNameObject(target, 'panelClosed:', $.NSWindowWillCloseNotification, panel);",
         "  app.activateIgnoringOtherApps(true);",
         "  panel.makeKeyAndOrderFront(null);",
         "  app.run;",
         "}");
   // Values of NSItalicFontMask and NSBoldFontMask.
   private static final int MAC_ITALIC = 1;
   private static final int MAC_BOLD = 2;

   private final Listener listener;
   private Process process;

   MacFontPanel(Listener listener) {
      this.listener = listener;
   }

   void show(Font font) {
      if (process != null && process.isAlive()) {
         return;
      }
      int traits = (font.isBold() ? MAC_BOLD : 0) | (font.isItalic() ? MAC_ITALIC : 0);
      try {
         process = new ProcessBuilder("/usr/bin/osascript", "-l", "JavaScript", "-e", SCRIPT,
               font.getFamily(), String.valueOf(traits), String.valueOf(font.getSize()))
               .redirectError(ProcessBuilder.Redirect.DISCARD).start();
      }
      catch (IOException e) {
         return;
      }
      Process started = process;
      Thread reader = new Thread(() -> readChanges(started), "Mac font panel");
      reader.setDaemon(true);
      reader.start();
   }

   void close() {
      if (process != null) {
         process.destroy();
         process = null;
      }
   }

   private void readChanges(Process started) {
      try (BufferedReader lines = new BufferedReader(
            new InputStreamReader(started.getInputStream(), StandardCharsets.UTF_8))) {
         String line;
         while ((line = lines.readLine()) != null) {
            String received = line;
            SwingUtilities.invokeLater(() -> applyLine(received));
         }
      }
      catch (IOException e) {
         // The panel closed.
      }
   }

   /** @param line family, Mac traits, and size, separated by tabs */
   void applyLine(String line) {
      String[] parts = line.split("\t");
      if (parts.length != 3) {
         return;
      }
      try {
         int traits = Integer.parseInt(parts[1]);
         int style = ((traits & MAC_BOLD) != 0 ? Font.BOLD : 0) | ((traits & MAC_ITALIC) != 0 ? Font.ITALIC : 0);
         listener.fontChosen(parts[0], style, Integer.parseInt(parts[2]));
      }
      catch (NumberFormatException e) {
         // Ignore a line we cannot read.
      }
   }
}
