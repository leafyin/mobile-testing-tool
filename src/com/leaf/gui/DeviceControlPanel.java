package com.leaf.gui;

import com.leaf.model.RemoteEntry;
import com.leaf.service.AdbService;
import com.leaf.service.ScrcpyService;
import com.leaf.utils.ConfigUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DeviceControlPanel extends JPanel {

    private static final int KEYCODE_HOME = 3;
    private static final int KEYCODE_BACK = 4;
    private static final int KEYCODE_POWER = 26;
    private static final int KEYCODE_ENTER = 66;

    private final JLabel deviceLabel = new JLabel("未选择设备");
    private final JLabel pathLabel = new JLabel(AdbService.SD_CARD_PATH);
    private final DefaultListModel<RemoteEntry> fileModel = new DefaultListModel<>();
    private final JList<RemoteEntry> fileList = new JList<>(fileModel);
    private final JPanel controlContent = new JPanel(new BorderLayout(8, 8));
    private final JButton previewButton = new JButton("开始预览");
    private final JLabel exportPathLabel = new JLabel();
    private final DefaultComboBoxModel<String> inputHistoryModel = new DefaultComboBoxModel<>();
    private final JComboBox<String> inputCombo = new JComboBox<>(inputHistoryModel);
    private final ScrcpyService scrcpyService = new ScrcpyService();
    private final Timer previewStateTimer;

    private AdbService adbService;
    private String scrcpyPath;
    private String currentPath = AdbService.SD_CARD_PATH;

    public DeviceControlPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        previewStateTimer = new Timer(1000, e -> updatePreviewButtonState());
        previewStateTimer.start();
        previewButton.addActionListener(e -> togglePreview());

        inputCombo.setEditable(true);
        for (String item : ConfigUtil.loadInputHistory()) {
            inputHistoryModel.addElement(item);
        }
        JTextField inputEditor = (JTextField) inputCombo.getEditor().getEditorComponent();
        inputEditor.addActionListener(e -> sendInputText());

        String savedExportPath = ConfigUtil.loadExportPath();
        if (savedExportPath != null && !savedExportPath.isBlank()) {
            exportPathLabel.setText(savedExportPath);
        } else {
            exportPathLabel.setText("未设置");
        }

        controlContent.add(buildPlaceholder(), BorderLayout.CENTER);
        add(controlContent, BorderLayout.CENTER);
    }

    public void setScrcpyPath(String scrcpyPath) {
        this.scrcpyPath = scrcpyPath;
    }

    public void setDevice(AdbService service) {
        if (scrcpyService.isRunning()) {
            if (service == null || !service.getDeviceId().equals(scrcpyService.getRunningDeviceId())) {
                scrcpyService.stop();
            }
        }

        this.adbService = service;
        if (service == null) {
            controlContent.removeAll();
            controlContent.add(buildPlaceholder(), BorderLayout.CENTER);
            controlContent.revalidate();
            controlContent.repaint();
            updatePreviewButtonState();
            return;
        }

        deviceLabel.setText("当前设备: " + service.getDeviceId());
        currentPath = AdbService.SD_CARD_PATH;
        pathLabel.setText(currentPath);

        controlContent.removeAll();
        controlContent.add(buildControlPanel(), BorderLayout.CENTER);
        controlContent.revalidate();
        controlContent.repaint();

        refreshFiles();
        updatePreviewButtonState();
    }

    public void stopPreview() {
        scrcpyService.stop();
        updatePreviewButtonState();
    }

    private JPanel buildPlaceholder() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(new JLabel("请在左侧选择一台设备"));
        return panel;
    }

    private JPanel buildControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel header = new JPanel(new BorderLayout(8, 4));
        header.add(deviceLabel, BorderLayout.WEST);

        JPanel previewPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        previewPanel.add(previewButton);
        header.add(previewPanel, BorderLayout.EAST);
        header.add(buildKeyEventPanel(), BorderLayout.SOUTH);

        JPanel topSection = new JPanel(new BorderLayout(8, 8));
        topSection.add(header, BorderLayout.NORTH);
        topSection.add(buildTextInputPanel(), BorderLayout.SOUTH);

        panel.add(topSection, BorderLayout.NORTH);
        panel.add(buildFilePanel(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTextInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBorder(BorderFactory.createTitledBorder("输入文字"));

        inputCombo.setPrototypeDisplayValue("这是一条用于估算宽度的历史输入示例文本");
        JButton sendButton = new JButton("发送");
        sendButton.addActionListener(e -> sendInputText());

        panel.add(inputCombo, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildKeyEventPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("设备控制"));

        panel.add(actionButton("Home", () -> adbService.keyEvent(KEYCODE_HOME)));
        panel.add(actionButton("返回", () -> adbService.keyEvent(KEYCODE_BACK)));
        panel.add(actionButton("电源/关屏", () -> adbService.keyEvent(KEYCODE_POWER)));
        panel.add(actionButton("确认", () -> adbService.keyEvent(KEYCODE_ENTER)));

        return panel;
    }

    private JPanel buildFilePanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("SDCard 文件"));

        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedEntry();
                }
            }
        });

        JPanel toolbar = new JPanel(new BorderLayout(6, 0));
        toolbar.add(pathLabel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton upButton = new JButton("上级目录");
        JButton refreshButton = new JButton("刷新");
        JButton exportButton = new JButton("批量导出");
        JButton mediaFilterButton = new JButton("仅媒体");
        JButton deleteButton = new JButton("删除");
        JButton exportDirButton = new JButton("导出目录");

        upButton.addActionListener(e -> navigateUp());
        refreshButton.addActionListener(e -> refreshFiles());
        exportButton.addActionListener(e -> exportSelectedFiles());
        mediaFilterButton.addActionListener(e -> showMediaOnly());
        deleteButton.addActionListener(e -> deleteSelectedFiles());
        exportDirButton.addActionListener(e -> chooseExportDirectory());

        buttons.add(upButton);
        buttons.add(refreshButton);
        buttons.add(mediaFilterButton);
        buttons.add(exportButton);
        buttons.add(deleteButton);
        buttons.add(exportDirButton);
        toolbar.add(buttons, BorderLayout.EAST);

        JPanel northPanel = new JPanel(new BorderLayout(0, 4));
        northPanel.add(toolbar, BorderLayout.NORTH);

        JPanel exportDirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        exportDirPanel.add(new JLabel("导出位置: "));
        exportDirPanel.add(exportPathLabel);
        northPanel.add(exportDirPanel, BorderLayout.SOUTH);

        panel.add(northPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(fileList), BorderLayout.CENTER);
        return panel;
    }

    private JButton actionButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> {
            if (adbService == null) {
                return;
            }
            action.run();
        });
        return button;
    }

    private void togglePreview() {
        if (adbService == null) {
            return;
        }
        if (scrcpyService.isRunning()) {
            scrcpyService.stop();
            updatePreviewButtonState();
            return;
        }

        try {
            scrcpyService.start(scrcpyPath, adbService.getAdbPath(), adbService.getDeviceId(), message -> SwingUtilities.invokeLater(() -> {
                updatePreviewButtonState();
                JOptionPane.showMessageDialog(
                        DeviceControlPanel.this,
                        message,
                        "预览失败",
                        JOptionPane.ERROR_MESSAGE
                );
            }));
            updatePreviewButtonState();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "预览失败",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void updatePreviewButtonState() {
        boolean running = scrcpyService.isRunning()
                && adbService != null
                && adbService.getDeviceId().equals(scrcpyService.getRunningDeviceId());
        previewButton.setText(running ? "停止预览" : "开始预览");
    }

    private void sendInputText() {
        if (adbService == null) {
            return;
        }
        Object item = inputCombo.getEditor().getItem();
        String text = item == null ? "" : item.toString().trim();
        if (text.isEmpty()) {
            return;
        }
        adbService.inputText(text);
        addToInputHistory(text);
    }

    private void addToInputHistory(String text) {
        for (int i = inputHistoryModel.getSize() - 1; i >= 0; i--) {
            if (text.equals(inputHistoryModel.getElementAt(i))) {
                inputHistoryModel.removeElementAt(i);
            }
        }
        inputHistoryModel.insertElementAt(text, 0);
        while (inputHistoryModel.getSize() > ConfigUtil.MAX_INPUT_HISTORY) {
            inputHistoryModel.removeElementAt(inputHistoryModel.getSize() - 1);
        }
        inputCombo.setSelectedIndex(0);
        inputCombo.getEditor().setItem(text);

        List<String> history = new ArrayList<>();
        for (int i = 0; i < inputHistoryModel.getSize(); i++) {
            history.add(inputHistoryModel.getElementAt(i));
        }
        ConfigUtil.saveInputHistory(history);
    }

    private void navigateUp() {
        currentPath = AdbService.parentPath(currentPath);
        pathLabel.setText(currentPath);
        refreshFiles();
    }

    private void openSelectedEntry() {
        RemoteEntry selected = fileList.getSelectedValue();
        if (selected == null) {
            return;
        }
        if (selected.isDirectory()) {
            currentPath = selected.getPath();
            pathLabel.setText(currentPath);
            refreshFiles();
        }
    }

    private void refreshFiles() {
        if (adbService == null) {
            return;
        }
        fileModel.clear();
        AdbService.ListDirectoryResult result = adbService.listDirectory(currentPath);
        for (RemoteEntry entry : result.entries) {
            fileModel.addElement(entry);
        }
        if (!result.success) {
            JOptionPane.showMessageDialog(this, "无法访问目录: " + currentPath, "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showMediaOnly() {
        if (adbService == null) {
            return;
        }
        fileModel.clear();
        AdbService.ListDirectoryResult result = adbService.listDirectory(currentPath);
        for (RemoteEntry entry : result.entries) {
            if (entry.isDirectory() || entry.isMediaFile()) {
                fileModel.addElement(entry);
            }
        }
    }

    private void chooseExportDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择默认导出目录");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String saved = ConfigUtil.loadExportPath();
        if (saved != null && !saved.isBlank()) {
            chooser.setCurrentDirectory(new File(saved));
        }
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String dir = chooser.getSelectedFile().getAbsolutePath();
        exportPathLabel.setText(dir);
        ConfigUtil.saveExportPath(dir);
    }

    private void exportSelectedFiles() {
        List<RemoteEntry> selected = fileList.getSelectedValuesList();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择一个或多个文件");
            return;
        }

        List<String> filePaths = selected.stream()
                .filter(entry -> !entry.isDirectory())
                .map(RemoteEntry::getPath)
                .collect(Collectors.toList());

        long dirCount = selected.stream().filter(RemoteEntry::isDirectory).count();
        if (filePaths.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择文件，不支持导出目录");
            return;
        }

        String savedExportPath = ConfigUtil.loadExportPath();
        String localDir;
        if (savedExportPath != null && !savedExportPath.isBlank()) {
            localDir = savedExportPath;
        } else {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("选择导出目录");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            localDir = chooser.getSelectedFile().getAbsolutePath();
        }

        int successCount = adbService.pullMultiple(filePaths, localDir);
        int failCount = filePaths.size() - successCount;

        StringBuilder message = new StringBuilder();
        message.append("成功导出 ").append(successCount).append(" / ").append(filePaths.size()).append(" 个文件到:\n").append(localDir);
        if (dirCount > 0) {
            message.append("\n已跳过 ").append(dirCount).append(" 个目录");
        }
        if (failCount > 0) {
            message.append("\n失败 ").append(failCount).append(" 个文件");
        }

        JOptionPane.showMessageDialog(
                this,
                message.toString(),
                failCount > 0 ? "部分导出失败" : "导出完成",
                failCount > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void deleteSelectedFiles() {
        List<RemoteEntry> selected = fileList.getSelectedValuesList();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择一个或多个文件");
            return;
        }

        List<RemoteEntry> files = selected.stream()
                .filter(entry -> !entry.isDirectory())
                .collect(Collectors.toList());

        if (files.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择文件（暂不支持删除目录）");
            return;
        }

        StringBuilder fileListMsg = new StringBuilder("确定要删除以下 ").append(files.size()).append(" 个文件吗？\n\n");
        for (RemoteEntry f : files) {
            fileListMsg.append("  - ").append(f.getName()).append("\n");
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                fileListMsg.toString(),
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        List<String> paths = files.stream()
                .map(RemoteEntry::getPath)
                .collect(Collectors.toList());

        int successCount = adbService.deleteFiles(paths);
        int failCount = paths.size() - successCount;

        JOptionPane.showMessageDialog(
                this,
                "成功删除 " + successCount + " / " + paths.size() + " 个文件"
                        + (failCount > 0 ? "\n失败 " + failCount + " 个文件" : ""),
                failCount > 0 ? "部分删除失败" : "删除完成",
                failCount > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE
        );

        refreshFiles();
    }
}
