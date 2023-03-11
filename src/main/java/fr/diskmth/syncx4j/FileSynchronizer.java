package fr.diskmth.syncx4j;

import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class FileSynchronizer
{
    private static final String SERVER_HOSTNAME = "185.157.247.2";
    private static final int SERVER_PORT = 21;
    private static final String SERVER_USERNAME = "zyalisfr1";
    private static final String SERVER_PASSWORD = "s6p$e1F81";
    private static final String SERVER_DIRECTORY = "error_docs";

    private static final String CLIENT_DIRECTORY = "C:/Users/gille/Dev/projects/Java/SyncX4j/test";

    public static void main(String[] args) throws Exception
    {
        final Long start = System.currentTimeMillis();
        final SyncXClient syncXClient = new SyncXClient(SERVER_HOSTNAME, SERVER_PORT, SERVER_USERNAME, SERVER_PASSWORD, true);
        syncXClient.login();

        System.out.println("#####");

        final List<SyncXFile> remoteFiles = syncXClient.getRemoteFiles(SERVER_DIRECTORY);
        remoteFiles.forEach((file) -> System.out.println(file.getPath() + " -> " + file.getName() + " -> " + file.getSize()));

        System.out.println("#####");

        /*final HashMap<String, List<File>> localFiles = syncXClient.getLocalFiles(CLIENT_DIRECTORY);
        localFiles.forEach((key, value) -> value.forEach((file) -> System.out.println(key + " -> " + file.getName())));

        System.out.println("#####");

        final HashMap<String, Long> filesToDownload = syncXClient.getFilesToDownload(remoteFiles, localFiles);
        filesToDownload.forEach((key, value) -> System.out.println(key + " -> " + value));

        System.out.println("#####");

        final HashMap<String, Long> filesToDelete = syncXClient.getFilesToDelete(remoteFiles, localFiles);
        filesToDelete.forEach((key, value) -> System.out.println(key + " -> " + value));

        System.out.println("#####");*/

        final Long end = System.currentTimeMillis();
        System.out.println("Time elapsed: " + (end - start) + "ms or " + (end - start) / 1000 + "s");
        syncXClient.logout();
    }
}

