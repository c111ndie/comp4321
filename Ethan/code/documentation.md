# Text generator.java
It is just a program with two classes that allows writing the results into the required txt file.

I placed it in util file of the spider.

## WebpageData

This class packages the information required into a single object. This should permit 
### Variables
- boolean initialized: A tag that checks if WebpageData is initialized to facilitate sanity check
- String pageTitle
- String url
- String lastModData
- String sizeOfPage
- String[] keywords
- Object[] freq: This allows it to accept both int and String as input
- String[] childLinks

### Methods
- WebpageData(): Dummy Constructor
- WebpageData(String pageTitle, String url, String lastModDate, String sizeOfPage, String[] keywords, int[] freq, String[] childLinks): 
Constructor that receives all required data
- WebpageData(PageRecord page): Constructor that loads page record data.
- void loadWebpageData(String pageTitle, String url, String lastModDate, String sizeOfPage, String[] keywords, int[] freq, String[] childLinks): 
Loads required data
- boolean checkInitialized(): Checks if necessary fields have been filled
- void displayInfo(): Debugging method that prints content out

### Usage
Create a WebpageData instance, and  

## Printer

This class is assigned to a txt path, receives a content and write them to the file.

### Variables
- private String output: The output text file path
- initialized = false:  A tag that checks if WebpageData is initialized to facilitate sanity check
- private boolean empty: A tag that is true if the Printer has checked the target is empty

### Methods
- Printer(String output_txt_file_name): Constructor. FIle path mandatory.
- void initTxtFile(): Clears the text file.
- void appendWebpageData(WebpageData data): Writes content to the end of text file. Automatically adds separator if file to be written is not empty.
- void appendWebpageData(PageRecord data): Writes content to the end of text file. Automatically adds separator if file to be written is not empty.