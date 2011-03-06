package com.mysticx.bukkit.backupplugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Usage of following class can go as ...
 * <P>
 *
 * <PRE>
 * <CODE>
 * 		SysCommandExecutor cmdExecutor = new SysCommandExecutor();
 * 		cmdExecutor.setOutputLogDevice(new LogDevice());
 * 		cmdExecutor.setErrorLogDevice(new LogDevice());
 * 		int exitStatus = cmdExecutor.runCommand(commandLine);
 * </CODE>
 * </PRE>
 *
 * </P>
 *
 * OR
 *
 * <P>
 *
 * <PRE>
 * <CODE>
 * 		SysCommandExecutor cmdExecutor = new SysCommandExecutor();
 * 		int exitStatus = cmdExecutor.runCommand(commandLine);
 *
 * 		String cmdError = cmdExecutor.getCommandError();
 * 		String cmdOutput = cmdExecutor.getCommandOutput();
 * </CODE>
 * </PRE>
 *
 * </P>
 *
 * http://www.javalobby.org/java/forums/t53333.html
 *
 * @author Venkat
 *
 */
public final class SysCommandExecutor {
    private String fWorkingDirectory = null;
    private List<EnvironmentVar> fEnvironmentVarList = null;

    private StringBuffer fCmdOutput = null;
    private StringBuffer fCmdError = null;
    private AsyncStreamReader fCmdOutputThread = null;
    private AsyncStreamReader fCmdErrorThread = null;

    public void setWorkingDirectory(String workingDirectory) {
        fWorkingDirectory = workingDirectory;
    }

    public void setEnvironmentVar(String name, String value) {
        if (fEnvironmentVarList == null) {
            fEnvironmentVarList = new ArrayList<EnvironmentVar>();
        }

        fEnvironmentVarList.add(new EnvironmentVar(name, value));
    }

    public String getCommandOutput() {
        return fCmdOutput.toString();
    }

    public String getCommandError() {
        return fCmdError.toString();
    }

    public int runCommand(String commandLine) throws Exception {
        /* run command */
        Process process = runCommandHelper(commandLine);

        /* start output and error read threads */
        startOutputAndErrorReadThreads(process.getInputStream(), process.getErrorStream());

        /* wait for command execution to terminate */
        int exitStatus = -1;
        try {
            exitStatus = process.waitFor();

        } catch (InterruptedException ex) {
            throw new Exception(ex);

        } finally {
            /* notify output and error read threads to stop reading */
            notifyOutputAndErrorReadThreadsToStopReading();
        }

        return exitStatus;
    }

    private Process runCommandHelper(String commandLine) throws IOException {
        Process process = null;
        if (fWorkingDirectory == null) {
            process = Runtime.getRuntime().exec(commandLine, getEnvTokens());
        }
        else {
            process = Runtime.getRuntime().exec(commandLine, getEnvTokens(), new File(fWorkingDirectory));
        }

        return process;
    }

    private void startOutputAndErrorReadThreads(InputStream processOut, InputStream processErr) {
        fCmdOutput = new StringBuffer();
        fCmdOutputThread = new AsyncStreamReader(processOut, fCmdOutput, "OUTPUT");
        fCmdOutputThread.start();

        fCmdError = new StringBuffer();
        fCmdErrorThread = new AsyncStreamReader(processErr, fCmdError, "ERROR");
        fCmdErrorThread.start();
    }

    private void notifyOutputAndErrorReadThreadsToStopReading() {
        fCmdOutputThread.stopReading();
        fCmdErrorThread.stopReading();
    }

    private String[] getEnvTokens() {
        if (fEnvironmentVarList == null) {
            return new String[0];
        }

        String[] envTokenArray = new String[fEnvironmentVarList.size()];
        Iterator<EnvironmentVar> envVarIter = fEnvironmentVarList.iterator();
        int nEnvVarIndex = 0;
        while (envVarIter.hasNext()) {
            EnvironmentVar envVar = (EnvironmentVar) (envVarIter.next());
            String envVarToken = envVar.getfName() + "=" + envVar.getfValue();
            envTokenArray[nEnvVarIndex++] = envVarToken;
        }

        return envTokenArray;
    }
}

/**
 * http://www.javalobby.org/java/forums/t53333.html
 *
 * @author Venkat
 *
 */
class EnvironmentVar {
    private String fName = null;
    private String fValue = null;

    public EnvironmentVar(String name, String value) {
        fName = name;
        fValue = value;
    }

    public String getfName() {
        return fName;
    }

    public String getfValue() {
        return fValue;
    }
}
