package com.comp4321.spider.util;

import com.comp4321.spider.store.PageRecord;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class accepts webpage data and packages them into one object
 */

public class WebpageData
{
    public String title;
    public String url;
    public String lastModDate;
    public String sizeChars;
    public String[] keywords;
    public Object[] freq;
    public String[] childLinks;

    public boolean initialized;

    // Constructor
    public WebpageData() {
        this.initialized = false;
    }
    public WebpageData(String pageTitle, String url, String lastModDate, String sizeOfPage,
                       String[] keywords, int[] freq, String[] childLinks) {
        this.title = pageTitle;
        this.url = url;
        this.lastModDate = lastModDate;
        this.sizeChars = sizeOfPage;
        this.keywords = keywords;
        this.freq = freq;
        this.childLinks = childLinks;
        checkInitialized();
    }
    public WebpageData(String pageTitle, String url, String lastModDate, String sizeOfPage,
                       String[] keywords, String[] freq, String[] childLinks) {
        this.title = pageTitle;
        this.url = url;
        this.lastModDate = lastModDate;
        this.sizeChars = sizeOfPage;
        this.keywords = keywords;
        this.freq = freq;
        this.childLinks = childLinks;
        checkInitialized();
    }
    public WebpageData(PageRecord page) {
        this.title = page.title;
        this.url = page.url;
        this.lastModDate = (page.lastModifiedRfc1123 == null || page.lastModifiedRfc1123.isBlank()) ? "N/A" : page.lastModifiedRfc1123;
        this.sizeChars = String.valueOf(page.sizeChars);
        this.childLinks = page.outLinks.toArray(new String[0]);
        // Keywords and frequencies are not available in PageRecord, so set to null
        this.keywords = null; 
        this.freq = null;
        this.initialized = false;
    }
    public WebpageData(PageRecord page, String[] keywords, String[] freq) {
        this.title = page.title;
        this.url = page.url;
        this.lastModDate = (page.lastModifiedRfc1123 == null || page.lastModifiedRfc1123.isBlank()) ? "N/A" : page.lastModifiedRfc1123;
        this.sizeChars = String.valueOf(page.sizeChars);
        this.childLinks = page.outLinks.toArray(new String[0]);
        // Keywords and frequencies are not available in PageRecord, so set to null
        this.keywords = keywords; 
        this.freq = freq;
        this.initialized = true;
    }
    public WebpageData(PageRecord page, String[] keywords, int[] freq) {
        this.title = page.title;
        this.url = page.url;
        this.lastModDate = (page.lastModifiedRfc1123 == null || page.lastModifiedRfc1123.isBlank()) ? "N/A" : page.lastModifiedRfc1123;
        this.sizeChars = String.valueOf(page.sizeChars);
        this.childLinks = page.outLinks.toArray(new String[0]);
        // Keywords and frequencies are not available in PageRecord, so set to null
        this.keywords = keywords; 
        this.freq = freq;
        this.initialized = true;
    }
    public void loadWebpageData(String pageTitle, String url, String lastModDate, String sizeOfPage,
                       String[] keywords, int[] freq, String[] childLinks) {
        this.title = pageTitle;
        this.url = url;
        this.lastModDate = lastModDate;
        this.sizeChars = sizeOfPage;
        this.keywords = keywords;
        this.freq = freq;
        this.childLinks = childLinks;
        checkInitialized();
    }
    public void loadWebpageData(String pageTitle, String url, String lastModDate, String sizeOfPage,
                       String[] keywords, String[] freq, String[] childLinks) {
        this.title = pageTitle;
        this.url = url;
        this.lastModDate = lastModDate;
        this.sizeChars = sizeOfPage;
        this.keywords = keywords;
        this.freq = freq;
        this.childLinks = childLinks;
        checkInitialized();
    }

    public boolean checkInitialized() {
        if (this.title == null || this.url == null || this.lastModDate == null || this.sizeChars == null) {
            this.initialized = false;
        } else {
            this.initialized = true;
        }
        return this.initialized;
    }

    // Utility method to display info
    public void displayInfo() {
        if (!this.initialized) {
            System.err.println("WebpageData not initialized.");
            return;
        }
        System.out.println("Page Title: " + this.title);
        System.out.println("URL: " + this.url);
        System.out.println("Last Modified: " + this.lastModDate);
        System.out.println("Size of Page: " + this.sizeChars);
        if (this.keywords != null) {
            System.out.println("Keywords: " + String.join(", ", this.keywords));
        }
        if (this.freq != null) {
            System.out.print("Frequencies: ");
            for (Object f : this.freq) {
                System.out.print(f + " ");
            }
            System.out.println();
        }
        if (this.childLinks != null) {
            System.out.println("Child Links: " + String.join(", ", this.childLinks));
        }
    }
}

/**
 * This class uses the WebpageData class to write content on the txt file.
 * Optimized with session-based buffering for improved I/O performance.
 */
public class Printer
{
    private String output; // Output txt file name
    public boolean initialized;
    private int entryCount; // Track number of entries written
    private BufferedWriter sessionWriter; // Reusable writer for batch operations
    private boolean inSession; // Flag indicating if session is active
    
    /**
     * Constructor for objects of class Printer
     */
    public Printer(String output_txt_file_name)
    {
        this.output = output_txt_file_name;
        this.initialized = true;
        this.entryCount = 0;
        this.sessionWriter = null;
        this.inSession = false;
    }
    
