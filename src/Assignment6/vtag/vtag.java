import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class vtag {
	private HashMap<String, Integer> tagtag_count = new HashMap<>();
	private HashMap<String, Integer> wordtag_count = new HashMap<>();
	private HashMap<String, Integer> tag_count = new HashMap<>();
	private HashMap<String, Integer> word_count = new HashMap<>();
	private HashMap<String, Integer> tagtag_singleton = new HashMap<>();
	private HashMap<String, Integer> wordtag_singleton = new HashMap<>();
	private HashMap<String, List<String>> tag_dict = new HashMap<>();
	private List<String> tags = new ArrayList<>();
	int n;
	
	public static void main(String[] args){
		try {
			vtag t = new vtag();
			t.train(args[0]);
			t.test(args[1]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void train(String filename) throws IOException{
		File f = new File(filename);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String previous_tag = null;
		String line;
		while((line=br.readLine())!=null){
			wordtag_count.put(line, wordtag_count.getOrDefault(line, 0) + 1);
			String[] s = line.split("/");
			word_count.put(s[0], word_count.getOrDefault(s[0], 0) + 1);
			tag_count.put(s[1], tag_count.getOrDefault(s[1], 0) + 1);
			
			if(wordtag_count.get(line)==1){
				List<String> l = tag_dict.getOrDefault(s[0], new ArrayList<String>());
				l.add(s[1]);
				tag_dict.put(s[0], l);
				wordtag_singleton.put(s[1], wordtag_singleton.getOrDefault(s[1], 0) + 1);
			}else if(wordtag_count.get(line)==2)
				wordtag_singleton.put(s[1], wordtag_singleton.get(s[1]) - 1);
			
			if(previous_tag != null){
				String tmp = s[1] + "/" + previous_tag;
				tagtag_count.put(tmp, tagtag_count.getOrDefault(tmp, 0) + 1);
				if(tagtag_count.get(tmp)==1)
					tagtag_singleton.put(previous_tag, tagtag_singleton.getOrDefault(previous_tag, 0) + 1);
				else if(tagtag_count.get(tmp)==2)
					tagtag_singleton.put(previous_tag, tagtag_singleton.get(previous_tag) - 1);
			}
			
			if(!s[1].equals("###") && !tags.contains(s[1]))
				tags.add(s[1]);
			
			previous_tag = s[1];
		}
		br.close();
	}
	
	public void test(String filename) throws IOException{
		File f = new File(filename);
		BufferedReader br = new BufferedReader(new FileReader(f));
		List<String> data = new ArrayList<>();
		String line;
		while((line=br.readLine())!=null)
			data.add(line);
		br.close();
		n = data.size() - 1;
		
		String[] w = new String[n+1];
		w[0] = "###";
		String[] actual_tag = new String[n+1];
		actual_tag[0] = "###";
		HashMap<String, Double> mu = new HashMap<>();
		mu.put("###/0", 0.0);
		HashMap<String, Double> log_prob = new HashMap<>();
		HashMap<String, String> backpointer = new HashMap<>();
		
		for(int i=1; i<data.size(); i++){
			String[] s = data.get(i).split("/");
			w[i] = s[0];
			actual_tag[i] = s[1];
			for(String cur : tag_dict.getOrDefault(w[i], tags)){
				for(String pre : tag_dict.getOrDefault(w[i-1], tags)){
					double p = p_tt(cur, pre) + p_wt(w[i], cur);
					double tmp = mu.getOrDefault(pre + "/" + (i-1), Double.NEGATIVE_INFINITY) + p;
					String state = cur + "/" + i;
					if(tmp > mu.getOrDefault(state, Double.NEGATIVE_INFINITY)){
						mu.put(state, tmp);
						log_prob.put(state, p);
						backpointer.put(state, pre);
					}
				}
			}
		}

		String[] t = new String[n+1];
		t[n] = "###";
		double total = 0.0;
		for(int i=n; i>0; i--){
			String st = t[i] + "/" + i;
			t[i-1] = backpointer.get(st);
			total += log_prob.get(st);
		}
		
		double novel_correct = 0.0;
		double novel = 0.0;
	    double known_correct = 0.0;
	    double known = 0.0;
	    for(int i=0; i<n+1; i++){
	    	if(w[i].equals("###")) continue;
    		if(word_count.containsKey(w[i])){
    			if(actual_tag[i].equals(t[i])) known_correct += 1;
    			known += 1;
    		}else{
    			if(actual_tag[i].equals(t[i])) novel_correct += 1;
    			novel += 1;
    		}
	    }
	    
	    System.out.println(String.format("Model perplexity per tagged test word: %.3f", Math.exp(- total / n)));
	    System.out.println(String.format("Tagging accuracy (Viterbi decoding): %.2f%% (known: %.2f%% novel: %.2f%%)", 
	    		100 * (novel_correct + known_correct) / (novel + known), 100 * known_correct / known, 
	    		novel==0 ? 0 : 100 * (novel_correct / novel)));
	    
	    forward_backward(filename);
	}
	
	public void forward_backward(String fileName) throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fileName))));
		List<String> data = new ArrayList<>();
		String line = "";
		while((line = br.readLine()) != null) data.add(line);
		n = data.size() - 1;
		br.close();
		
		HashMap<String, Double> a = new HashMap<>();
		a.put("###/0", Math.log(1.0));
		HashMap<String, Double> b = new HashMap<>();
		String[] w = new String[data.size()];
		w[0] = "###";
		String[] actual_tag = new String[data.size()];
		Pair[] log_prob = pairBuilder(data.size());
		
		//forward
		for(int i = 1; i < data.size(); i++){
			String[] temp = data.get(i).split("/");
			w[i] = temp[0];
			actual_tag[i] = temp[1];
			for(String tc : tag_dict.getOrDefault(w[i], tags)){
				for(String tp : tag_dict.getOrDefault(w[i - 1], tags)){
					double p = p_tt(tc, tp) + p_wt(w[i], tc);
					String key = join(tc, String.valueOf(i));
					a.put(key, logadd(a.getOrDefault(join(tc, String.valueOf(i)), Double.NEGATIVE_INFINITY), 
									a.getOrDefault(join(tp, String.valueOf(i - 1)), Double.NEGATIVE_INFINITY) + p));
				}
			}
		}
		double S = a.get(join("###", String.valueOf(n)));
		b.put(join("###", String.valueOf(n)), Math.log(1.0));
		
		//backward
		for(int j = 1; j < data.size(); j++){
			int i = data.size() - j;
			for(String tc : tag_dict.getOrDefault(w[i], tags)){
				if(log_prob[i].getValue() < a.getOrDefault(join(tc, String.valueOf(i)), Double.NEGATIVE_INFINITY)
										+ b.getOrDefault(join(tc, String.valueOf(i)), Double.NEGATIVE_INFINITY) - S){
					log_prob[i].setKey(tc);
					log_prob[i].setValue(a.getOrDefault(join(tc, String.valueOf(i)), Double.NEGATIVE_INFINITY)
										+ b.getOrDefault(join(tc, String.valueOf(i)), Double.NEGATIVE_INFINITY) - S);
				}
				for(String tp : tag_dict.getOrDefault(w[i - 1], tags)){
					double p = p_tt(tc, tp) + p_wt(w[i], tc);
					b.put(join(tp, String.valueOf(i - 1)), logadd(b.getOrDefault(join(tp, String.valueOf(i - 1)), Double.NEGATIVE_INFINITY),
															b.getOrDefault(join(tc, String.valueOf(i)), Double.NEGATIVE_INFINITY) + p));
				}
			}
		}
		
	    double known_correct = 0.0;
	    double known = 0.0;
	    double novel_correct = 0.0;
	    double novel = 0.0;
	    StringBuilder sb = new StringBuilder();
	    for(int i = 0; i < data.size(); i++){
	    	if(log_prob[i].getKey().equals("###")){
	    		sb.append("###/###\n");
	    		continue;
	    	}
	    	sb.append(w[i] + "/" + log_prob[i].getKey() + "\n");
	    	if(actual_tag[i].equals(log_prob[i].getKey())){
	    		if(!word_count.containsKey(w[i])) novel_correct++;
	    		else known_correct++;
	    	}
	    	if(!word_count.containsKey(w[i])) novel++;
	    	else known++;
	    }
	   
	    FileWriter fw = new FileWriter(new File("test-output"));
	    fw.write(sb.toString());
	    fw.close();
	    
	    double total = 100 * (novel_correct + known_correct) / (novel + known);
	    System.out.println(String.format("Tagging accuracy (posterior decoding): %.2f%% (known: %.2f%% novel: %.2f%%)", 
	    		total, 100 * known_correct / known, novel == 0 ? 0 : (100 * novel_correct / novel)));
	}
	
	public double p_tt(String t1, String t2){
		String key = join(t1, t2);
		double numerator = tagtag_count.getOrDefault(key, 0) + (1 + tagtag_singleton.getOrDefault(t2, 0)) * p_tt_backoff(t1, t2);
		double denominator = tag_count.getOrDefault(t2, 0) + 1 + tagtag_singleton.getOrDefault(t2, 0);
		return Math.log(numerator / denominator);
	}
	
	public double p_tt_backoff(String t1, String t2){
		return (double)tag_count.getOrDefault(t1, 0)/ n;
	}
	
	public double p_wt(String w, String t){
		if(w.equals("###") && t.equals("###")) return 0;
		String key = join(w, t);
		double numerator = wordtag_count.getOrDefault(key, 0) + (1 + wordtag_singleton.getOrDefault(t, 0)) * p_wt_backoff(w, t);
		double denominator = tag_count.getOrDefault(t, 0) + 1 + wordtag_singleton.getOrDefault(t, 0);
		return Math.log(numerator / denominator);
	}
	
	public double p_wt_backoff(String w, String t){
		return ((double)word_count.getOrDefault(w, 0) + 1) / (n + word_count.size());
	}
	
	public double logadd(double x, double y){
		if(y <= x){
			return x + Math.log1p(Math.exp(y - x));
		}
		else{
			return y + Math.log1p(Math.exp(x - y));
		}
	}
	
	public String join(String a, String b){
		return a + "/" + b;
	}
	
	public class Pair{
		String key;
		double value;
		Pair(String key, double value){
			this.key = key;
			this.value = value;
		}
		public void setKey(String key){
			this.key = key;
		}
		public String getKey(){
			return key;
		}
		public void setValue(double value){
			this.value = value;
		}
		public double getValue(){
			return value;
		}
	}
	
	public Pair[] pairBuilder(int size){
		Pair[] res = new Pair[size];
		for(int i=0; i<res.length; i++)
			res[i] = new Pair("###", Double.NEGATIVE_INFINITY);
		return res;
	}
}
