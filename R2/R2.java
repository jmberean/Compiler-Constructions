// Hand-written R2 compiler
import java.io.*;
import java.util.*;
//======================================================
class R2
{  
  public static void main(String[] args) throws 
                                             IOException
  {
    System.out.println("R2 compiler written by ...");

    if (args.length != 1)
    {
      System.err.println("Wrong number cmd line args");  
      System.exit(1);
    }

    // set to true to debug token manager
    boolean debug = false;

    // build the input and output file names
    String inFileName = args[0] + ".s";
    String outFileName = args[0] + ".a";

    // construct file objects
    Scanner inFile = new Scanner(new File(inFileName));
    PrintWriter outFile = new PrintWriter(outFileName);

    // identify compiler/author in the output file
    outFile.println("; from R2 compiler written by ...");

    // construct objects that make up compiler
    R2SymTab st = new R2SymTab();
    R2TokenMgr tm =  new R2TokenMgr(
                                inFile, outFile, debug);
    R2CodeGen cg = new R2CodeGen(outFile, st);
    R2Parser parser = new R2Parser(st, tm, cg);

    // parse and translate
    try
    {
      parser.parse();
    }      
    catch (RuntimeException e) 
    {
      System.err.println(e.getMessage());
      outFile.println(e.getMessage());
      outFile.close();
      System.exit(1);
    }

    outFile.close();
  }
}                                           // end of R2
//======================================================
interface R2Constants
{
  // --R2 integers that identify token kinds
  int EOF = 0;
  int PRINTLN = 1;
  int UNSIGNED = 2;
  int ID = 3;
  int ASSIGN = 4;
  int SEMICOLON = 5;
  int LEFTPAREN = 6;
  int RIGHTPAREN = 7;
  int PLUS = 8;
  int MINUS = 9;
  int TIMES = 10;
  int ERROR = 11;

  int DIV = 12;
  int PRINT = 13;
  int LEFTCURLYBRACKET = 14;
  int RIGHTCURLYBRACKET = 15;
  int READINT = 16;


