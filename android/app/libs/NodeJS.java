package org.nodejs.mobile;

/**
 * Minimal NodeJS class interface for Android.
 * This is a stub that matches the nodejs-mobile API.
 */
public class NodeJS {
    private static NodeJS instance;
    
    public NodeJS(android.content.Context context, NodeAppListener listener) {
        // Initialize nodejs-mobile
    }
    
    public void setWorkingDirectory(String dir) {
        // Set working directory
    }
    
    public void start(java.util.List<String> arguments) {
        // Start node.js with arguments
    }
    
    public void stop() {
        // Stop node.js
    }
    
    public static NodeJS getInstance() {
        return instance;
    }
}