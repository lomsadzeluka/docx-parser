import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChapterProcessor {
    public final Map<Integer, Map<Integer, Map<Integer, String>>> chapterHeaderMappings = new HashMap<>();
    public Map<Integer, Map<Integer, String>> chapterHeaderIndexTextMapping = new HashMap<>();
    public Map<Integer, Map<Integer, Integer>> chapterHeaderFirstVerses = new HashMap<>();

    private static final Pattern VERSE_PATTERN = Pattern.compile("(\\d+)(.*)");
    private static final Pattern LIBRARY_PATTERN = Pattern.compile("(\\[?(\\d*)\\]?|\\*)(.*)");
    private Map<Integer, Map<Integer, String>> headerVerseMappings = new HashMap<>();
    private Map<Integer, String> verseMappings = new HashMap<>();

    private int libraryWordIndex = 0;
    private final Map<Integer, String> libraryMappings = new HashMap<>();
    private int currentHeaderIndex = 0;

    private Map<Integer, Integer> headerFirstVerses = new HashMap<>();

    private boolean firstSubheaderFound = false;

    void processDocument(int chapter, String fileName) {
        // Initialize for new chapter
        headerVerseMappings = new HashMap<>();
        headerFirstVerses = new HashMap<>();
        currentHeaderIndex = 0;
        firstSubheaderFound = false;
        verseMappings = new HashMap<>(); // Initialize verseMappings for the new chapter
        headerVerseMappings.put(currentHeaderIndex, verseMappings); // associate it with the currentHeaderIndex (which is 0 at this point)

        chapterHeaderMappings.put(chapter, headerVerseMappings);
        chapterHeaderFirstVerses.put(chapter, headerFirstVerses);

        try (FileInputStream fis = new FileInputStream(fileName)) {
            XWPFDocument document = new XWPFDocument(fis);
            List<XWPFParagraph> paragraphs = document.getParagraphs();

            boolean firstLine = true;
            for (XWPFParagraph para : paragraphs) {
                String text = para.getText().trim();
                if (firstLine) {
                    System.out.println("\n" + text + "\n");
                    firstLine = false;
                    continue;
                }
                processParagraph(chapter, text);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process document", e);
        }
    }

    private void processParagraph(Integer chapter, String text) {
        if (text.isEmpty()) {
            return;
        }

        if (Character.isDigit(text.charAt(0))) {
            processVerse(text);
        } else if (text.charAt(0) == '[' || text.charAt(0) == '*') {
            processLibraryWord(text);
        } else {
            processSubHeader(chapter, text);
        }
    }

    private void processVerse(String text) {
        Matcher matcher = VERSE_PATTERN.matcher(text);
        if (matcher.find()) {
            int verseNumber = Integer.parseInt(matcher.group(1));
            String verse = matcher.group(2).trim();
            verseMappings.put(verseNumber, verse);

            // If the first verse for the current header hasn't been set yet, set it
            if (firstSubheaderFound && (headerFirstVerses.get(currentHeaderIndex) == null || headerFirstVerses.get(currentHeaderIndex) == -1)) {
                headerFirstVerses.put(currentHeaderIndex, verseNumber);
            }

            System.out.println(verseNumber + " " + verse);
        } else {
            throw new RuntimeException("Matcher failed for verse!");
        }
    }

    private void processLibraryWord(String text) {
        Matcher matcher = LIBRARY_PATTERN.matcher(text);
        if (matcher.find()) {
            libraryWordIndex += 1;
            libraryMappings.put(libraryWordIndex, text);
        }
    }

    private void processSubHeader(Integer chapter, String text) {
        firstSubheaderFound = true;
        currentHeaderIndex += 1;
        verseMappings = new HashMap<>();
        headerFirstVerses.put(currentHeaderIndex, -1);  // Initialize with -1
        headerVerseMappings.put(currentHeaderIndex, verseMappings);

        if (chapterHeaderIndexTextMapping.get(chapter) == null) {
            Map<Integer, String> subHeaderTitleMapping = new HashMap<>();
            chapterHeaderIndexTextMapping.put(chapter, subHeaderTitleMapping);
        }
        Map<Integer, String> subHeaderTitleMapping = chapterHeaderIndexTextMapping.get(chapter);
        subHeaderTitleMapping.put(currentHeaderIndex, text);
        System.out.println("\n[" + currentHeaderIndex + "] " + text);
    }
}
