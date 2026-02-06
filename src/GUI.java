import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class GUI extends JFrame {

    public GUI() {
        this.setTitle("Logan");
        int width = 400, height = 400;
        this.setSize(width, height);
        this.setDefaultCloseOperation(this.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());

        JLabel pathLabel;
        JLabel deviceLabel;
        JLabel[] labels = new JLabel[]{
                pathLabel = new JLabel("选择的ADB路径将显示在这里"),
                deviceLabel = new JLabel("设备device id")
        };

        JButton refreshButton;
        JButton dirButton;
        JComponent[] components = new JComponent[]{
                dirButton = new JButton("选择文件夹"),
                refreshButton = new JButton("刷新device")
        };

        refreshButton.addActionListener(e -> {
            String adbPath = pathLabel.getText();
            deviceLabel.setText(devices(adbPath).toString());
        });

        // 选择文件夹（仅目录）
        dirButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("选择ADB所在路径");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                String path = chooser.getSelectedFile().getAbsolutePath();
                pathLabel.setText(path);
            }
        });

        // 布局
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // 组件间距
        gbc.fill = GridBagConstraints.HORIZONTAL; // 水平填充
        gbc.gridx = 0;
        gbc.gridy = 0;
        for (int i = 0;i < labels.length;i++) {
            panel.add(labels[i], gbc);
            gbc.gridx++;
            panel.add(components[i], gbc);
            if (gbc.gridx == 1) {
                gbc.gridx = 0;
            }
            gbc.gridy++;
        }

        this.add(panel);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }
    public static void main(String[] args) {
        new GUI();
    }

    public ArrayList<String> devices(String adbPath) {
        ArrayList<String> devicesId = new ArrayList<>();
        ArrayList<String> cmdStr = execCMD(adbPath + "/adb devices");
        for (String s : cmdStr) {
            if (s.indexOf("\t") > 0) {
                System.out.println(devicesId.add(s.replace("\tdevice", "")));
            }
        }
        return devicesId;
    }

    public ArrayList<String> execCMD(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            ArrayList<String> cmdOutput = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                cmdOutput.add(line);
            }
            // 等待命令执行完成
            int exitCode = process.waitFor();
            System.out.println("退出码: " + exitCode);
            return cmdOutput;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}