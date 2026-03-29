import IRUtilities.*;
import java.io.*;
import java.util.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File.*;
import java.io.FileInputStream.*;
import java.io.FileNotFoundException.*;
import java.io.IOException;
import java.util.HashSet;

public class StopStem
{
	private Porter porter;
	private HashSet<String> stopWords;
	public boolean isStopWord(String word)
	{
		return stopWords.contains(word.toLowerCase());
	}

	public StopStem(String str)
	{
		super();
		porter = new Porter();
		stopWords = new HashSet<String>();

		// use BufferedReader to extract the stopwords in stopwords.txt (path passed as parameter str)
		// add them to HashSet<String> stopWords
		try (BufferedReader br = new BufferedReader(new FileReader(str))){
				String word;
				while ((word = br.readLine()) != null){
					word = word.trim().toLowerCase();
					if (!word.isEmpty()){
						stopWords.add(word);
					}
				}
		} catch (IOException e) {
			System.err.println("Error loading stopwords from file: " + str);
			e.printStackTrace();
		}
	}

	public String stem(String str)
	{
		if (str == null || str.isEmpty()) return"";
		return porter.stripAffixes(str.toLowerCase());
	}

	public static void main(String[] arg)
	{
		StopStem stopStem = new StopStem("stopwords.txt");
		String input="";
		try{
			do
			{
				System.out.print("Please enter a single English word: ");
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				input = in.readLine().trim();
				if(!input.isEmpty())
				{	
					if (stopStem.isStopWord(input))
						System.out.println("It should be stopped");
					else
			   			System.out.println("The stem of it is \"" + stopStem.stem(input)+"\"");
				}
			}
			while(!input.isEmpty());
		}
		catch(IOException ioe)
		{
			System.err.println(ioe.toString());
		}
	}
}
