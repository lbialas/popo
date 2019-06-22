package pl.edu.mimuw.kd209238.indekser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;


public class Main {
    public static void main(String[] args) throws IOException {
        String indexPath = System.getProperty("user.home") + File.separator + ".index";
        Files.createDirectories(Paths.get(indexPath + File.separator + ".paths"));
        try {
            Files.createFile(Paths.get(indexPath + File.separator + ".paths" + File.separator + "path.txt"));
        } catch (Exception ignore) {}

        if(args.length > 0){
            if ("--purge".equals(args[0])) {
                removeAllPaths();
                IndexFiles.deleteAll();
                System.out.println("Index deleted");
                return;

            } else if ("--add".equals(args[0])) {
                String s;
                s = args[1];
                addPath(s);
                IndexFiles.indexDocs(Paths.get(s), indexPath);
                System.out.println("Directory added");
                return;
            } else if ("--rm".equals(args[0])) {
                String s;
                s = args[1];
                removePath(s);
                IndexFiles.removeDocs(Paths.get(s), indexPath);
                System.out.println("Directory removed");
                return;
            } else if ("--reindex".equals(args[0])) {
                IndexFiles.deleteAll();
                String[] paths = IndexFiles.getPaths();
                for (String path: paths) {
                    IndexFiles.indexDocs(Paths.get(path), indexPath);
                }
                return;
            } else if ("--list".equals(args[0])) {
                String[] paths = IndexFiles.getPaths();
                for (String path: paths) {
                    IndexFiles.indexDocs(Paths.get(path), indexPath);
                }
                return;
            }
        }
        else {
            WatchDir.glowna();
        }
    }

    private static void addPath(String s) throws IOException {
        String pathsPath = System.getProperty("user.home") + File.separator
                + ".index" + File.separator + ".paths" + File.separator + "path.txt";
        StringBuffer str = new StringBuffer();
        try (BufferedReader reader = new BufferedReader(new FileReader(pathsPath))) {
            while (true) {
                String currentLine = reader.readLine();
                if(currentLine != null) {
                    str.append(currentLine);
                    str.append("\n");
                }
                else
                    break;
            }
        }
        str.append(s);
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(pathsPath))) {
            bw.write(str.toString());
        }
    }

    private static void removePath(String pathToRemove) throws IOException {
        String pathsPath = System.getProperty("user.home") + File.separator
                + ".index" + File.separator + ".paths" + File.separator + "path.txt";
        StringBuffer str = new StringBuffer();
        try (BufferedReader reader = new BufferedReader(new FileReader(pathsPath))) {
            while (true) {
                String currentLine = reader.readLine();
                if(currentLine != null) {
                    if(currentLine.equals(pathToRemove)){
                        continue;
                    }
                    str.append(currentLine);
                    str.append("\n");
                }
                else
                    break;
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(pathsPath))) {
            bw.write(str.toString());
        }
    }

    private static void removeAllPaths() throws IOException {
        String pathsPath = System.getProperty("user.home") + "\\.index\\.paths\\path.txt";
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(pathsPath))) {
            bw.write("");
        }
    }
}
