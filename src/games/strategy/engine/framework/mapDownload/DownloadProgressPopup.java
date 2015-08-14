package games.strategy.engine.framework.mapDownload;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import games.strategy.engine.framework.GameRunner2;

/**
 * Intent is to have a model JFRame that shows the progress of (map) downloads
 * This component based upon the code example found at: http://mrbool.com/file-downloader-in-java/24291
 */
public class DownloadProgressPopup extends JFrame {

  public static void main(String[] args) {
    String site = "http://downloads.sourceforge.net/project/tripleamaps/maps/World_At_War.zip";
    String fileName = "World_At_War.zip";



    new DownloadProgressPopup(site, fileName);
  }

  private static class FileDownloadMeta {
    // public final String url;
    // public final String mapName;
  }


  // public static interface Download
  // public DownloadProgressPopup(List<DownloadFileDescription> downloadFiles) {

  public DownloadProgressPopup(String site, String fileName) {
    super.setTitle("Downloading Maps");

    JFrame frame = new JFrame();

    JPanel panel = new DownloadStatusPanel(site, fileName);



    frame.add(panel);
    frame.setVisible(true);
    frame.setLayout(new FlowLayout());
    frame.setSize(400, 200);

  }



  private void executeDownload(final DownloadStatusPanel trackingPanel, final String site, final String fileName) {
    Runnable r = new Runnable() {
      @Override
      public void run() {
        executeDownloadNonThreaded(trackingPanel, site, fileName);
      }
    };
    Thread t = new Thread(r);
    t.start();

  }


  private void executeDownloadNonThreaded(final DownloadStatusPanel trackingPanel, final String site,
      final String fileName) {
    try {
      URL url = new URL(site);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      int filesize = connection.getContentLength();
      java.io.BufferedInputStream in = new java.io.BufferedInputStream(connection.getInputStream());

      File destination = new File(GameRunner2.getUserMapsFolder(), fileName);
      java.io.FileOutputStream fileOutput = new java.io.FileOutputStream(destination);
      java.io.BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput, 1024);

      final int bufferSize = 1024;
      byte[] data = new byte[bufferSize];
      float totalDataRead = 0;
      int i = 0;
      while ((i = in.read(data, 0, bufferSize)) >= 0) {
        totalDataRead = totalDataRead + i;
        bufferedOutput.write(data, 0, i);
        final float Percent = (totalDataRead * 100) / filesize;
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            trackingPanel.updateProgress((int) Percent);
          }
        });
      }
      bufferedOutput.close();
      in.close();
    } catch (Exception e) {
      javax.swing.JOptionPane.showConfirmDialog((java.awt.Component) null, e.getMessage(), "Error",
          javax.swing.JOptionPane.DEFAULT_OPTION);
    }

  }
}

