import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;

public class IndexDbInspector {
    public static void main(String[] args) throws Exception {
        String dbName = args.length > 0 ? args[0] : "../spider/indexDB";
        RecordManager recman = RecordManagerFactory.createRecordManager(dbName);

        String[] names = {
            "urlToPageId", "pageIdToUrl", "wordToWordId", "wordIdToWord",
            "bodyInvertedIndex", "titleInvertedIndex",
            "forwardIndex", "pageMetadata", "counters"
        };

        for (String name : names) {
            long recid = recman.getNamedObject(name);
            System.out.println(name + " => recid=" + recid);
            if (recid != 0) {
                HTree tree = HTree.load(recman, recid);
                int shown = 0;
                FastIterator iter = tree.keys();
                Object k;
                while ((k = iter.next()) != null) {
                    System.out.println("  " + k + " => " + tree.get(k));
                    shown++;
                    if (shown >= 20) {
                        System.out.println("  ... (20 entries max)");
                        break;
                    }
                }
                if (shown == 0) {
                    System.out.println("  (empty)");
                }
            }
        }

        recman.close();
    }
}
