package Assignment1;

import java.awt.geom.GeneralPath;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;


public class q1 {
	static boolean Tree;
	static String file;
	static int sentenceNum;
	static String LHS="ROOT";
	static List<String> senTemplate;
	static List<Double> senTemplatePower;
	static ParseTree head;
	static HashMap<String, ParseTree> toTreePosition;
	static Random seedGen=new Random(1344l);
	static Random templateChose=new Random(1232444l);
	public static void main(String[] args) throws Exception{
		Tree=args[0].equals("t")?true:false;
		//Tree=true;
		file=args[1];
		//file="/Users/SamZhang/Documents/NLP/hw/grammar3.gr";
		sentenceNum=Integer.parseInt(args[2]);
		//sentenceNum=3;
		senTemplate=new ArrayList();
		senTemplatePower=new ArrayList();
		toTreePosition=new HashMap();
		Reader();
		head=toTreePosition.get("S");
		for(int i=0;i<sentenceNum;i++) Generator();
	}
	public static void Reader() throws Exception{
		BufferedReader bfr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(file))));
		String line="";
		int count=0;
		while((line=bfr.readLine())!=null){
			count++;
			line=line.trim();
			if(line.length()==0||line.charAt(0)=='#') continue;
			String[] tokens=line.split("\\s+");
			double curPower=0;
			for(int i=0;i<tokens.length;i++){
				//System.out.println(Arrays.toString(tokens));
				String token=tokens[i];
				if(isNumeric(token)){
					curPower=Double.parseDouble(token);
					continue;
				}
				if(token.charAt(0)=='#') break;
				if(token.contains(LHS)){
					StringBuilder sb=new StringBuilder();//filter out comment
					for(int j=0;j<tokens.length;j++){
						if(tokens[j].charAt(0)!='#') sb.append(tokens[j]+" ");
						else break;
					}
					senTemplate.add(sb.toString().trim());//ROOT
					senTemplatePower.add(curPower);
					break;
				}
				else{
					toTreePosition.putIfAbsent(token, new ParseTree(token, seedGen.nextLong()));
					ParseTree node=toTreePosition.get(token);
					node.builder(tokens, i+1, curPower);
					break;
				}
			}
		}
	}
	public static String Generator(){
		int index=randomPicker(templateChose, senTemplatePower);
		String template=senTemplate.get(index);
		//System.out.println("Template : "+template);
		String[] tempArray=template.split("\\s");
		StringBuilder sb=new StringBuilder();
		for(String s:tempArray){
			if(isNumeric(s)) continue;
			if(s.equals(LHS)){
				if(Tree) sb.append("("+LHS+" ");
				continue;
			}
			if(toTreePosition.containsKey(s)){
				if(Tree) sb.append("("+s+" "+sentenceBuilder(toTreePosition.get(s), sb.length()+3)+")\n"+getSpaces(6));
				else sb.append(sentenceBuilder(toTreePosition.get(s), 9)+" ");
			}
			else sb.append(s+" ");
		}
		System.out.println(sb.toString().trim()+(Tree?")":""));
		return sb.toString().trim();
	}
	public static boolean isNumeric(String s){
		for(char c:s.toCharArray())
			if(!Character.isDigit(c)) return false;
		return true;
	}
	public static boolean isAllLowerCase(String s){
		for(char c:s.toCharArray())
			if(!Character.isLowerCase(c)) return false;
		return true;
	}
	public static boolean isComment(String s){
		return s.charAt(0)=='#';
	}
	public static String sentenceBuilder(ParseTree node, int tab){
		double childPowerSum=0, wordPowerSum=0;
		for(double p:node.childPower) childPowerSum+=p;

		for(double p:node.wordPower) wordPowerSum+=p;
		double regularChildPower=childPowerSum/(childPowerSum+wordPowerSum);
		double regularWordPower=wordPowerSum/(childPowerSum+wordPowerSum);
		double categoryRandom=node.r.nextDouble();
		if(node.isTermination&&(node.child.isEmpty()||categoryRandom<regularWordPower)){
			int index=q1.randomPicker(node.r, node.wordPower);
			//return node.words.get(index);
			return Tree?node.words.get(index).replaceAll(" ", "\n"+getSpaces(tab)):node.words.get(index);
		}
		//System.out.println(node.name+" "+node.childPower.size());
		List<ParseTree> cur=node.child.get(q1.randomPicker(node.r, node.childPower));
		StringBuilder sb=new StringBuilder();
		for(ParseTree temp:cur){
			String ret=sentenceBuilder(temp,tab+1+temp.name.length()+1);
			boolean isRetaWord=!ret.contains("(");
			String res="";
			if(isRetaWord) res=Tree?"("+temp.name+" "+ret+")":ret;
			else res=Tree?"("+temp.name+" "+ret+")":ret;
			sb.append(res+(Tree?"\n"+getSpaces(tab):" "));
		}
		return sb.toString().trim();
	}
	public static int randomPicker(Random r, List<Double> power){
		int len=power.size();
		int index=0;
		double sum=power.get(0);
		for(int i=1;i<len;i++){
			sum+=power.get(i);
			index=r.nextDouble()<=(power.get(i)/sum)?i:index;
		}
		return index;
	}
	public static String getSpaces(int i){
		StringBuilder sb=new StringBuilder();
		while(i-->0) sb.append(" ");
		return sb.toString();
	}
}
class ParseTree{
	String name;
	List<List<ParseTree>> child;
	List<Double> childPower;
	boolean isTermination;
	List<String> words;
	List<Double> wordPower;
	Random r;
	public ParseTree(String name, long seed){
		this.name=name;
		child=new ArrayList();
		childPower=new ArrayList();
		words=new ArrayList();
		wordPower=new ArrayList();
		isTermination=false;
		r=new Random(seed);
	}
	public void builder(String[] tokens, int start, double power){
		for(int i=start;i<tokens.length;i++){
			if(q1.isAllLowerCase(tokens[i])){//termination
				isTermination=true;
				StringBuilder sb=new StringBuilder();
				while(i<tokens.length) sb.append(tokens[i++]+" ");	
				words.add(sb.toString().trim());
				wordPower.add(power);
			}
			else if(q1.isComment(tokens[i])) break;//comment
			else{//nontermination
				List<ParseTree> temp=new ArrayList();
				while(i<tokens.length){
					if(q1.isComment(tokens[i])) break;
					q1.toTreePosition.putIfAbsent(tokens[i], new ParseTree(tokens[i], r.nextLong()));
					temp.add(q1.toTreePosition.get(tokens[i++]));
				}
				this.child.add(temp);
				childPower.add(power);
			}
		}
	}
	/*public String sentenceBuilder(ParseTree node){
		System.err.println("here");
		double childPowerSum=0, wordPowerSum=0;
		for(double p:childPower) childPowerSum+=p;
		for(double p:wordPower) wordPowerSum+=p;
		double regularChildPower=childPowerSum/(childPowerSum+wordPowerSum);
		double regularWordPower=wordPowerSum/(childPowerSum+wordPowerSum);
		double categoryRandom=r.nextDouble();
		if(node.isTermination&&(node.child.isEmpty()||categoryRandom<regularWordPower)){
			int index=q1.randomPicker(r, wordPower);
			System.out.println(this.name+"  "+words.get(index));
			return words.get(index);
		}
		List<ParseTree> cur=child.get(q1.randomPicker(r, childPower));
		StringBuilder sb=new StringBuilder();
		for(ParseTree temp:cur) sb.append(sentenceBuilder(temp)+" ");
		return sb.toString().trim();
	}*/
}
