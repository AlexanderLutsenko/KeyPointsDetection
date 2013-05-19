package detection.forest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Houses a Breiman Random Forest
 *
 * @see <a
 * href="http://www.stat.berkeley.edu/~breiman/RandomForests/cc_home.htm">Breiman's
 * Random Forests (UC Berkeley)</a>
 * @author kapelner
 *
 */
public class RandomForest {

    /**
     * the number of threads to use when generating the forest
     */
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    /**
     * the number of categorical responses of the data (the classes, the "Y"
     * values) - set this before beginning the forest creation
     */
    public static int C = 101;
    /**
     * the number of attributes in the data - set this before beginning the
     * forest creation
     */
    public static int M = 720;
    /**
     * Of the M total attributes, the random forest computation requires a
     * subset of them to be used and picked via random selection. "Ms" is the
     * number of attributes in this subset. The formula used to generate Ms was
     * recommended on Breiman's website.
     */
    public static int Ms = (int)Math.round(Math.log(M)/Math.log(2)+1); //recommende by Breiman: =(int)Math.round(Math.log(M)/Math.log(2)+1);
    /**
     * the collection of the forest's decision trees
     */
    private ArrayList<DTree> trees;
    /**
     * the starting time when timing random forest creation
     */
    private long time_o;
    /**
     * the number of trees in this random tree
     */
    private int numTrees;
    /**
     * For progress bar display for the creation of this random forest, this is
     * the amount to update by when one tree is completed
     */
    private double update;
    /**
     * For progress bar display for the creation of this random forest, this
     * records the total progress
     */
    private double progress;
    /**
     * this is an array whose indices represent the forest-wide importance for
     * that given attribute
     */
    private int[] importances;
    /**
     * This maps from a data record to an array that records the classifications
     * by the trees where it was a "left out" record (the indices are the class
     * and the values are the counts)
     */
    private HashMap<int[], int[]> estimateOOB;
    /**
     * the total forest-wide error
     */
    private double error;
    /**
     * the thread pool that controls the generation of the decision trees
     */
    private ExecutorService treePool;
    /**
     * the original training data matrix that will be used to generate the
     * random forest classifier
     */
    private ArrayList<int[]> data;

    /**
     * Initializes a Breiman random forest creation
     *
     * @param numTrees	the number of trees in the forest
     * @param data	the training data used to generate the forest
     * @param buildProgress	records the progress of the random forest creation
     */
    public RandomForest(int numTrees, ArrayList<int[]> data) {
        this.numTrees = numTrees;
        this.data = data;
        trees = new ArrayList<DTree>(numTrees);
        update = 100 / ((double) numTrees);
        progress = 0;
        StartTimer();
        System.out.print("creating " + numTrees + " trees in a random Forest. . . ");
//		ArrayList<Datum> master=AssignClassesAndGetAllData(data);

        estimateOOB = new HashMap<int[], int[]>(data.size());
    }

    /**
     * Begins the random forest creation
     */
    public void Start() {
        treePool = Executors.newFixedThreadPool(NUM_THREADS);
        for (int t = 0; t < numTrees; t++) {
            treePool.execute(new CreateTree(data, this));
        }
        treePool.shutdown();
        try {
            treePool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS); //effectively infinity
        } catch (InterruptedException ignored) {
            System.out.println("interrupted exception in Random Forests");
        }
//	    buildProgress.setValue(100); //just to make sure

        CalcErrorRate();
        CalcImportances();
        System.out.print("done in " + TimeElapsed(time_o));
    }

    /**
     * This calculates the forest-wide error rate. For each "left out" data
     * record, if the class with the maximum count is equal to its actual class,
     * then increment the number of correct. One minus the number correct over
     * the total number is the error rate.
     */
    private void CalcErrorRate() {
        double N = 0;
        int correct = 0;
        for (int[] record : estimateOOB.keySet()) {
            N++;
            int[] map = estimateOOB.get(record);
            int Class = FindMaxIndex(map);
            if (Class == DTree.GetClass(record)) {
                correct++;
            }
        }
        error = 1 - correct / N;
        System.out.println("Forest error rate:" + error);
    }

    /**
     * Update the error map by recording a class prediction for a given data
     * record
     *
     * @param record	the data record classified
     * @param Class	the class
     */
    public void UpdateOOBEstimate(int[] record, int Class) {
        if (estimateOOB.get(record) == null) {
            int[] map = new int[C];
            map[Class]++;
            estimateOOB.put(record, map);
        } else {
            int[] map = estimateOOB.get(record);
            map[Class]++;
        }
    }

    /**
     * This calculates the forest-wide importance levels for all attributes.
     *
     */
    private void CalcImportances() {
        importances = new int[M];
        for (DTree tree : trees) {
            for (int i = 0; i < M; i++) {
                importances[i] += tree.getImportanceLevel(i);
            }
        }
        for (int i = 0; i < M; i++) {
            importances[i] /= numTrees;
        }

//		Datum.PrintImportanceLevels(importances);
    }

    /**
     * Start the timer when beginning forest creation
     */
    private void StartTimer() {
        time_o = System.currentTimeMillis();
    }

    /**
     * This class houses the machinery to generate one decision tree in a thread
     * pool environment.
     *
     * @author kapelner
     *
     */
    private class CreateTree implements Runnable {

        /**
         * the training data to generate the decision tree (same for all trees)
         */
        private ArrayList<int[]> data;
        /**
         * the current forest
         */
        private RandomForest forest;

        /**
         * A default, dummy constructor
         */
        public CreateTree(ArrayList<int[]> data, RandomForest forest) {
            this.data = data;
            this.forest = forest;
        }

        /**
         * Creates the decision tree
         */
        public void run() {
            trees.add(new DTree(data, forest));
//			System.out.println("tree added in RandomForest.AddTree.run()");
            progress += update;
        }
    }

    /**
     * Evaluates an incoming data record. It first allows all the decision trees
     * to classify the record, then it returns the majority vote
     *
     * @param record	the data record to be classified
     */
    public int Evaluate(int[] record) {
        int[] counts = new int[C];
        for (int t = 0; t < numTrees; t++) {
            int Class = (trees.get(t)).Evaluate(record);
            counts[Class]++;
        }
        return FindMaxIndex(counts);
    }

    /**
     * Given an array, return the index that houses the maximum value
     *
     * @param arr	the array to be investigated
     * @return	the index of the greatest value in the array
     */
    public static int FindMaxIndex(int[] arr) {
        int index = 0;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
                index = i;
            }
        }
        return index;
    }

    /**
     * Attempt to abort random forest creation
     */
    public void Stop() {
        treePool.shutdownNow();
    }

    /**
     * Given a certain time that's elapsed, return a string representation of
     * that time in hr,min,s
     *
     * @param timeinms	the beginning time in milliseconds
     * @return	the hr,min,s formatted string representation of the time
     */
    private static String TimeElapsed(long timeinms) {
        int s = (int) (System.currentTimeMillis() - timeinms) / 1000;
        int h = (int) Math.floor(s / ((double) 3600));
        s -= (h * 3600);
        int m = (int) Math.floor(s / ((double) 60));
        s -= (m * 60);
        return "" + h + "hr " + m + "m " + s + "s";
    }
}