  // --R2 tokenImage provides string for each token kind
  String[] tokenImage = 
  {
    "<EOF>",
    "\"println\"",
    "<UNSIGNED>",
    "<ID>",
    "\"=\"",
    "\";\"",
    "\"(\"",
    "\")\"",
    "\"+\"",
    "\"-\"",
    "\"*\"",
    "<ERROR>",
    "\"/\"",
	"\"print\"",
    "\"{\"",
    "\"}\"",
    "\"readint\""
  };
}                        // end of R2Constants interface
//======================================================
class R2SymTab
{
  private ArrayList<String> symbol;
  private ArrayList<String> dwValue;
  private ArrayList<Boolean> needsdw;
  //-----------------------------------------
  public R2SymTab()
  {
    symbol = new ArrayList<String>();
    dwValue = new ArrayList<String>();
    needsdw = new ArrayList<Boolean>();
  }
  //-----------------------------------------
  public int enter(String s, String v, boolean b)
  {
    int index = symbol.indexOf(s);
    if (index >= 0)    // s already in symbol?
      return index;    // yes, then return its index

    index = symbol.size();
    symbol.add(s);     // add symbol
    dwValue.add(v);    // add value
    needsdw.add(b);    // add needsdw value
    return index;
  }
  //-----------------------------------------
  public String getSymbol(int index)
  {
    return symbol.get(index);
  }
  //-----------------------------------------
  public String getdwValue(int index)
  {
    return dwValue.get(index);
  }
  //-----------------------------------------
  public boolean getNeedsdw(int index)
  {
    return needsdw.get(index);
  }
  //-----------------------------------------
  public void setNeedsdw(int index)
  {
    needsdw.set(index, true);
  }
  //-----------------------------------------
  public int getSize()
  {
    return symbol.size();
  }
}                                     // end of R2SymTab
//======================================================
class R2TokenMgr implements R2Constants
{
  private Scanner inFile;          
  private PrintWriter outFile;
  private boolean debug;
  private char currentChar;
  private int currentColumnNumber;
  private int currentLineNumber;
  private String inputLine;    // holds 1 line of input
  private Token token;         // holds 1 token
  private StringBuffer buffer; // token image built here
  //-----------------------------------------
  public R2TokenMgr(Scanner inFile, 
                    PrintWriter outFile, boolean debug)
  {
    this.inFile = inFile;
    this.outFile = outFile;
    this.debug = debug;
    currentChar = '\n';        //  '\n' triggers read
    currentLineNumber = 0;
    buffer = new StringBuffer();
  }
  //-----------------------------------------
  public Token getNextToken()
  {
    // skip whitespace
    while (Character.isWhitespace(currentChar))
      getNextChar();

    // construct token to be returned to parser
    token = new Token();   
    token.next = null;

    // save start-of-token position
    token.beginLine = currentLineNumber;
    token.beginColumn = currentColumnNumber;

    // check for EOF
    if (currentChar == EOF)
    {
      token.image = "<EOF>";
      token.endLine = currentLineNumber;
      token.endColumn = currentColumnNumber;
      token.kind = EOF;
    }

    else  // check for unsigned int
    if (Character.isDigit(currentChar)) 
    {
      buffer.setLength(0);  // clear buffer
      do  // build token image in buffer
      {
        buffer.append(currentChar); 
        token.endLine = currentLineNumber;
        token.endColumn = currentColumnNumber;
        getNextChar();
      } while (Character.isDigit(currentChar));
      // save buffer as String in token.image
      token.image = buffer.toString();
      token.kind = UNSIGNED;
    }

    else  // check for identifier
    if (Character.isLetter(currentChar)) 
    { 
      buffer.setLength(0);  // clear buffer
      do  // build token image in buffer
      {
        buffer.append(currentChar);
        token.endLine = currentLineNumber;
        token.endColumn = currentColumnNumber;
        getNextChar();
      } while (Character.isLetterOrDigit(currentChar));
      // save buffer as String in token.image
      token.image = buffer.toString();

      // --R2 check if keyword
      if (token.image.equals("print"))
        token.kind = PRINT;
    else if (token.image.equals("println"))
        token.kind = PRINTLN;
    else if (token.image.equals("readint"))
        token.kind = READINT;
    else  // not a keyword so kind is ID
        token.kind = ID;
    }

    else  // --R2 process single-character token
    {  
      switch(currentChar)
      {
        case '=':
          token.kind = ASSIGN;
          break;                               
        case ';':
          token.kind = SEMICOLON;
          break;                               
        case '(':
          token.kind = LEFTPAREN;
          break;                               
        case ')':
          token.kind = RIGHTPAREN;
          break;                               
        case '+':
          token.kind = PLUS;
          break;                               
        case '-':
          token.kind = MINUS;
          break;                               
        case '*':
          token.kind = TIMES;
          break;                               
          case '/':
          token.kind = DIV;
          break;   
        case '{':
          token.kind = LEFTCURLYBRACKET;
          break; 
        case '}':
          token.kind = RIGHTCURLYBRACKET;
          break; 		  
        default:
          token.kind = ERROR;
          break;                                  
      }

      // save currentChar as String in token.image
      token.image = Character.toString(currentChar);

      // save end-of-token position
      token.endLine = currentLineNumber;
      token.endColumn = currentColumnNumber;

      getNextChar();  // read beyond end of token
    }

    // token trace appears as comments in output file
    if (debug)
      outFile.printf(
        "; kd=%3d bL=%3d bC=%3d eL=%3d eC=%3d im=%s%n",
        token.kind, token.beginLine, token.beginColumn, 
        token.endLine, token.endColumn, token.image);

    return token;     // return token to parser
  }     
  //-----------------------------------------
  private void getNextChar()
  {
    if (currentChar == EOF)
      return;

    if (currentChar == '\n')        // need next line?
    {
      if (inFile.hasNextLine())     // any lines left?
      {
        inputLine = inFile.nextLine();  // get next line
        // output source line as comment
        outFile.println("; " + inputLine);
        inputLine = inputLine + "\n";   // mark line end
        currentColumnNumber = 0;
        currentLineNumber++;   
      }                                
      else  // at end of file
      {
         currentChar = EOF;
         return;
      }
    }

    // get next char from inputLine
    currentChar = 
                inputLine.charAt(currentColumnNumber++);

    // --R2 in S2, test for single-line comment goes here
    if(currentChar == '/' && inputLine.charAt(currentColumnNumber) == '/'){
		currentChar = '\n';
	}
  }
}                                   // end of R2TokenMgr
//======================================================
class R2Parser implements R2Constants
{
  private R2SymTab st;
  private R2TokenMgr tm;
  private R2CodeGen cg;
  private Token currentToken;
  private Token previousToken;
  //-----------------------------------------
  public R2Parser(R2SymTab st, R2TokenMgr tm, 
                                           R2CodeGen cg)
  {
    this.st = st;
    this.tm = tm;
    this.cg = cg;   
    currentToken = tm.getNextToken();   // prime
    previousToken = null;
  }
  //-----------------------------------------
  // Construct and return an exception that contains
  // a message consisting of the image of the current
  // token, its location, and the expected tokens.
  //
  private RuntimeException genEx(String errorMessage)
  {
    return new RuntimeException("Encountered \"" + 
      currentToken.image + "\" on line " + 
      currentToken.beginLine + " column " + 
      currentToken.beginColumn +
      System.getProperty("line.separator") + 
      errorMessage);
  }
  //-----------------------------------------
  // Advance currentToken to next token.
  //
  private void advance()
  {
    previousToken = currentToken; 

    // If next token is on token list, advance to it.
    if (currentToken.next!=null)
      currentToken = currentToken.next;

    // Otherwise, get next token from token mgr and 
    // put it on the list.
    else
      currentToken = 
                  currentToken.next = tm.getNextToken();
  }
  //-----------------------------------------
  // getToken(i) returns ith token without advancing
  // in token stream.  getToken(0) returns 
  // previousToken.  getToken(1) returns currentToken.
  // getToken(2) returns next token, and so on.
  //
  private Token getToken(int i)
  {
    if (i <= 0)
      return previousToken;

    Token t = currentToken;
    for (int j = 1; j < i; j++)  // loop to ith token
    {
      // if next token is on token list, move t to it
      if (t.next != null)
        t = t.next;

      // Otherwise, get next token from token mgr and 
      // put it on the list.
      else
        t = t.next = tm.getNextToken();
    }
    return t;
  }
  //-----------------------------------------
  // If the kind of the current token matches the
  // expected kind, then consume advances to the next
  // token. Otherwise, it throws an exception.
  //
  private void consume(int expected)
  {
    if (currentToken.kind == expected)
      advance();
    else
      throw genEx("Expecting " + tokenImage[expected]);
  }
  //-----------------------------------------
  public void parse()
  {
    program();
  }
  //-----------------------------------------
  private void program()
  {
    statementList();
    cg.endCode();
    if (currentToken.kind != EOF)
      throw genEx("Expecting <EOF>");
  }
  //--R2---------------------------------------
  private void statementList()
  {
    switch(currentToken.kind)
    {
      case ID:
      case PRINTLN:
      case PRINT:
      case SEMICOLON:
      case LEFTCURLYBRACKET:
        statement();
        statementList();
        break;
      case EOF:
      case RIGHTCURLYBRACKET:
        ;
        break;
      default:
        throw genEx("Expecting statement or <EOF>");
    }
  }
  //--R2---------------------------------------
  private void statement()
  {
    switch(currentToken.kind)
    {
       case ID: 
         assignmentStatement(); 
         break;
       case PRINTLN:    
         printlnStatement(); 
         break;
        case PRINT:    
         printStatement(); 
         break;
       case SEMICOLON:    
         nullStatement(); 
         break;
     case LEFTCURLYBRACKET:    
         compoundStatement(); 
         break;
       default:         
         throw genEx("Expecting statement");
    }
  }
  //-----------------------------------------
  private void assignmentStatement()
  {
    Token t;
    int left, expVal;

    t = currentToken;
    consume(ID);
    left  = st.enter(t.image, "0", true);
    consume(ASSIGN);
    expVal = expr();
    cg.assign(left, expVal);
    consume(SEMICOLON);
  }
  //-----------------------------------------
  private void printlnStatement()
  {
    int expVal;

    consume(PRINTLN);
    consume(LEFTPAREN);
    expVal = expr();
    cg.println(expVal);
    consume(RIGHTPAREN);
    consume(SEMICOLON);
  }
//--R2---------------------------------------
private void printStatement()
{
    int expVal;

    consume(PRINT);
    consume(LEFTPAREN);
    expVal = expr();
    cg.print(expVal);
    consume(RIGHTPAREN);
    consume(SEMICOLON);
}
   //--R2---------------------------------------
   private void nullStatement()
   {
     consume(SEMICOLON);
   }
    //--R2---------------------------------------
   private void compoundStatement()
   {
     consume(LEFTCURLYBRACKET);
     statementList();
     consume(RIGHTCURLYBRACKET);
   }
  //-----------------------------------------
  private int expr()
  {
    int left, expVal;

    left  = term();
    expVal = termList(left);
    return expVal;
  }
  //-----------------------------------------
  private int termList(int left)
  {
    int right, temp, expVal;

    switch(currentToken.kind)
    {
      case PLUS:
        consume(PLUS);
        right = term();
        temp = cg.add(left, right);   // emits ld/add/st
        expVal = termList(temp);
        return expVal;
    case MINUS:
        consume(MINUS);
        right = term();
        temp = cg.sub(left, right);   // emits ld/add/st
        expVal = termList(temp);
        return expVal;
      case RIGHTPAREN:
      case SEMICOLON:
        ;
        return left;     // do this at end of expression
      default:
        throw genEx("Expecting \"+\", \")\", or \";\"");
    }
  }
  //-----------------------------------------
  private int term()
  {
    int left, termVal;

    left = factor();
    termVal = factorList(left);
    return termVal;
  }
  //-----------------------------------------
  private int factorList(int left)
  {
    int right, temp, termVal;

    switch(currentToken.kind)
    {
      case TIMES:
        consume(TIMES);
        right = factor();
        temp = cg.mult(left, right);  //emits ld/mult/st
        termVal = factorList(temp);
        return termVal;
    case DIV:
        consume(DIV);
        right = factor();
        temp = cg.div(left, right);  //emits ld/mult/st
        termVal = factorList(temp);
        return termVal;
      case PLUS:
      case MINUS:
      case RIGHTPAREN:
      case SEMICOLON:
        ;
        return left;           // do this at end of term
      default:
        throw genEx("Expecting op, \")\", or \";\"");
    }
  }
  //-----------------------------------------
  private int factor()
  {  
    Token t;
    int index;

    switch(currentToken.kind)
    {
      case UNSIGNED:
        t = currentToken;
        consume(UNSIGNED);
        index  = st.enter("@" + t.image, t.image, true);
        return index;
      case PLUS:
        consume(PLUS);
        t = currentToken;
        consume(UNSIGNED);
        index  = st.enter("@" + t.image, t.image, true);
        return index;
      case MINUS:
        consume(MINUS);
        t = currentToken;
        consume(UNSIGNED);
        index = st.enter(
                   "@_" + t.image, "-" + t.image, true);
        return index;
      case ID:
        t = currentToken;
        consume(ID);
        index = st.enter(t.image, "0", true);
        return index;
      case LEFTPAREN:
        consume(LEFTPAREN);
        index = expr();
        consume(RIGHTPAREN);
        return index;
      default:
        throw genEx("Expecting factor");
    }
  }
}                                     // end of R2Parser
//======================================================
class R2CodeGen
{
  private PrintWriter outFile;
  private R2SymTab st;
  private int tempIndex;
  //-----------------------------------------
  public R2CodeGen(PrintWriter outFile, R2SymTab st)
  {
    this.outFile = outFile;
    this.st = st;
    tempIndex = 0;
    outFile.println("          !register");
  }
  //-----------------------------------------
  private void emitInstruction(String op)
  {
    outFile.printf("          %-4s%n", op); 
  }
  //-----------------------------------------
  private void emitInstruction(String op, String opnd)
  {           
    outFile.printf("          %-4s      %s%n", op,opnd); 
  }
  //-----------------------------------------
  private void emitInstruction(String op, int opndIndex)
  {           
    emitInstruction(op, st.getSymbol(opndIndex)); 
  }
  //-----------------------------------------
  private void emitdw(String label, String value)
  {           
    outFile.printf(
            "%-9s dw         %s%n", label + ":", value);
  }
  //-----------------------------------------
  public void endCode()
  {
    outFile.println();
    outFile.println("          halt");

    int size = st.getSize();
    // emit a dw if corresponding needsdw value is true
    for (int i=0; i < size; i++) 
       if (st.getNeedsdw(i))
          emitdw(st.getSymbol(i), st.getdwValue(i));
  }
  //-----------------------------------------
  private int getTemp()
  {
    String temp = "@t" + tempIndex++;  // create temp
    return st.enter(temp, "0", true);  // return index
  }
  //-----------------------------------------
  public void assign(int left, int expVal)
  {
    emitInstruction("ld", expVal);
    emitInstruction("st", left);
  }
  //-----------------------------------------
  public void println(int expVal)
  {
    emitInstruction("ld", expVal);
    emitInstruction("dout");
    emitInstruction("ldc", "'\\n'");
    emitInstruction("aout");
  }
    //--R2---------------------------------------
    public void print(int expVal)
    {
        emitInstruction("ld", expVal);
        emitInstruction("dout");
    }
  //-----------------------------------------
  public int add(int left, int right)
  {
    emitInstruction("ld", left);
    emitInstruction("add", right);
    int temp = getTemp();
    emitInstruction("st", temp);
    return temp;
  }
//--R2---------------------------------------
    public int sub(int left, int right)
    {
      emitInstruction("ld", left);
      emitInstruction("sub", right);
      int temp = getTemp();
      emitInstruction("st", temp);
      return temp;
    }
  //-----------------------------------------
  public int mult(int left, int right)
  {
    emitInstruction("ld", left);
    emitInstruction("mult", right);
    int temp = getTemp();
    emitInstruction("st", temp);
    return temp;
  }
//--R2---------------------------------------
    public int div(int left, int right)
    {
      emitInstruction("ld", left);
      emitInstruction("div", right);
      int temp = getTemp();
      emitInstruction("st", temp);
      return temp;
    }
}                                    // end of R2CodeGen