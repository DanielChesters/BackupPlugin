package com.mysticx.bukkit.backupplugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

/**
 * http://www.javalobby.org/java/forums/t53333.html
 *
 * @author Venkat
 *
 */
class AsyncStreamReader extends Thread {
    private StringBuffer fBuffer = null;
    private InputStream fInputStream = null;
    private String fThreadId = null;
    private boolean fStop = false;

    private String fNewLine = null;

    public AsyncStreamReader(InputStream inputStream, StringBuffer buffer, String threadId) {
        fInputStream = inputStream;
        fBuffer = buffer;
        fThreadId = threadId;

        fNewLine = System.getProperty("line.separator");
    }

    public String getBuffer() {
        return fBuffer.toString();
    }

    public void run() {
        try {
            readCommandOutput();
        } catch (Exception ex) {
        }
    }

    private void readCommandOutput() throws IOException {
        BufferedReader bufOut = new BufferedReader(new InputStreamReader(fInputStream));
        String line = null;
        while ((fStop) && ((line = bufOut.readLine()) != null)) {
            fBuffer.append(line + fNewLine);
            printToDisplayDevice(line);
        }
        bufOut.close();
    }

    public void stopReading() {
        fStop = true;
    }

    private void printToDisplayDevice(String line) {
        MessageHandler.log(Level.FINEST, "(" + fThreadId + ") " + line);
    }
}