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
    public WebpageData(PageRecord page, String[] keywords, Object[] freq) {
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
        if (this.title == null || this.url == null || this.lastModDate == null || this.sizeOfPage == null) {
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
        System.out.println("Page Title: " + pageTitle);
        System.out.println("URL: " + url);
        System.out.println("Last Modified: " + lastModDate);
        System.out.println("Size of Page: " + sizeOfPage);
        if (keywords != null) {
            System.out.println("Keywords: " + String.join(", ", keywords));
        }
        if (freq != null) {
            System.out.print("Frequencies: ");
            for (int f : freq) {
                System.out.print(f + " ");
            }
            System.out.println();
        }
        if (childLinks != null) {
            System.out.println("Child Links: " + String.join(", ", childLinks));
        }
    }
}

/**
 * This class uses the WebpageData class to write content on the txt file.
 */
public class Printer
{
    private String output; // Output txt file name
    public boolean initialized;
    private boolean empty;
    
    /**
     * Constructor for objects of class Printer
     */
    public Printer(String output_txt_file_name)
    {
        this.output = output_txt_file_name;
        this.initialized = true;
        this.empty = false;
    }
    
    /**
     * Clears all content
     */
    public void initTxtFile()
    {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(this.output))) 
        {} catch (IOException e) {e.printStackTrace();}
        this.initialized = true;
        this.empty = true;
    }
    /**
     * Adds webpage content
     */
    public void appendWebpageData(WebpageData data)
    {
        // Sanity check
        if (!this.initialized) {
            System.err.println("Printer not initialized. Did you do some forbiddened casting?");
            return;
        }
        if (!data.initialized) {
            System.err.println("WebpageData not initialized. Trying to check if initialization requirements are met...\n");
            if (!data.checkInitialized()) {
                System.err.println("WebpageData still not initialized after check. Please check your data.");
                return;
            }
        }
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("buffered_example.txt", true))) 
        {
            // Separates previous entries with new line
            if (!this.empty) // Is this not the first entry?
            {   
                // Add separator if not first entry
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
    
            // Last modification date and size
            bw.write(data.lastModDate + ", " + data.sizeChars);
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
        } 
        catch (IOException e) {e.printStackTrace();}
        this.empty = false;
    }