    /**
     * Clears all content
     */
    public void initTxtFile()
    {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(this.output))) 
        {} catch (IOException e) {e.printStackTrace();}
        this.initialized = true;
        this.entryCount = 0;
    }

    /**
     * Start a session for batch writing (significantly improves performance for multiple entries).
     * Must call endSession() when done writing.
     * 
     * @throws IOException if file cannot be opened
     */
    public void startSession() throws IOException {
        if (this.inSession) {
            System.err.println("Session already active. Call endSession() first.");
            return;
        }
        this.sessionWriter = new BufferedWriter(new FileWriter(this.output, true));
        this.inSession = true;
    }

    /**
     * End the current session and flush all buffered data to disk.
     * 
     * @throws IOException if file cannot be flushed or closed
     */
    public void endSession() throws IOException {
        if (!this.inSession || this.sessionWriter == null) {
            System.err.println("No active session to end.");
            return;
        }
        this.sessionWriter.flush();
        this.sessionWriter.close();
        this.sessionWriter = null;
        this.inSession = false;
    }
    /**
     * Adds webpage content (single-write mode or batch mode)
     * For best performance with multiple entries, use startSession/endSession pattern.
     */
    public void appendWebpageData(WebpageData data)
    {
        // Sanity check
        if (!this.initialized) {
            System.err.println("Printer not initialized. Did you do some forbidden casting?");
            return;
        }
        if (!data.initialized) {
            System.err.println("WebpageData not initialized. Trying to check if initialization requirements are met...\n");
            if (!data.checkInitialized()) {
                System.err.println("WebpageData still not initialized after check. Please check your data.");
                return;
            }
        }
        
        try {
            // Use session writer if available, otherwise create temporary writer
            BufferedWriter bw = (this.inSession && this.sessionWriter != null) 
                              ? this.sessionWriter
                              : new BufferedWriter(new FileWriter(this.output, true));
            
            // Write separator between entries (after first entry)
            if (this.entryCount > 0) {
                bw.newLine();
                bw.write("========================================================================");
                bw.newLine();
            }
            
            // Page title
            bw.write(data.title);
            bw.newLine();
    
            // URL
            bw.write(data.url);
            bw.newLine();
    
            // Last modification date and size (optimized string formatting)
            bw.write(String.format("%s, %s bytes", data.lastModDate, data.sizeChars));
            bw.newLine();

            // Keywords with frequencies
            if (data.keywords != null && data.freq != null) {
                StringBuilder keywordLine = new StringBuilder();
                for (int i = 0; i < data.keywords.length; i++) {
                    keywordLine.append(data.keywords[i])
                               .append(" ")
                               .append(data.freq[i]);
                    if (i < data.keywords.length - 1) {
                        keywordLine.append("; ");
                    }
                }
                bw.write(keywordLine.toString());
                bw.newLine();
            }

            // Child links
            if (data.childLinks != null) {
                for (String link : data.childLinks) {
                    bw.write(link);
                    bw.newLine();
                }
            }

            // Only close if not in session (single-write mode)
            if (!this.inSession) {
                bw.close();
            }
            
            this.entryCount++;
        } 
        catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Adds webpage content from a PageRecord object (single-write or batch mode)
     */
    public void appendWebpageData(PageRecord record)
    {
        if (!this.initialized) {
            System.err.println("Printer not initialized.");
            return;
        }
        if (record == null || record.title == null || record.title.isEmpty()) {
            System.err.println("Invalid PageRecord provided.");
            return;
        }
        
        try {
            // Use session writer if available, otherwise create temporary writer
            BufferedWriter bw = (this.inSession && this.sessionWriter != null)
                              ? this.sessionWriter
                              : new BufferedWriter(new FileWriter(this.output, true));
            
            // Write separator between entries
            if (this.entryCount > 0) {
                bw.newLine();
                bw.write("========================================================================");
                bw.newLine();
            }
            
            bw.write(record.title);
            bw.newLine();
            bw.write(record.url);
            bw.newLine();
            
            String lastMod = (record.lastModifiedRfc1123 != null && !record.lastModifiedRfc1123.isBlank())
                           ? record.lastModifiedRfc1123 : "N/A";
            bw.write(String.format("%s, %d bytes", lastMod, record.sizeChars));
            bw.newLine();
            
            if (record.outLinks != null && !record.outLinks.isEmpty()) {
                for (String link : record.outLinks) {
                    bw.write(link);
                    bw.newLine();
                }
            }

            // Only close if not in session
            if (!this.inSession) {
                bw.close();
            }

            this.entryCount++;
        }
        catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Batch append multiple WebpageData objects efficiently using session buffering.
     * Much faster than calling appendWebpageData() in a loop.
     * 
     * @param dataList List of WebpageData objects to write
     * @throws IOException if batch operation fails
     */
    public void appendWebpageDataBatch(java.util.List<WebpageData> dataList) throws IOException {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }
        
        this.startSession();
        for (WebpageData data : dataList) {
            this.appendWebpageData(data);
        }
        this.endSession();
    }

    /**
     * Batch append multiple PageRecord objects efficiently using session buffering.
     * Much faster than calling appendWebpageData(PageRecord) in a loop.
     * 
     * @param recordList List of PageRecord objects to write
     * @throws IOException if batch operation fails
     */
    public void appendPageRecordBatch(java.util.List<PageRecord> recordList) throws IOException {
        if (recordList == null || recordList.isEmpty()) {
            return;
        }
        
        this.startSession();
        for (PageRecord record : recordList) {
            this.appendWebpageData(record);
        }
        this.endSession();
    }

    /**
     * Get the total number of entries written in this session.
     * 
     * @return Number of entries written
     */
    public int getEntryCount() {
        return this.entryCount;
    }

    /**
     * Reset entry counter (typically after calling initTxtFile()).
     */
    public void resetEntryCount() {
        this.entryCount = 0;
    }