package mars.venus;

import java.awt.BorderLayout;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import mars.Globals;

/**
 * Updates the Mac app from the newest GitHub release. It works only in the
 * packaged app, because a plain jar has no app to replace.
 */
final class MacUpdater {
   private static final String REPOSITORY = "Tuguberk/Mars-Macos";
   // A file in the newest release. The GitHub API limits requests from one network, this file does not.
   private static final String LATEST_URL = "https://github.com/" + REPOSITORY + "/releases/latest/download/latest.json";
   private static final String DOWNLOAD_PREFIX = "https://github.com/" + REPOSITORY + "/releases/download/";
   private static final String AUTO_KEY = "MacUpdateAuto";
   private static final String LAST_CHECK_KEY = "MacUpdateLastCheck";
   private static final String SKIP_KEY = "MacUpdateSkip";
   private static final long ONE_DAY_MILLIS = 24L * 60 * 60 * 1000;
   private static final String HELPER = String.join("\n",
         "#!/bin/sh",
         "# Arguments: process id, app, new app, work folder.",
         "while kill -0 \"$1\" 2>/dev/null; do sleep 0.2; done",
         "backup=\"$2.old.$$\"",
         "if /bin/mv \"$2\" \"$backup\"; then",
         "  if /bin/mv \"$3\" \"$2\"; then /bin/rm -rf \"$backup\"; else /bin/mv \"$backup\" \"$2\"; fi",
         "fi",
         "/usr/bin/open \"$2\"",
         "/bin/rm -rf \"$4\"",
         "");

   static final class Release {
      final String version;
      final String url;
      final String sha256;

      Release(String version, String url, String sha256) {
         this.version = version;
         this.url = url;
         this.sha256 = sha256;
      }
   }

   private MacUpdater() {
   }

   private static String appPath() {
      return System.getProperty("jpackage.app-path");
   }

   private static String currentVersion() {
      return System.getProperty("jpackage.app-version");
   }

   private static boolean isAvailable() {
      return MacIntegration.isMac() && appPath() != null && currentVersion() != null;
   }

   private static Preferences preferences() {
      return Preferences.userRoot().node("/mars");
   }

   // ---- Menu ----

   static void addSettingsItem(JMenu settings) {
      if (!isAvailable()) {
         return;
      }
      JCheckBoxMenuItem auto = new JCheckBoxMenuItem("Check for Updates Automatically",
            preferences().getBoolean(AUTO_KEY, true));
      auto.addActionListener(event -> preferences().putBoolean(AUTO_KEY, auto.isSelected()));
      settings.add(auto);
   }

   static void addHelpItem(JMenu help, VenusUI ui) {
      if (!isAvailable()) {
         return;
      }
      JMenuItem check = new JMenuItem("Check for Updates...");
      check.addActionListener(event -> check(ui, true));
      help.add(check);
   }

   /** Checks at most once a day, and only if the user did not turn this off. */
   static void checkOnStart(VenusUI ui) {
      if (!isAvailable() || !preferences().getBoolean(AUTO_KEY, true)) {
         return;
      }
      if (System.currentTimeMillis() - preferences().getLong(LAST_CHECK_KEY, 0) >= ONE_DAY_MILLIS) {
         check(ui, false);
      }
   }

   // ---- Check ----

   private static void check(VenusUI ui, boolean userAsked) {
      Thread thread = new Thread(() -> {
         try {
            Release release = fetchLatest();
            SwingUtilities.invokeLater(() -> showResult(ui, release, userAsked));
         }
         catch (IOException e) {
            if (userAsked) {
               SwingUtilities.invokeLater(() -> showError(ui, "Could not check for updates.", e));
            }
         }
      }, "Mac update check");
      thread.setDaemon(true);
      thread.start();
   }

   private static Release fetchLatest() throws IOException {
      try (InputStream in = open(LATEST_URL).getInputStream()) {
         ByteArrayOutputStream bytes = new ByteArrayOutputStream();
         in.transferTo(bytes);
         return parse(bytes.toString(StandardCharsets.UTF_8.name()), macArchitecture());
      }
   }

   private static HttpURLConnection open(String url) throws IOException {
      HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(30000);
      connection.setRequestProperty("User-Agent", "Mars-Macos/" + currentVersion());
      return connection;
   }

