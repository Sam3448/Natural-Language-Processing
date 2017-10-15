/**
 * The backoff add-lambda language model
 * This is yours to implement!
 * @author YOUREMAIL (YOURNAME)
 */
class BackoffAddLambdaLanguageModel extends LanguageModel {
  final double lambda;

  /** 
   * Constructs an add-lambda language model trained on the given file.
   */
  
  public BackoffAddLambdaLanguageModel(double lambda) throws java.io.IOException {
    if (lambda < 0) {
      System.err.println(
          "You must include a non-negative lambda value in smoother name");
      System.exit(1);
    }
    this.lambda = lambda;
  }
  
  /**
   * Computes the trigram probability p(z | x,y )
   * This is yours to implement!
   */
  public double prob(String x_, String y_, String z_) {
    String x = vocab.contains(x_) ? x_ : Constants.OOV;
    String y = vocab.contains(y_) ? y_ : Constants.OOV;
    String z = vocab.contains(z_) ? z_ : Constants.OOV;

    final String xyz = x + " " + y + " " + z;
    final String xy = x + " "  + y;
    final String yz = y + " "  + z;
    
    final double xyzCount;
    Integer xyzCountInt = tokens.get(xyz);
    if (xyzCountInt == null) {
      xyzCount = 0.0;
    } else {
      xyzCount = (double) xyzCountInt;
    }
    
    final double xyCount;
    Integer xyCountInt = tokens.get(xy);
    if (xyCountInt == null) {
      xyCount = 0.0;
    } else {
      xyCount = (double) xyCountInt;
    }
    //*******************************************************
    final double yzCount;
    Integer yzCountInt = tokens.get(yz);
    if (yzCountInt == null) {
      yzCount = 0.0;
    } else {
      yzCount = (double) yzCountInt;
    }

    final double yCount;
    Integer yCountInt = tokens.get(y);
    if (yCountInt == null){
      yCount = 0.0;
    }
    else{
      yCount = (double) yCountInt;
    }
    final double zCount = tokens.containsKey(z)? (double) tokens.get(z):0.0;
    final double p_z = zCount/vocabSize;

    final double p_z_y = (yzCount + lambda * vocabSize * p_z) / (yCount + lambda * vocabSize);
    
    return (xyzCount + lambda*vocabSize*p_z_y) / (xyCount + lambda * vocabSize);
  }
}
