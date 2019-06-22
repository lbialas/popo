package pl.edu.mimuw.kd209238.indekser;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import ucar.nc2.util.IO;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class IndexFiles {

    private IndexFiles() {}

    /** Index all text files under a directory. */
    //docDir is not NULL
    public static void addToIndeks(Path docDir) {
        String indexPath = System.getProperty("user.home") + "\\.index";

        if (!Files.isReadable(docDir)) {
            System.out.println("Fatal error, irredable or nonexistant directory");
            System.exit(1);
        }
        try {
            indexDocs(docDir, indexPath);
        } catch (IOException e) {
            System.out.println("IOException");
        }
    }

    static void indexDocs(Path path, String indexPath) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        indexDoc(file, indexPath);
                    } catch (IOException ignore) {
                        // don't index files that can't be read.
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDoc(path, indexPath);
        }
    }

    /** Indexes a single document */
    static void indexDoc(Path file, String indexPath) throws IOException {
        /* make a new, empty document */
        Document doc = new Document();

        Field pathField = new StringField("path", file.toString(), Field.Store.YES);
        doc.add(pathField);

        doc.add(new TextField("name", file.getFileName().toString(), Field.Store.YES));

        String parsedFile = FileParser.parseFile(file);
        //todo co jak null?
        if(parsedFile != null)
            doc.add(new TextField("contents", parsedFile, Field.Store.YES));
        else
            return;



        Directory dir = FSDirectory.open(Paths.get(indexPath));
        Analyzer analyzer;
        if(recognizeLanguage(parsedFile))
            analyzer = new EnglishAnalyzer();
        else
            analyzer = new PolishAnalyzer();

        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        // Add new documents to an existing index, or creates the new one:
        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
        IndexWriter writer = new IndexWriter(dir, iwc);
        writer.updateDocument(new Term("path", file.toString()), doc);
        writer.close();
    }

    static void removeDoc(Path file, String indexPath) throws IOException {

        Directory dir = FSDirectory.open(Paths.get(indexPath));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
        IndexWriter writer = new IndexWriter(dir, iwc);
        writer.deleteDocuments(new Term("path", file.toString()));
        writer.close();
    }

    static void removeDocs(Path path, String indexPath) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        removeDoc(file, indexPath);
                    } catch (IOException ignore) {
                        // don't index files that can't be read.
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            removeDoc(path, indexPath);
        }
    }

    static void deleteAll() throws IOException{
        String indexPath = System.getProperty("user.home") + File.separator + ".index";

        Directory dir = FSDirectory.open(Paths.get(indexPath));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
        IndexWriter writer = new IndexWriter(dir, iwc);
        writer.deleteAll();
        writer.close();
    }

    static String[] getPaths() throws IOException {
        String pathsPath = System.getProperty("user.home") + File.separator
                + ".index" + File.separator + ".paths" + File.separator + "path.txt";
        ArrayList<String> paths = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(pathsPath))) {
            while (true) {
                String currentLine = reader.readLine();
                if(currentLine != null) {
                    paths.add(currentLine);
                }
                else
                    break;
            }
        }

        return paths.toArray(new String[paths.size()]);
    }

    private static boolean recognizeLanguage(String content) {
        LanguageDetector languageDetector =
                new OptimaizeLangDetector().loadModels();
        languageDetector.addText(content);
        LanguageResult result = languageDetector.detect();
        String s = result.getLanguage();
        return !s.equals("pl");
    }
}
