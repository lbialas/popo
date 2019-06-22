package pl.edu.mimuw.kd209238.searcher;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.store.FSDirectory;
import org.fusesource.jansi.AnsiConsole;
import org.jline.builtins.Completers;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static org.fusesource.jansi.Ansi.ansi;

public class Main {

    public static void main(String[] args) throws Exception{
        String index = System.getProperty("user.home") + File.separator + ".index";
        Searcher s = new Searcher(true, false, true, Integer.MAX_VALUE, "term");
        try (Terminal terminal = TerminalBuilder.builder()
                .jna(false)
                .jansi(true)
                .build()) {
            LineReader lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(new Completers.FileNameCompleter())
                    .build();
            while (true) {
                String line;
                try {
                    line = lineReader.readLine("> ");
                    String input = inputParser(line, s);
                    if(input != null) {
                        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
                        IndexSearcher searcher = new IndexSearcher(reader);

                        Query query = s.newQuerry(line);

                        s.doSearch(searcher, query);

                        reader.close();
                    }
                } catch (UserInterruptException | EndOfFileException e) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("IOException");
            System.exit(-1);
        }
    }

    private static String inputParser(String input, Searcher s) {
        if(input.startsWith("%")) {
            if (input.startsWith("%lang en")) {
                s.lang = true;
            }
            else if (input.startsWith("%lang pl")) {
                s.lang = false;
            }
            else if (input.startsWith("%details on")) {
                s.details = true;
            }
            else if (input.startsWith("%details off")) {
                s.details = false;
            }
            else if (input.startsWith("%limit")) {
                int a = parseInt(input.substring(6));
                if(a == 0) {
                    s.limit = Integer.MAX_VALUE;
                }
                else {
                    s.limit = a;
                }
            }
            else if (input.startsWith("%color on")) {
                s.color = true;
            }
            else if (input.startsWith("%color off")) {
                s.color = false;
            }
            else if (input.startsWith("%term")) {
                s.queryType = "term";
            }
            else if (input.startsWith("%phrase")) {
                s.queryType = "phrase";
            }
            else if (input.startsWith("%fuzzy")) {
                s.queryType = "fuzzy";
            } else {
                System.out.println("Wrong query!");
            }
            return null;
        }
        else {
            return input;
        }
    }
}
