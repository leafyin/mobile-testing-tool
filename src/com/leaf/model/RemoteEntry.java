package com.leaf.model;

public class RemoteEntry {

    private final String name;
    private final String path;
    private final boolean directory;

    public RemoteEntry(String name, String path, boolean directory) {
        this.name = name;
        this.path = path;
        this.directory = directory;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isDirectory() {
        return directory;
    }

    public boolean isMediaFile() {
        if (directory) {
            return false;
        }
        String lower = name.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp")
                || lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".avi")
                || lower.endsWith(".mov") || lower.endsWith(".3gp");
    }

    @Override
    public String toString() {
        return (directory ? "[目录] " : "[文件] ") + name;
    }
}
