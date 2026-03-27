import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class BufferedWriterExample {
    public static void main(String[] args) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("buffered_example.txt"))) {
            bw.write("Line 1");
            bw.newLine();
            bw.write("Line 2");
            bw.newLine();
            bw.write("Line 3");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class AnotherTestExample {
    public static void main(String page_title, String URL, String last_mod_date, String size_of_page, int keyword_count, 
                            String[] keywords, int[] freq, int link_count, String[] child_links) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("debug_for_test_code.txt"))) {
            bw.write(page_title);
            bw.newLine();
            bw.write(URL);
            bw.newLine();
            bw.write(last_mod_date);
            bw.write(", ");
            bw.write(size_of_page);
            bw.newLine();
            for (int i = 0, i < keyword_count; i++)
            {
                bw.write(keywords[i]);
                bw.write(" ");
                bw.write(freq[i]);
                if (i + 1 != keyword_count)
                {
                    bw.write(";");
                }    
            }    
            for (int i = 0; i < link_count; i++)
            {
                bw.write(child_links[i]);
                bw.newLine();
            }        
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
