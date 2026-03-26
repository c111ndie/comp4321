# Description

## Spider
A spider (or called a crawler) functions to fetch pages recursively from a given website:

- Given a starting URL and the number of pages to be indexed, recursively fetch the required
number of pages from the given site into the local system using a breadth-first strategy.
- For each page fetched into the local system, extract all the hyperlinks so that the spider can
recursively process more pages, and proceed to the indexing functions in Step 2.
- Build a file structure containing the parent/child link relation. As noted elsewhere, every
URL is represented internally as a page-ID, so the file structure should be able to return the
page-IDs of the children pages given the page-ID of the parent page and vice versa
- Before a page is fetched into the system, it must perform several checks:
  - If the URL doesn’t exist in the inverted index, go ahead to retrieve the URL
  - If the URL already exists in the index but the last modification date of the URL is later than that recorded in the index, go ahead to retrieve the URL; otherwise, ignore the URL
  - To handle cyclic links gracefully
  - We may run your crawler several times with some web page modifications in between to test your crawler’s robustness.
- Resource: The HTML parser library from http://htmlparser.sourceforge.net/ provides the basic functions to fetch the webpages and to extract the keywords and links from the webpages; notice that the HTML parser is a very large library, but we need no more than a few basic functions from it.
 
# Indexer
An indexer that extracts keywords from a page and inserts them into an inverted file.

- The indexer first removes all stop words from the file; a dictionary of stop words will be provided
- It then transforms words into stems using the Porter’s algorithm
- It inserts the stems into the two inverted files:
  - all stems extracted from the page body, together with all statistical information needed to support the vector space model (i.e., no need to support Boolean operations), are inserted into one inverted file
  - all stems extracted from the page title are inserted into another inverted file
- The indexes must be able to support phrase search, such as “hong kong” in page titles and page bodies.
- Resource: A Java Implementation of Porter’s algorithm is available in lab 3. The JDBM library from http://jdbm.sourceforge.net/ is suggested to be used to create and manipulate the file structures for storing the inverted file and other file structures needed.

# Search Engine

# Web Interface

# Q&A
Slido link: https://app.sli.do/event/udSJsTVFvWCu8YFq1Q9ysi/live/questions
