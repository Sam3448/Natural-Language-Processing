import java.io.*;
import java.security.MessageDigest;

/**
 * Driver class for HW2 on N-grams
 */
public class TextCat {

    private static String getModelFilename(String smoother, String lexicon, String train_file) {
        File l_file = new File(lexicon);
        File t_file = new File(train_file);
        String filename = smoother + "_" + l_file.getName() + "_" + t_file.getName() + ".model";
        return filename;
    }

    /**
     * Calculates the probability of a file
     *
     * @param testfile the location of the
     * @param lm       a trained language model
     * @return log probability of a file (i.e. a sequence of words in a file)
     * @throws IOException on error reading file
     */
    public static double fileLogProb(String testfile, LanguageModel lm)
            throws IOException {
        BufferedReader reader =
                new BufferedReader(new FileReader(new File(testfile)));

        double logprob = 0.0;

        String x = Constants.BOS;
        String y = Constants.BOS;
        
        String line;
        int i = 0;
        while ((line = reader.readLine()) != null && line.length() != 0) {
            for (String z : line.trim().split("\\s+")) {
                if (!lm.vocab.contains(z)) z = Constants.OOV; 
                logprob += Math.log(lm.prob(x, y, z));
                x = y;
                y = z;
            }
        }
        logprob += Math.log(lm.prob(x, y, Constants.EOS));
        reader.close();

        return logprob;
    }

    public static void main(String[] args) {
        final String mode = args[0];
        final String smoother = args[1];
        final String lexicon = args[2];
        final String trainFile1 = args[3];
        final String trainFile2 = args[4];

        if (args.length < 4) {
            System.err.println("warning: no input files specified");
        }

        LanguageModel lm = null;
        if (mode.equals("TRAIN")) {
          try {
              lm = LanguageModel.getLM(smoother, lexicon);
              lm.setVocabSize(trainFile1, trainFile2);
              lm.train(trainFile1);
              LanguageModel.save(getModelFilename(smoother, lexicon, trainFile1), lm);
              lm.train(trainFile2);
              LanguageModel.save(getModelFilename(smoother, lexicon, trainFile2), lm);              
          } catch (IOException e) {
              System.err.format("error: error reading %s or %s\n", trainFile1, trainFile2);
              e.printStackTrace(System.err);
              System.exit(1);
          }
        } else if (mode.equals("TEST")) {
          LanguageModel lm1 = LanguageModel.load(getModelFilename(smoother, lexicon, trainFile1));
          LanguageModel lm2 = LanguageModel.load(getModelFilename(smoother, lexicon, trainFile2));
          double prior = Double.parseDouble(args[5]);
          double numTokens = 0;
          double[] prob_Gen = new double[args.length - 6];
          double[] prob_Spam = new double[args.length - 6];
          int[] count = new int[2];
          int errCount = 0;
          String goodTrain =trainFile1.substring(trainFile1.lastIndexOf("/")+1);
          String badTrain =trainFile2.substring(trainFile2.lastIndexOf("/")+1);
          String curTaskGoodName = trainFile1;//goodTrain.substring(0, goodTrain.indexOf("."));
          String curTaskBadName = trainFile2;//badTrain.substring(0, badTrain.indexOf("."));

          for (int i = 6; i < args.length; i++) {
              final String testfile = args[i];
              try {
                  double ce = fileLogProb(testfile, lm1) / Constants.LOG2 + Math.log(prior)/Constants.LOG2; // ???
                  prob_Gen[i - 6] = ce;

                  ce = fileLogProb(testfile, lm2) / Constants.LOG2 + Math.log(1 - prior)/Constants.LOG2;
                  prob_Spam[i - 6] = ce;

                  //String curFile = testfile.substring(testfile.lastIndexOf("/")+1);

                  System.out.println((prob_Gen[i-6]>prob_Spam[i-6]?curTaskGoodName:curTaskBadName) + '\t' + testfile);
                  if(prob_Gen[i-6]>prob_Spam[i-6]){
                    if(testfile.contains(curTaskBadName)) errCount++;
                    count[0]++;
                  }
                  else{
                    if(testfile.contains(curTaskGoodName)) errCount++;
                    count[1]++;
                  }
              } catch (IOException e) {
                  System.err.format("warning: error reading %s\n", testfile);
                  e.printStackTrace(System.err);
              }
          }
          System.out.println(count[0] + " files were more probably "+ curTaskGoodName +" (" + (count[0]+0.0)*100/(count[0]+count[1]) + "%)");
          System.out.println(count[1] + " files were more probably "+ curTaskBadName +" (" + (count[1]+0.0)*100/(count[0]+count[1]) + "%)");
          //System.out.println("error classification : " + errCount + " out of : " + (count[0]+count[1]));
          //System.out.println("Error Rate : " + ((errCount+0.0)*100/(count[0]+count[1])) + "%");
          //System.out.format("Overall cross-entropy:\t%g\n", totalCrossEntropy / numTokens);
        } else System.exit(1);
    }
}