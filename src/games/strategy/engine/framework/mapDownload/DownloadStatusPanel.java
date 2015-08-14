package games.strategy.engine.framework.mapDownload;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;


public class DownloadStatusPanel extends JPanel {
  private final JProgressBar progressBar;

  public DownloadStatusPanel(final String site, final String fileName) {

    JLabel label = new JLabel("Map: " + fileName);
    add(label);

    progressBar = new JProgressBar(0, 100);
    progressBar.setSize(50, 50);
    progressBar.setValue(0);
    progressBar.setStringPainted(true);
    add(progressBar);
    DownloadStatusPanel downloadPanel = this;
    add(new ExecuteButton(downloadPanel, site, fileName));

  }

  public void updateProgress(int percentDone) {
    progressBar.setValue(percentDone);
  }

  private class ExecuteButton extends JButton {
    private static final long serialVersionUID = -3778493055140058432L;

    private ExecuteButton(final DownloadStatusPanel downloadPanel, final String site, final String fileName) {
      super("Download");
      super.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          // downloadPanel.executeDownload(downloadPanel, site, fileName);
        }
      });
    }
  }
}
