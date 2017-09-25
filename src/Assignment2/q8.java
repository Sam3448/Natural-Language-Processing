package Assignment2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

public class q8 {
	static String file;
	static String word[];
	static int dimension;
	static int wordNum;
	static int limit = 10;
	static HashMap<String, Double[]> word2Vec;
	static PriorityQueue<Node> pq;
	
	public static void main(String[] args) throws Exception{
		file=args[0];
		word = new String[3];
		if(args.length < 3)
			for(int i=0; i<3; i++) word[i] = args[1];
		else 
			for(int i=0; i<3; i++) word[i] = args[i+1];
		pq = new PriorityQueue<>( (a, b) -> (a.score<b.score?-1:1));
		word2Vec = new HashMap();
		readFile(file);
		wordNum=word2Vec.size();
		HashMap<String, Double>word2Score = getScore(word, word2Vec);
		Ranking(pq, word2Score, limit);
		Node[] res = new Node[limit];
		for(int i=0; i<limit; i++) res[i] = pq.poll();
		swap(res);
		for(int i=0; i<limit; i++) System.out.println(res[i].score+"\t"+res[i].word);
	}
	public static void readFile(String file) throws Exception{
		BufferedReader bfr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(file))));
		String line="";
		int count=0;
		while((line=bfr.readLine())!=null){
			String[] temp = line.split("\\s+");
			List<String> cur = new ArrayList();
			for(String s:temp){
				if(s.contains(" ")){
					String[] temp2 = s.split(" ");
					cur.add(temp2[0]);
					cur.add(temp2[1]);
				}
				else cur.add(s);
			}
			if(cur.size() == 2) continue;
			dimension = cur.size()-1;
			String word = cur.get(0);
			word2Vec.putIfAbsent(word, new Double[dimension]);
			Double[] vec = word2Vec.get(word);
			for(int i=1; i<dimension+1; i++) vec[i-1] = Double.parseDouble(cur.get(i));
		}
	}
	public static HashMap<String, Double> getScore(String[] word, HashMap<String, Double[]> word2Vec){
		HashMap<String, Double> word2Score = new HashMap();
		if(!word2Vec.containsKey(word[0])||!word2Vec.containsKey(word[1])||!word2Vec.containsKey(word[2])) return word2Score;
		Double[] target = new Double[dimension];
		for(int j = 0; j < dimension; j++) target[j] = word2Vec.get(word[0])[j] - word2Vec.get(word[1])[j] + word2Vec.get(word[2])[j];
		for(String cur : word2Vec.keySet()){
			double distance = -1;
			if(!cur.equals(word[0])&&!cur.equals(word[1])&&!cur.equals(word[2])){
				double timeAdd1=0;
				double timeAdd2=0;
				double timeBoth=0;
				for(int k=0;k<dimension;k++){
					timeAdd1+=target[k]*target[k];
					timeAdd2+=word2Vec.get(cur)[k]*word2Vec.get(cur)[k];
					timeBoth+=target[k]*word2Vec.get(cur)[k];
				}
				distance=timeBoth/(Math.sqrt(timeAdd1)*Math.sqrt(timeAdd2));
			}
			word2Score.put(cur, distance);
		}
		return word2Score;
	}
	public static void Ranking(PriorityQueue<Node> pq, HashMap<String, Double> word2Score, int limit){
		for(String key : word2Score.keySet()){
			pq.offer(new Node(key, word2Score.get(key)));
			while(pq.size()>limit) pq.poll();
		}
	}
	public static void swap(Node[] res){
		int l = 0, r = res.length-1;
		while(l < r){
			Node temp = res[l];
			res[l++] = res[r];
			res[r--] = temp;
		}
	}
}
class Node{
	String word;
	Double score;
	Node(String word, Double score){
		this.word = word;
		this.score = score;
	}
}
