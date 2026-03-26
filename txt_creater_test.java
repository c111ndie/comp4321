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
