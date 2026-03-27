/**
 * This class accepts webpage data and packages them into one object
 *
 * @author Ethan
 * @version 27/3
 */
public class WebpageData
{
    public String pageTitle;
    public String url;
    public String lastModDate;
    public String sizeOfPage;
    public String[] keywords;
    public int[] freq;
    public String[] childLinks;

    // Constructor
    public WebpageData(String pageTitle, String url, String lastModDate, String sizeOfPage,
                       String[] keywords, int[] freq, String[] childLinks) {
        this.pageTitle = pageTitle;
        this.url = url;
        this.lastModDate = lastModDate;
        this.sizeOfPage = sizeOfPage;
        this.keywords = keywords;
        this.freq = freq;
        this.childLinks = childLinks;
    }

    // Utility method to display info
    public void displayInfo() {
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


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class uses the WebpageData class to write content on the txt file.
 *
 * @author Ethan
 * @version 27/3
 */
public class Printer
{
    private String output; // Output txt file name
    private boolean initialized = false;
    private boolean empty = true;
    
    /**
     * Constructor for objects of class Printer
     */
    public Printer(String output_txt_file_name)
    {
        this.output = output_txt_file_name;
        this.initialized = false;
        this.empty = true;
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
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("buffered_example.txt", true))) 
        {
            // Separates previous entries with new line
            if (!this.empty)
            {
                bw.newLine();
                bw.write("========================================================================"); // Separator
                bw.newLine();
            }
            
            // Page title
            bw.write(data.pageTitle);
            bw.newLine();
    
            // URL
            bw.write(data.url);
            bw.newLine();
    
            // Last modification date and size
            bw.write(data.lastModDate + ", " + data.sizeOfPage);
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
}

