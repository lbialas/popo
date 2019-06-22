package pl.edu.mimuw.kd209238.searcher;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.store.FSDirectory;
import org.apache.poi.ss.formula.functions.T;
import org.jline.builtins.Completers;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fusesource.jansi.AnsiConsole;
import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

public class Searcher {
    public boolean lang;
    public boolean details;
    public int limit;
    public boolean color;
    public String queryType;

    public Searcher(boolean lang, boolean details, boolean color, int limit, String queryType) {
        this.lang = lang;
        this.details = details;
        this.color = color;
        this.limit = limit;
        this.queryType = queryType;
    }

    public void doSearch(IndexSearcher searcher, Query query) throws IOException {

        Analyzer analyzer;
        if (lang) {
            Map<String, Analyzer> analyzerMap = new HashMap<>();
            analyzerMap.put("title", new EnglishAnalyzer());
            analyzerMap.put("contents", new EnglishAnalyzer());
            analyzer = new PerFieldAnalyzerWrapper(
                    new StandardAnalyzer(), analyzerMap);
        } else {
            Map<String, Analyzer> analyzerMap = new HashMap<>();
            analyzerMap.put("title", new PolishAnalyzer());
            analyzerMap.put("contents", new PolishAnalyzer());
            analyzer = new PerFieldAnalyzerWrapper(
                    new StandardAnalyzer(), analyzerMap);
        }

        TopDocs results = searcher.search(query, limit);
        ScoreDoc[] hits = results.scoreDocs;

        int numTotalHits = Math.toIntExact(results.totalHits.value);
        System.out.println("Files count: " + numTotalHits);

        UnifiedHighlighter highlighter = new UnifiedHighlighter(searcher, analyzer);
        String[] fragments = highlighter.highlight("contents", query, results);


        int end = Math.min(numTotalHits, limit);
        for (int i = 0; i < end; i++) {
            int docid = hits[i].doc;
            Document doc = searcher.doc(docid);
            String path = doc.get("path");

            if (path != null) {
                System.out.println(path);
                //todo change formatter
                if (details && color) {
                    fragments[i] = fragments[i].replaceAll("<b>", "@|red ");
                    fragments[i] = fragments[i].replaceAll("</b>", "|@");
                    AnsiConsole.systemInstall();
                    System.out.println(ansi().eraseScreen().render(fragments[i]));
                    AnsiConsole.systemUninstall();
                } else if (details) {
                    System.out.println(fragments[i]);
                }
            } else {
                System.out.println((i + 1) + ". " + "No path for this document");
            }

        }
    }

    public List<String> analyze(String text, Analyzer analyzer) throws IOException {
        List<String> result = new ArrayList<String>();
        TokenStream tokenStream = analyzer.tokenStream("contents", text);
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            result.add(attr.toString());
        }
        return result;
    }

    public Query newQuerry(String s) throws IOException {
        Map<String, Analyzer> analyzerMap = new HashMap<>();
        if (lang) {
            analyzerMap.put("title", new PolishAnalyzer());
            analyzerMap.put("contents", new PolishAnalyzer());
        } else {
            analyzerMap.put("title", new PolishAnalyzer());
            analyzerMap.put("contents", new PolishAnalyzer());
        }
        Analyzer analyzer = new PerFieldAnalyzerWrapper(
                new StandardAnalyzer(), analyzerMap);
        List<String> result = analyze(s, analyzer);

        if (result.size() == 0)
            return null;

        //todo co jak null
        if (queryType.equals("fuzzy")) {
            Term name = new Term("name", result.get(0));
            Term contents = new Term("contents", result.get(0));

            Query nameQuery = new FuzzyQuery(name);
            Query contentsQuery = new FuzzyQuery(contents);

            return new BooleanQuery.Builder()
                    .add(nameQuery, BooleanClause.Occur.SHOULD)
                    .add(contentsQuery, BooleanClause.Occur.SHOULD)
                    .build();
        } else if (queryType.equals("term")) {
            Term name = new Term("name", result.get(0));
            Term contents = new Term("contents", result.get(0));

            Query nameQuery = new TermQuery(name);
            Query contentsQuery = new TermQuery(contents);

            return new BooleanQuery.Builder()
                    .add(nameQuery, BooleanClause.Occur.SHOULD)
                    .add(contentsQuery, BooleanClause.Occur.SHOULD)
                    .build();
        } else {
            PhraseQuery.Builder builderN = new PhraseQuery.Builder();
            for (String r : result) {
                builderN.add(new Term("name", r));
            }
            Query nameQuery = builderN.build();

            PhraseQuery.Builder builderC = new PhraseQuery.Builder();
            for (String r : result) {
                builderC.add(new Term("contents", r));
            }
            Query contentsQuery = builderC.build();

            return new BooleanQuery.Builder()
                    .add(contentsQuery, BooleanClause.Occur.SHOULD)
                    .build();
        }
    }
}
