   package mars.venus;

   import com.formdev.flatlaf.FlatDarkLaf;
   import com.formdev.flatlaf.FlatLightLaf;
   import com.formdev.flatlaf.themes.FlatMacDarkLaf;
   import com.formdev.flatlaf.themes.FlatMacLightLaf;
   import java.awt.Window;
   import javax.swing.JCheckBox;
   import javax.swing.JRadioButton;
   import javax.swing.JTabbedPane;
   import javax.swing.JToggleButton;
   import javax.swing.LookAndFeel;
   import javax.swing.SwingUtilities;
   import javax.swing.UIManager;
   import mars.Globals;

    public final class DarkTheme {

       private DarkTheme() {
      }

       public static void apply(boolean darkMode) {
         try {
            UIManager.setLookAndFeel(lookAndFeel(darkMode));
         }
             catch (Exception e) {
               return;
            }
         resetSharedButtonUIs();
         Window[] windows = Window.getWindows();
         for (int i = 0; i < windows.length; i++) {
            SwingUtilities.updateComponentTreeUI(windows[i]);
         }
         updateOpenEditors();
      }

       private static LookAndFeel lookAndFeel(boolean darkMode) {
         if (MacIntegration.isMac()) {
            return darkMode ? new FlatMacDarkLaf() : new FlatMacLightLaf();
         }
         return darkMode ? new FlatDarkLaf() : new FlatLightLaf();
      }

       private static void updateOpenEditors() {
         VenusUI ui = Globals.getGui();
         if (ui == null || ui.getMainPane() == null) {
            return;
         }
         JTabbedPane tabs = (JTabbedPane) ui.getMainPane().getEditTabbedPane();
         for (int i = 0; i < tabs.getTabCount(); i++) {
            if (tabs.getComponentAt(i) instanceof EditPane) {
               ((EditPane) tabs.getComponentAt(i)).updateThemeColors();
            }
         }
      }

       private static void resetSharedButtonUIs() {
         new JToggleButton().updateUI();
         new JRadioButton().updateUI();
         new JCheckBox().updateUI();
      }
   }
