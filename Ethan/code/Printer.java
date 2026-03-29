import java.io.*;

/**
 * This class uses the WebpageData class to write content on the txt file.
 *
 * @author Ethan
 * @version 27/3
 */
public class Printer
{
    private String output; // Output txt file name
    public boolean initialized;
    private boolean empty;
    private BufferedWriter sessionWriter; // For session-based buffering
    
    /**
     * Constructor for objects of class Printer
     */
    public Printer(String output_txt_file_name)
    {
        this.output = output_txt_file_name;
        this.initialized = true;
        this.empty = false;
        this.sessionWriter = null;
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
     * Start a session for batch writing to improve I/O efficiency.
     */
    public void startSession() throws IOException {
        if (sessionWriter == null) {
            sessionWriter = new BufferedWriter(new FileWriter(this.output, true));
        }
    }
    
    /**
     * End the session and close the writer.
     */
    public void endSession() throws IOException {
        if (sessionWriter != null) {
            sessionWriter.close();
            sessionWriter = null;
        }
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
        
        try {
            BufferedWriter bw = (sessionWriter != null) ? sessionWriter : new BufferedWriter(new FileWriter(this.output, true));
            
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
            
            if (sessionWriter == null) {
                bw.close();
            }
        } 
        catch (IOException e) {e.printStackTrace();}
        this.empty = false;
    }

}

