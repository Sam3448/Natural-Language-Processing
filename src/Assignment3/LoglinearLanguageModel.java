/**
 * The Loglinear language model
 * This is yours to implement!
 */

import java.io.*;
import java.util.*;

class LoglinearLanguageModel extends LanguageModel {

    /**
     * The word vector for w can be found at vectors.get(w).
     * You can check if a word is contained in the lexicon using
     * if w in vectors:
     */
    Map<String, double[]> vectors;  // loaded using readVectors()

    /**
     * The dimension of word vector
     */
    int dim;

    /**
     * The constant that determines the strength of the regularizer.
     * Should ordinarily be >= 0.
     */
    double C;

    /**
     * the two weight matrices X and Y used in log linear model
     * They are initialized in train() function and represented as two
     * dimensional arrays.
     */
    double[][] X, Y;

    /**
     * Construct a log-linear model that is TRAINED on a particular corpus.
     *
     * @param C       The constant that determines the strength of the regularizer.
     *                Should ordinarily be >= 0.
     * @param lexicon The filename of the lexicon
     */
    public LoglinearLanguageModel(double C, String lexicon) throws java.io.IOException {
        if (C < 0) {
            System.err.println(
                    "You must include a non-negative lambda value in smoother name");
            System.exit(1);
        }
        this.C = C;
        readVectors(lexicon);
    }