   private static String macArchitecture() {
      return "aarch64".equals(System.getProperty("os.arch")) ? "arm64" : "x86_64";
   }

   /** Returns null if the release has no complete file for this Mac. */
   static Release parse(String json, String architecture) {
      String version = field(json, "version");
      String url = field(json, architecture + "_url");
      String sha256 = field(json, architecture + "_sha256");
      if (version == null || url == null || sha256 == null || !version.matches("\\d+(\\.\\d+){0,2}")
            || !url.startsWith(DOWNLOAD_PREFIX) || !sha256.matches("[0-9a-f]{64}")) {
         return null;
      }
      return new Release(version, url, sha256);
   }

   private static String field(String json, String name) {
      Matcher value = Pattern.compile("\"" + name + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
      return value.find() ? value.group(1) : null;
   }

   static boolean isNewer(String candidate, String current) {
      String[] first = candidate.split("\\.");
      String[] second = current.split("\\.");
      for (int i = 0; i < Math.max(first.length, second.length); i++) {
         int a = i < first.length ? Integer.parseInt(first[i]) : 0;
         int b = i < second.length ? Integer.parseInt(second[i]) : 0;
         if (a != b) {
            return a > b;
         }
      }
      return false;
   }

   private static void showResult(VenusUI ui, Release release, boolean userAsked) {
      boolean newer = release != null && isNewer(release.version, currentVersion());
      if (!userAsked) {
         if (FileStatus.get() == FileStatus.RUNNING) {
            return; // Do not stop a running program with a question. Ask at the next start.
         }
         preferences().putLong(LAST_CHECK_KEY, System.currentTimeMillis());
         if (!newer || release.version.equals(preferences().get(SKIP_KEY, null))) {
            return;
         }
      }
      else if (!newer) {
         JOptionPane.showMessageDialog(ui, "You have the newest version of Mars-Macos (" + currentVersion() + ").",
               "Check for Updates", JOptionPane.INFORMATION_MESSAGE);
         return;
      }
      Object[] options = {"Update Now", "Later", "Skip This Version"};
      int answer = JOptionPane.showOptionDialog(ui,
            "Mars-Macos " + release.version + " is available. You have " + currentVersion() + ".",
            "Update Available", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
      if (answer == 0) {
         new Installer(ui, release).start();
      }
      else if (answer == 2) {
         preferences().put(SKIP_KEY, release.version);
      }
   }

   private static void showError(VenusUI ui, String message, Exception e) {
      JOptionPane.showMessageDialog(ui, message + "\n" + e.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
   }

   // ---- Install ----

   private static final class Installer extends SwingWorker<Void, Integer> {
      private final VenusUI ui;
      private final Release release;
      private final File app = new File(appPath()).getParentFile().getParentFile().getParentFile();
      private final File staged = new File(app.getParentFile(), ".Mars-Macos-update.app");
      private final JDialog dialog;
      private final JProgressBar bar = new JProgressBar(0, 100);
      private File work;
      private File dmg;
      private boolean replaceable;

      Installer(VenusUI ui, Release release) {
         this.ui = ui;
         this.release = release;
         dialog = new JDialog(ui, "Updating Mars-Macos", true);
         JButton cancel = new JButton("Cancel");
         cancel.addActionListener(event -> cancel(true));
         JPanel panel = new JPanel(new BorderLayout(8, 8));
         panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
         panel.add(new JLabel("Downloading Mars-Macos " + release.version + "..."), BorderLayout.NORTH);
         panel.add(bar, BorderLayout.CENTER);
         panel.add(cancel, BorderLayout.SOUTH);
         dialog.setContentPane(panel);
         dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
         dialog.setSize(360, 130);
         dialog.setLocationRelativeTo(ui);
      }

      void start() {
         execute();
         dialog.setVisible(true); // Modal. done() closes it.
      }

      @Override
      protected Void doInBackground() throws Exception {
         work = Files.createTempDirectory("mars-update").toFile();
         dmg = new File(work, "update.dmg");
         download();
         // An app that runs from a disk image or a protected folder cannot replace itself.
         replaceable = !app.getPath().contains("/AppTranslocation/") && app.getParentFile().canWrite() && app.canWrite();
         if (replaceable) {
            copyNewApp();
         }
         return null;
      }

      private void download() throws IOException, NoSuchAlgorithmException {
         MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
         HttpURLConnection connection = open(release.url);
         long total = connection.getContentLengthLong();
         try (InputStream in = new DigestInputStream(connection.getInputStream(), sha256);
              OutputStream out = Files.newOutputStream(dmg.toPath())) {
            byte[] buffer = new byte[65536];
            long received = 0;
            int count;
            while ((count = in.read(buffer)) != -1) {
               if (isCancelled()) {
                  throw new CancellationException();
               }
               out.write(buffer, 0, count);
               received += count;
               if (total > 0) {
                  publish((int) (received * 100 / total));
               }
            }
         }
         StringBuilder actual = new StringBuilder();
         for (byte b : sha256.digest()) {
            actual.append(String.format("%02x", b));
         }
         if (!actual.toString().equals(release.sha256)) {
            throw new IOException("The downloaded file is not correct.");
         }
      }

      private void copyNewApp() throws IOException, InterruptedException {
         File mount = new File(work, "mount");
         mount.mkdir();
         run("/usr/bin/hdiutil", "attach", "-quiet", "-nobrowse", "-readonly", "-mountpoint", mount.getPath(), dmg.getPath());
         try {
            run("/bin/rm", "-rf", staged.getPath());
            run("/usr/bin/ditto", new File(mount, "Mars-Macos.app").getPath(), staged.getPath());
         }
         finally {
            run("/usr/bin/hdiutil", "detach", "-quiet", "-force", mount.getPath());
         }
         if (!new File(staged, "Contents/MacOS/Mars-Macos").canExecute()) {
            throw new IOException("The new app is not complete.");
         }
      }

      @Override
      protected void process(List<Integer> percents) {
         bar.setValue(percents.get(percents.size() - 1));
      }

      @Override
      protected void done() {
         dialog.dispose();
         try {
            get();
         }
         catch (CancellationException | InterruptedException e) {
            cleanUp();
            return;
         }
         catch (ExecutionException e) {
            Exception reason = e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
            cleanUp();
            showError(ui, "Could not update Mars-Macos.", reason);
            return;
         }
         if (replaceable) {
            quitAndReplace();
         }
         else {
            openDiskImage();
         }
      }

      private void quitAndReplace() {
         if (!MacIntegration.saveAndCloseForUpdate()) {
            cleanUp();
            JOptionPane.showMessageDialog(ui, "The update was stopped. Close your work or stop the running program, "
                  + "then choose Help > Check for Updates.", "Check for Updates", JOptionPane.INFORMATION_MESSAGE);
            return;
         }
         try {
            File helper = new File(work, "install.sh");
            Files.write(helper.toPath(), HELPER.getBytes(StandardCharsets.UTF_8));
            new ProcessBuilder("/bin/sh", helper.getPath(), String.valueOf(ProcessHandle.current().pid()),
                  app.getPath(), staged.getPath(), work.getPath())
                  .redirectInput(new File("/dev/null"))
                  .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                  .redirectError(ProcessBuilder.Redirect.DISCARD)
                  .start();
            System.exit(0);
         }
         catch (IOException e) {
            cleanUp();
            showError(ui, "Could not update Mars-Macos.", e);
         }
      }

      private void openDiskImage() {
         JOptionPane.showMessageDialog(ui, "Mars-Macos cannot replace itself in this folder. "
               + "The new disk image will open now. Drag the app to Applications.",
               "Update Downloaded", JOptionPane.INFORMATION_MESSAGE);
         try {
            run("/usr/bin/open", dmg.getPath());
         }
         catch (IOException | InterruptedException e) {
            showError(ui, "Could not open the disk image.", e);
         }
      }

      private void cleanUp() {
         try {
            run("/bin/rm", "-rf", staged.getPath());
            if (work != null) {
               run("/bin/rm", "-rf", work.getPath());
            }
         }
         catch (IOException | InterruptedException e) {
            // Only temporary files stay.
         }
      }

      private static void run(String... command) throws IOException, InterruptedException {
         Process process = new ProcessBuilder(command)
               .redirectOutput(ProcessBuilder.Redirect.DISCARD)
               .redirectError(ProcessBuilder.Redirect.DISCARD)
               .start();
         if (process.waitFor() != 0) {
            throw new IOException(new File(command[0]).getName() + " failed.");
         }
      }
   }
}
