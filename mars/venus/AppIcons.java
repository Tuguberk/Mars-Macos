package mars.venus;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import mars.Globals;

/**
 * Loads toolbar icons. On a Mac the icons are SVG files, so they stay sharp on
 * every screen and change color with the theme.
 */
public final class AppIcons {
   private static final int TOOLBAR_SIZE = 22;

   private AppIcons() {
   }

   /** @param file the old bitmap name, for example New22.png */
   public static Icon get(String file) {
      if (MacIntegration.isMac()) {
         String name = file.replaceAll("\\d+\\.\\w+$", "").toLowerCase();
         String svg = "images/icons/" + name + ".svg";
         if (AppIcons.class.getClassLoader().getResource(svg) != null) {
            return new FlatSVGIcon(svg, TOOLBAR_SIZE, TOOLBAR_SIZE);
         }
      }
      URL bitmap = AppIcons.class.getResource(Globals.imagesPath + file);
      return new ImageIcon(bitmap);
   }
}
