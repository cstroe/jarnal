package jarnal;

import java.util.*;

class token{
	public boolean isNumber;
	public String token;
	public double ntoken;
}

class Expression{
	private static Hashtable unary = null;
	private static Hashtable binary = null;
	private LinkedList tStack;
	public double operand1;
	private double operand2;
	public String operator;
	public int state = 0;
	public String delimiter;
	
	private static void init(){
		unary = new Hashtable();
		binary = new Hashtable();
		unary.put("+", "O");
		unary.put("-", "O");
		unary.put("(", "(");
		binary.put("+", new Integer(0));
		binary.put("-", new Integer(0));
		binary.put("*", new Integer(1));
		binary.put("/", new Integer(1));
		binary.put("(", new Integer(3));
		binary.put(")", new Integer(4));
		binary.put("^", new Integer(2));
	}
	
	public Expression(LinkedList tStack){
		this.tStack = tStack;
		if(unary == null) init();
	}

	private double binop(double x1, double x2, String op){
		double ans = 0.0;
		if(op.equals("+")) ans = x1 + x2;
		else if(op.equals("-")) ans = x1 - x2;
		else if(op.equals("*")) ans = x1 * x2;
		else if(op.equals("/")) ans = x1 / x2;
		else if(op.equals("^")) ans = Math.pow(x1, x2);
		return ans;
	}

	public double eval(){
	//System.out.println("state=" + state + " operand1=" + operand1 + " operator=" + operator + " operand2=" + operand2 + " delimiter=" + delimiter);
		double ans = 0.0;
		if(state == 0){
			if(tStack.size() == 0){
				System.err.println("unexpected end of input stream looking for start of expression");
				return ans;
			}
			token t = (token) tStack.remove(0);
			if(t.isNumber) {
				operand1 = t.ntoken;
				state = 1;
			}
			else{
				String z = t.token;
				String y = (String) unary.get(z);
				if(y == null) {
					System.err.println("expected a unary operator not " + z);
					return ans;
				}
				else{
					Expression ex = new Expression(tStack);
					if(y.equals("O")) {
						ans = ex.eval();
						if(z.equals("-")) ans = -ans;
					}
					if(y.equals("(")) {
						ex.delimiter = z;
						operand1 = ex.eval();
						state = 1;
					}
				}
			
			}
		}
		if(state == 1){
			if(tStack.size() == 0) return operand1;
			token t = (token) tStack.remove(0);
			if(t.isNumber) {
				System.err.println("expected a binary operator not " + t.ntoken);
				return ans;
			}
			else{
				String z = t.token;
				Integer y = (Integer) binary.get(z);
				if(y == null) {
					System.err.println("expected binary operator not " + z);
					return ans;
				}
				else {
					if(y.intValue() == 4){
						if(delimiter != null) ans = operand1;
						else {
							System.err.println("unexpected delimiter " + z);
							return ans;
						}
					}
					else {
						Expression ex = new Expression(tStack);
						if(y.intValue() == 3) {
							ex.operator = "*";
							tStack.add(0, t);
						}
						else ex.operator = z;
						ex.operand1 = operand1;
						ex.delimiter = delimiter;
						ex.state = 2;
						ans = ex.eval();
					}					
				}
			}
		}
		if(state == 2){
			if(tStack.size() == 0){
				System.err.println("unexpected end of input stream looking for second operand");
				return ans;
			}
			token t = (token) tStack.remove(0);
			if(t.isNumber) operand2 = t.ntoken;
			else {
				String z = t.token;
				String y = (String) unary.get(z);
				if(y != null){
					tStack.add(0, t);
					Expression ex = new Expression(tStack);
					operand2 = ex.eval();
				}
				else {
					System.err.println("expected unary operator not " + z);
					return ans;
				}
			}
			if(tStack.size() == 0) return binop(operand1, operand2, operator);
			t = (token) tStack.get(0);
			if(t.isNumber) {
				System.err.println("expected operator not number " + t.ntoken);
				return ans;
			}
			else {
				String z = t.token;
				Integer y = (Integer) binary.get(z);
				if(y == null) {
					System.err.println("expected binary operator not " + z);
					return ans;
				}
				else {
					if(y.intValue() == 4) {
						ans = binop(operand1, operand2, operator);
						if(delimiter != null) tStack.remove(0);
					}
					else{
						if(y.intValue() == 3){
							token tt = new token();
							tt.isNumber = false;
							z = "*";
							tt.token = z;
							tStack.add(0, tt);
							y = new Integer(1);
						}
						Integer yy = (Integer) binary.get(operator);
						if(y.intValue() > yy.intValue()){
							Expression ex = new Expression(tStack);
							ex.operand1 = operand2;
							tStack.remove(0);
							ex.operator = z;
							ex.state = 2;
							ans = binop(operand1, ex.eval(), operator);
							if(delimiter != null){
								t = (token) tStack.remove(0);
								if(((Integer) binary.get(t.token)).intValue() !=4)
									System.out.println("delimiter errors");
							}
						}
						else {
							operand1 = binop(operand1, operand2, operator);
							state = 1;
							ans = this.eval();
						} 
					}
				}
			}
		}
		return ans;

	}
}

public class Calculator{

	String s;
	int pos;
	char y[];
	boolean isNumber;
	public LinkedList tStack = new LinkedList();

	public Calculator(String s){
		this.s = s;
		//this.s = "1+2(3+4*5.0)";
		y = this.s.toCharArray();
		pos = -1;
	}
	
	public double calc(){
		String z = nextToken();
		while(z != null){
			token t = new token();
			t.isNumber = isNumber;
			if(isNumber) {
				try{
					t.ntoken = Double.parseDouble(z);
				}
				catch(Exception ex){
					System.err.println("unrecognized token " + z);
					return 0.0;
				}
			}
			else t.token = z;
			tStack.add(t);
			z = nextToken();
		}
		return new Expression(tStack).eval();
	}

	private String nextToken(){
		pos++;
		isNumber = false;
		for(pos = pos; pos < s.length(); pos++) if(!s.substring(pos, pos + 1).equals(" ")) break;
		if(pos == s.length()) return null;
		String x = s.substring(pos, pos + 1);
		if(x.equals("*")) return x;
		else if(x.equals("/")) return x;
		else if(x.equals("^")) return x;
		else if(x.equals("(")) return x;
		else if(x.equals(")")) return x;
		else if(x.equals("=")) return x;
		else if(x.equals("+")) return x;
		else if(x.equals("-")) return x;
		x = "";
		isNumber = true;
		for(; pos < s.length(); pos++){
			boolean flag = true;
			if(y[pos] == '.') flag = false;
			if(Character.isDigit(y[pos])) flag = false;
			if(flag) break;
			x = x + s.substring(pos, pos + 1);
		}
		pos--;	
		return x;	
	}
}
