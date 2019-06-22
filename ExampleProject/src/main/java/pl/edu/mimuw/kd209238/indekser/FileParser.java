package pl.edu.mimuw.kd209238.indekser;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileParser {
    public static String parseFile(Path file) {
        Parser parser = new AutoDetectParser();
        ContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        try(InputStream stream = Files.newInputStream(file)) {
            parser.parse(stream, handler, metadata, context);
            return handler.toString();
        }
        catch (Exception e) {
            return null;
        }
    }
}
