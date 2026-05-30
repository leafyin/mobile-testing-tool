import com.leaf.gui.DeviceControlPanel;
import com.leaf.service.AdbService;
import com.leaf.utils.ConfigUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class GUI extends JFrame {

    private final JLabel adbPathLabel = new JLabel("未选择");
    private final JLabel scrcpyPathLabel = new JLabel("未选择");
    private final DefaultListModel<String> deviceModel = new DefaultListModel<>();
    private final JList<String> deviceList = new JList<>(deviceModel);
    private final DeviceControlPanel deviceControlPanel = new DeviceControlPanel();

    private String adbPath;
    private String scrcpyPath;

    public GUI() {
        setTitle("Android Mobile Testing Tool");
        setSize(960, 640);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        adbPath = ConfigUtil.loadAdbPath();
        if (adbPath != null && new File(adbPath, "adb").exists()) {
            adbPathLabel.setText(adbPath);
            adbPathLabel.setForeground(Color.BLACK);
        } else {
            adbPathLabel.setForeground(Color.GRAY);
        }

        scrcpyPath = ConfigUtil.loadScrcpyPath();
        if (scrcpyPath != null && !scrcpyPath.isBlank()) {
            scrcpyPathLabel.setText(scrcpyPath);
            scrcpyPathLabel.setForeground(Color.BLACK);
        } else {
            scrcpyPathLabel.setText("自动检测 PATH");
            scrcpyPathLabel.setForeground(Color.GRAY);
        }
        deviceControlPanel.setScrcpyPath(scrcpyPath);

        deviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deviceList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            updateSelectedDevice();
        });

        add(buildMainPanel());
        setLocationRelativeTo(null);
        setVisible(true);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                deviceControlPanel.stopPreview();
            }
        });

        if (adbPath != null) {
            refreshDevices();
        }
    }

    private JPanel buildMainPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(buildTopBar(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                buildDevicePanel(),
                deviceControlPanel
        );
        splitPane.setDividerLocation(280);
        splitPane.setResizeWeight(0.28);
        root.add(splitPane, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildTopBar() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("ADB 路径:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(adbPathLabel, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton adbDirButton = new JButton("选择 ADB");
        adbDirButton.addActionListener(e -> chooseAdbPath());
        panel.add(adbDirButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Scrcpy 路径:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(scrcpyPathLabel, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton scrcpyDirButton = new JButton("选择 Scrcpy");
        scrcpyDirButton.addActionListener(e -> chooseScrcpyPath());
        panel.add(scrcpyDirButton, gbc);

        return panel;
    }

    private JPanel buildDevicePanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));

        deviceList.setVisibleRowCount(12);
        JScrollPane scrollPane = new JScrollPane(deviceList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("已连接设备"));

        JButton refreshButton = new JButton("刷新 device");
        refreshButton.addActionListener(e -> refreshDevices());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(refreshButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void chooseAdbPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择 ADB 所在路径");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        adbPath = chooser.getSelectedFile().getAbsolutePath();
        adbPathLabel.setText(adbPath);
        adbPathLabel.setForeground(Color.BLACK);
        ConfigUtil.saveAdbPath(adbPath);
        refreshDevices();
    }

    private void chooseScrcpyPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择 scrcpy 可执行文件");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        scrcpyPath = chooser.getSelectedFile().getAbsolutePath();
        scrcpyPathLabel.setText(scrcpyPath);
        scrcpyPathLabel.setForeground(Color.BLACK);
        ConfigUtil.saveScrcpyPath(scrcpyPath);
        deviceControlPanel.setScrcpyPath(scrcpyPath);
    }

    private void refreshDevices() {
        if (adbPath == null || adbPath.isBlank()) {
            JOptionPane.showMessageDialog(this, "请先选择 ADB 路径");
            return;
        }

        String selected = deviceList.getSelectedValue();
        deviceModel.clear();
        List<String> ids = AdbService.listDevices(adbPath);
        for (String id : ids) {
            deviceModel.addElement(id);
        }
        if (ids.isEmpty()) {
            deviceModel.addElement("未检测到已连接设备");
            deviceControlPanel.setDevice(null);
            return;
        }

        if (selected != null && ids.contains(selected)) {
            deviceList.setSelectedValue(selected, true);
        } else {
            deviceList.setSelectedIndex(0);
        }
    }

    private void updateSelectedDevice() {
        String deviceId = deviceList.getSelectedValue();
        if (deviceId == null || deviceId.contains("未检测") || adbPath == null) {
            deviceControlPanel.setDevice(null);
            return;
        }
        deviceControlPanel.setDevice(new AdbService(adbPath, deviceId));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUI::new);
    }
}
