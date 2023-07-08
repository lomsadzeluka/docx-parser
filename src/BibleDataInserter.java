import java.sql.*;
import java.util.Map;

public class BibleDataInserter {
    private static final String INSERT_STORY_SQL = "INSERT INTO stories (book_number, chapter, verse, order_if_several, title) VALUES (?, ?, ?, ?, ?)";
    private static final String INSERT_VERSE_SQL = "INSERT INTO verses (book_number, chapter, verse, text) VALUES (?, ?, ?, ?)";

    public void insertDataIntoDb(Integer bookNumber, ChapterProcessor chapterProcessor) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:PBTI.sqlite3")) {
            // Disable auto-commit
            conn.setAutoCommit(false);

            try {
                insertStoriesIntoDb(conn, bookNumber, chapterProcessor.chapterHeaderFirstVerses, chapterProcessor.chapterHeaderIndexTextMapping);
                insertVersesIntoDb(conn, bookNumber, chapterProcessor.chapterHeaderMappings);

                // If there is no error, commit the changes.
                conn.commit();
            } catch (SQLException e) {
                // If there is an error, roll back the changes.
                conn.rollback();
                throw new RuntimeException("Error inserting data into database", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error connecting to the database", e);
        }
    }

    private void insertStoriesIntoDb(Connection conn, Integer bookNumber, Map<Integer, Map<Integer, Integer>> chapterHeaderFirstVerses, Map<Integer, Map<Integer, String>> subheaderTitles) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_STORY_SQL)) {
            for (Map.Entry<Integer, Map<Integer, Integer>> chapterEntry : chapterHeaderFirstVerses.entrySet()) {
                int chapter = chapterEntry.getKey();
                for (Map.Entry<Integer, Integer> headerEntry : chapterEntry.getValue().entrySet()) {
                    int headerIndex = headerEntry.getKey();
                    int firstVerse = headerEntry.getValue();
                    String title = null;
                    Map<Integer, String> subheaderMap = subheaderTitles.get(chapter);
                    if (subheaderMap != null) {
                        title = subheaderMap.get(headerIndex);
                    }

                    if (title == null) {
                        throw new RuntimeException("TITLE IS NULL!");
                    }
                    // Check if the story already exists in the database
                    if (isStoryExists(conn, bookNumber, chapter, firstVerse)) {
                        // Skip insertion and continue to the next story
                        continue;
                    }

                    pstmt.setInt(1, bookNumber);
                    pstmt.setInt(2, chapter);
                    pstmt.setInt(3, firstVerse);
                    pstmt.setInt(4, headerIndex);
                    pstmt.setString(5, title);
                    pstmt.addBatch();
                }
            }

            // Execute the batch of insert statements
            pstmt.executeBatch();
        }
    }

    private void insertVersesIntoDb(Connection conn, Integer bookNumber, Map<Integer, Map<Integer, Map<Integer, String>>> chapterHeaderMappings) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_VERSE_SQL)) {
            for (Map.Entry<Integer, Map<Integer, Map<Integer, String>>> chapterEntry : chapterHeaderMappings.entrySet()) {
                int chapter = chapterEntry.getKey();
                for (Map.Entry<Integer, Map<Integer, String>> headerEntry : chapterEntry.getValue().entrySet()) {
                    for (Map.Entry<Integer, String> verseEntry : headerEntry.getValue().entrySet()) {
                        int verse = verseEntry.getKey();
                        String text = verseEntry.getValue();

                        // Check if the verse already exists in the database
                        if (isVerseExists(conn, bookNumber, chapter, verse)) {
                            // Skip insertion and continue to the next verse
                            continue;
                        }

                        pstmt.setInt(1, bookNumber);
                        pstmt.setInt(2, chapter);
                        pstmt.setInt(3, verse);
                        pstmt.setString(4, text);
                        System.out.println(bookNumber + " " + chapter + " " + verse);
                        pstmt.addBatch();
                    }
                }

                // Execute the batch of insert statements
                pstmt.executeBatch();
            }
        }
    }

    private boolean isStoryExists(Connection conn, int bookNumber, int chapter, int firstVerse) throws SQLException {
        String query = "SELECT COUNT(*) FROM stories WHERE book_number = ? AND chapter = ? AND verse = ?";
        return execute(conn, bookNumber, chapter, firstVerse, query);
    }

    private boolean isVerseExists(Connection conn, int bookNumber, int chapter, int verse) throws SQLException {
        String query = "SELECT COUNT(*) FROM verses WHERE book_number = ? AND chapter = ? AND verse = ?";
        return execute(conn, bookNumber, chapter, verse, query);
    }

    private boolean execute(Connection conn, int bookNumber, int chapter, int verse, String query) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, bookNumber);
            pstmt.setInt(2, chapter);
            pstmt.setInt(3, verse);
            try (ResultSet resultSet = pstmt.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count > 0;
                }
            }
        }
        return false;
    }
}