import com.comp4321.spider.indexer.PageMeta;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;

import java.util.Map;

public class TopKeywordCheck {
    public static void main(String[] args) throws Exception {
        String dbName = args.length > 0 ? args[0] : "spider/crawl-output/indexDB";
        RecordManager recman = RecordManagerFactory.createRecordManager(dbName);
        HTree pageMetadata = HTree.load(recman, recman.getNamedObject("pageMetadata"));

        int pages = 0;
        int badOrder = 0;
        int shown = 0;
        FastIterator it = pageMetadata.keys();
        Object key;
        while ((key = it.next()) != null) {
            if (!(key instanceof Integer) || ((Integer) key) < 0) {
                continue;
            }
            PageMeta meta = (PageMeta) pageMetadata.get(key);
            pages++;
            int previous = Integer.MAX_VALUE;
            int rank = 0;
            for (Map.Entry<String, Integer> entry : meta.topBodyStems.entrySet()) {
                rank++;
                if (entry.getValue() > previous) {
                    badOrder++;
                    break;
                }
                previous = entry.getValue();
            }
            if (shown < 5) {
                System.out.println("page " + key + " " + meta.title);
                rank = 0;
                for (Map.Entry<String, Integer> entry : meta.topBodyStems.entrySet()) {
                    if (rank >= 5) {
                        break;
                    }
                    System.out.println("  " + entry.getKey() + "=" + entry.getValue());
                    rank++;
                }
                shown++;
            }
        }

        System.out.println("PAGES_CHECKED=" + pages);
        System.out.println("BAD_TOP_KEYWORD_ORDER=" + badOrder);
        recman.close();
    }
}