    /**
     * Read word vectors from an external file.  The vectors are saved as
     * arrays in a dictionary self.vectors.
     *
     * @param filename The parameter vector: a map from feature names (strings)
     *                 to their weights.
     */
    private void readVectors(String filename) throws IOException {
        vectors = new HashMap<>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(filename)));
        String header = bufferedReader.readLine();
        String[] cfg = header.split("\\s+");
        dim = Integer.parseInt(cfg[cfg.length - 1]);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] arr = line.split("\\s+");
            assert arr.length == dim + 1;
            String word = arr[0];
            double[] vec = new double[dim];
            for (int i = 0; i < vec.length; ++i)
                vec[i] = Double.parseDouble(arr[i + 1]);
            vectors.put(word, vec);
        }

    }

    private void init() { // init model parameter
        this.X = new double[dim][dim];
        this.Y = new double[dim][dim];//???
        Random r = new Random(1l);
        for (int i = 0; i < dim; ++i) {
            Arrays.fill(X[i], 0.);
            Arrays.fill(Y[i], 0.);
            // for(int j = 0; j < dim; j++){
            //     X[i][j] = r.nextDouble();
            //     Y[i][j] = r.nextDouble();
            // }
        }
    }

    /**
     * You probably want to call the parent method train(trainFile)
     * to collect n-gram counts, then optimize some objective function
     * that considers the n-gram counts, and finally call setTheta() on
     * the result of optimization.  See INSTRUCTIONS for more hints.
     */
    public void train(String trainFile) throws IOException {
        super.train(trainFile);
        if (X == null) init();
        double gamma0 = 0.01;  // initial learning rate, used to compute actual learning rate
        int epochs = 20;  // number of passes
        int N = tokenList.size() - 2;

        /**
         * Train the log-linear model using SGD.
         * ******** COMMENT *********
         * In log-linear model, you will have to do some additional computation at
         * this point.  You can enumerate over all training trigrams as following.
         * 
         * for (int i = 2; i < tokenList.size(); ++i) {
         *   String x = tokenList.get(i - 2);
         *   String y = tokenList.get(i - 1);
         *   String z = tokenList.get(i);
         * }
         *
         * Note2: You can use showProgress() to log progress.
         *
         **/

        System.err.println("Start optimizing!!!");
        int t = 0;
        double gamma = gamma0;
        for(int i = 0; i < epochs; i++){
            double F = 0.0;
            for(int j = 0; j < N; j++){
                String x = tokenList.get(j);
                String y = tokenList.get(j+1);
                String z = tokenList.get(j+2);
                gamma = gamma0 / (1 + gamma0 * (C / N) * t);
                update(N, x, y, z, X, Y, gamma);
                t++;
                showProgress();
                double curRes = Math.log(prob(x, y, z));
                F += curRes;
                //System.out.println(j + "th word F = " + F +", curres = " + curRes);
            }
            System.out.println("F = " + F);
            //System.out.println(Arrays.deepToString(X));
        }

        System.err.format("\nFinished training on %d tokens\n", tokens.get(""));
    }


    public void update(int N, String x, String y, String z, double[][] X, double[][] Y, double gamma){
        List<String> poss_z = allPossiblez(x, y);
        double[] vec_x = vectors.containsKey(x)? vectors.get(x) : vectors.get("OOL");
        double[] vec_y = vectors.containsKey(y)? vectors.get(y) : vectors.get("OOL");
        double[] vec_z = vectors.containsKey(z)? vectors.get(z) : vectors.get("OOL");
        double denominator = 0.0;
        List<Double> expo_possible_z = new ArrayList<>();
        int count = 0;
        for(String possible_z : poss_z){
            count ++;
            double[] vec_possiblez = vectors.containsKey(possible_z)? vectors.get(possible_z) : vectors.get("OOL");
            double temp = expo(vec_x, vec_y, vec_possiblez, x, y, possible_z);
            if(temp > 1e100) temp = denominator / count;
            //System.out.println("\n"+temp);
            expo_possible_z.add(temp);
            denominator += temp;
        }
        if(denominator == 0) return;
        if(denominator >= 1e200) denominator = 1e200;
        for(int i = 0; i < dim; i++){
            for(int j = 0; j < dim; j++){
                double formerX = vec_x[i] * vec_z[j];
                double formerY = vec_y[i] * vec_z[j];
                double laterX = 0.0, laterY = 0.0;
                for(int k = 0; k < poss_z.size(); k++){
                    String possible_z = poss_z.get(k);
                    double[] vec_possiblez = vectors.containsKey(possible_z)? vectors.get(possible_z) : vectors.get("OOL");
                    laterX += expo_possible_z.get(k) * (vec_x[i] * vec_possiblez[j]);
                    laterY += expo_possible_z.get(k) * (vec_y[i] * vec_possiblez[j]);
                }
                laterX /= denominator;
                laterY /= denominator;
                double delta_fi_xij = formerX - laterX - ((2 * C) / N) * X[i][j];
                double delta_fi_yij = formerY - laterY - ((2 * C) / N) * Y[i][j];
                X[i][j] += gamma * delta_fi_xij;
                Y[i][j] += gamma * delta_fi_yij;
            }
        }
    }

    /**
     * Computes the trigram probability p(z | x,y )
     */
    public double prob(String x, String y, String z) {
        double[] vec_x = vectors.containsKey(x)? vectors.get(x) : vectors.get("OOL");
        double[] vec_y = vectors.containsKey(y)? vectors.get(y) : vectors.get("OOL");
        double[] vec_z = vectors.containsKey(z)? vectors.get(z) : vectors.get("OOL");
        double numerator = expo(vec_x, vec_y, vec_z, x, y, z);
        double denominator = 0.0;
        List<String> poss_z = allPossiblez(x, y);
        int count = 0;
        double temp;
        for(String possible_z : poss_z){
            double[] vec_possiblez = vectors.containsKey(possible_z)? vectors.get(possible_z) : vectors.get("OOL");
            temp = expo(vec_x, vec_y, vec_possiblez, x, y, possible_z);
            if(temp > 1e100){

                temp = denominator / count;
            }
            count ++;
            denominator += temp;
            //System.out.print("possible_z = "+possible_z +"  "+expo(vec_x, vec_y, vec_possiblez));
        }
        if(denominator == 0) return 1;
        if(numerator >= 1e100){
            return 1.0/count;
        }
        //System.out.println("  numerator = "+ numerator+" denominator = " + denominator+ x+" "+y+" "+z);
        return numerator / denominator;
    }
    public List<String> allPossiblez(String x, String y){
        String bi = x + " " + y + " ";
        List<String> poss_z = new ArrayList<>();
        for(String tri : tokens.keySet()){
            if(tri.startsWith(bi)){
                String possible_z = tri.substring(bi.length());
                poss_z.add(possible_z);
            }
        }
        return poss_z;
    }
    public double expo(double[] x, double[] y, double[] z, String x_, String y_, String z_){
        double[] temp1 = new double[dim];
        double[] temp2 = new double[dim];
        double res1 = 0, res2 = 0;
        for(int j = 0; j < dim; j++){
            double sum = 0;
            for(int i = 0; i < dim; i ++){
                sum += x[i] * X[i][j];
            }
            temp1[j] = sum;
        }
        for(int i = 0; i < dim; i++){
            res1 += temp1[i] * z[i];
        }

        for(int j = 0; j < dim; j++){
            double sum = 0;
            for(int i = 0; i < dim; i ++){
                sum += y[i] * Y[i][j];
            }
            temp2[j] = sum;
        }
        for(int i = 0; i < dim; i++){
            res2 += temp2[i] * z[i];
        }

        //System.out.print("******"+ res1 +"  "+ res2 +"******");
        return Math.exp(res1 + res2 + (tokens.containsKey(z_)?1:0) + (tokens.containsKey(y_ + " " + z_)?1:0) + (tokens.containsKey(x_ + " " +y_ + " " + z_)?1:0));
    }

    // Feel free to add other functions as you need.
}
