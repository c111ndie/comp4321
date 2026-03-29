# comp4321

Tasks for phase 1

You should submit:
- a document containing the design of the jdbm database scheme of the indexer. All supporting
databases should be defined, for example, forward and inverted indexes, mapping tables for
URL <=> page ID and word <=> word ID conversion. The jdbm database schema depends on
the functions implemented. You should include an explanation on your design. (Sean)
- the source codes of the spider(Cindie) and the test program (Ethan)
- a readme.txt file containing the instructions to build the spider and the test program, and
how to execute them. (Cindie)
- the db file(s) which contain the indexed 30 pages starting from
https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm or
https://comp4321-hkust.github.io/testpages/testpage.htm (backup website).
New Target URL: https://hitcslj.github.io/TestPages/testpage.html (Ethan)
- spider result.txt which is the output of test program (Ethan)

Zip the files and submit via Canvas. The assignment name is Phase1

Tasks assignment:
- spider: cindie
- indexer: sean
- test program + data retrieval: ethan

Instructions:

```bash
cd spider
./mvnw -q clean package
java -jar target/spider-1.0.0.jar \
  --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm \
  --max-pages 30 \
  --out out
```

Outputs are written to `spider/out/`: raw HTML in `pages/page<pageId>.html` and crawl state in `state.json`.
