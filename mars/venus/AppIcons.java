package mars.venus;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import mars.Globals;

/**
 * Loads icons. Most icons are SVG files, so they stay sharp on every screen
 * and change color with the theme.
 */
public final class AppIcons {
   private static final Pattern BITMAP_NAME = Pattern.compile("(.*?)(\\d+)\\.\\w+");

   private AppIcons() {
   }

   /** @param file the old bitmap name with its size, for example New22.png */
   public static Icon get(String file) {
      Matcher name = BITMAP_NAME.matcher(file);
      if (name.matches()) {
         String svg = "images/icons/" + name.group(1).toLowerCase() + ".svg";
         if (AppIcons.class.getClassLoader().getResource(svg) != null) {
            int size = Integer.parseInt(name.group(2));
            return new FlatSVGIcon(svg, size, size);
         }
      }
      URL bitmap = AppIcons.class.getResource(Globals.imagesPath + file);
      return new ImageIcon(bitmap);
   }
}
