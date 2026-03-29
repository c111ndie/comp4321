package com.comp4321.spider.util;

import com.comp4321.spider.store.PageRecord;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class accepts webpage data and packages them into one object
 *
 * @author Ethan
 * @version 27/3
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

