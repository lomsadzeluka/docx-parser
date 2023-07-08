public class ReadDocx {

    public static void main(String[] args) {
        new ReadDocx().run();
    }

    public void run() {
        ChapterProcessor chapterProcessor = new ChapterProcessor();
        BibleDataInserter bibleDataInserter = new BibleDataInserter();

        Integer totalChapterCount = 21;
        for (int chapter = 1; chapter <= totalChapterCount; chapter++) {
            String fileName = "upload/" + chapter + ".docx";
            chapterProcessor.processDocument(chapter, fileName);
        }
        Integer bookNumber = 490;
        bibleDataInserter.insertDataIntoDb(bookNumber, chapterProcessor);
    }
}
