package com.leaf.gui;

import com.leaf.utils.CmdUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class SettingPanel extends JPanel {

    public SettingPanel() {
        super(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // 组件间距
        gbc.fill = GridBagConstraints.HORIZONTAL; // 水平填充
        gbc.gridx = 0;
        gbc.gridy = 0;

        this.setBorder(BorderFactory.createTitledBorder("Android设备列表"));

        JLabel pathLabel = new JLabel("选择的ADB路径将显示在这里");

        JButton dirButton = new JButton("选择文件夹");
        JButton refreshButton = new JButton("刷新device");

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

        refreshButton.addActionListener(e -> {
            String adbPath = pathLabel.getText();
        });

    }

    public ArrayList<String> devices(String adbPath) {
        ArrayList<String> devicesId = new ArrayList<>();
        ArrayList<String> cmdStr = CmdUtil.execCMD(adbPath + "/adb devices");
        for (String s : cmdStr) {
            if (s.indexOf("\t") > 0) {
                System.out.println(devicesId.add(s.replace("\tdevice", "")));
            }
        }
        return devicesId;
    }

}
