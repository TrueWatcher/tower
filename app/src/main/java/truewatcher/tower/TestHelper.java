package truewatcher.tower;

import java.util.List;

import android.text.TextUtils;
import android.widget.TextView;

public class TestHelper {
  private static TestHelper sMe;
  private static TextView sOutputElement;
  private static String sOutput="";
  private int mCountAssert=0;
  
  public static TestHelper getInstance(TextView aView) {
    if(sMe == null) {
      sMe=new TestHelper();
    }
    sOutputElement=aView;
    return sMe;
  }
  
  public void print(String s) {
    sOutput=sOutput.concat(s);
    sOutputElement.setText(sOutput);
  }
  
  public void println(String s) {
    //sOutput=sOutput.concat("\n");
    print(s.concat("\n"));
  }
  
  public void printlnln(String s) {
    sOutput=sOutput.concat("\n");
    print(s.concat("\n"));
  }
  
  public static class TestFailure extends Exception {

    public TestFailure(String aMessage) { super(aMessage); }
  }
  
  public void assertTrue(boolean aStatement, String aMessage, String aMessageOk, String aExplanation) throws TestFailure {
    incCount();
    String out="";
    if (aStatement) {
      out="Passed "+mCountAssert;
      if ( aMessageOk != null &&  ! aMessageOk.isEmpty() ) out+=": "+aMessageOk;
      println(out);
      return;
    }
    out="Failed "+mCountAssert;
    if ( aExplanation != null &&  ! aExplanation.isEmpty() ) out+=": "+aExplanation;
    //out+=": "+aMessage;
    println(out);
    throw new TestFailure(aMessage);
  }
  
  public void assertTrue(boolean aStatement, String aMessage, String aMessageOk) throws TestFailure {
    assertTrue(aStatement, aMessage, aMessageOk, "");
  }
  
  public void assertTrue(boolean aStatement, String aMessage) throws TestFailure {
    assertTrue(aStatement, aMessage, "", "");
  }
  
  public void assertEquals(int aExpected,int aFound, String aMessage, String aMessageOk) throws TestFailure {
    String expl=aFound+" does not equal to the expected "+aExpected;
    assertTrue(aExpected == aFound, aMessage, aMessageOk, expl);
  }
  
  public void assertEquals(String aExpected,String aFound, String aMessage, String aMessageOk) throws TestFailure {
    String expl=aFound+" does not equal to the expected "+aExpected;
    assertTrue(aExpected.equals(aFound), aMessage, aMessageOk, expl);
  }
  
  public void assertEqualsList(List<String> aExpected, List<String> aFound, String aMessage, String aMessageOk) throws TestFailure {
    String e=TextUtils.join(", ", aExpected);
    String f=TextUtils.join(", ", aFound);
    String expl=f+" does not equal to the expected "+e;
    assertTrue(e.equals(f), aMessage, aMessageOk, expl);
  }
  
  public void assertContains(String aExpected,String aHaystack, String aMessage, String aMessageOk) throws TestFailure {
    String expl=aHaystack+" does not contain "+aExpected;
    assertTrue(aHaystack.indexOf(aExpected) >= 0, aMessage, aMessageOk, expl);
  }
  
  public void assertNotContains(String aExpected,String aHaystack, String aMessage, String aMessageOk) throws TestFailure {
    String expl=aHaystack+" still contains "+aExpected;
    assertTrue(aHaystack.indexOf(aExpected) < 0, aMessage, aMessageOk, expl);
  }
  
  public void csvLineDiff(String aExpected, String aObtained, String SEP) throws TestFailure {
    aExpected=aExpected.trim();
    String[] esa=TextUtils.split(aExpected, SEP);
    aObtained=aObtained.trim();
    String[] osa=TextUtils.split(aObtained, SEP);
    if (esa.length != osa.length) throw new TestFailure("Expected "+esa.length+" fields, got "+osa.length);
    String SAME="=";
    String DIFF="/";
    String ef,of;
    String[] rsa=new String[esa.length];
    for (int i=0; i < esa.length; i+=1) {
      ef=esa[i];
      of=osa[i];
      if (ef.isEmpty() && of.isEmpty()) { rsa[i]=""; }
      else if (ef.equals(of)) { rsa[i]=SAME; }
      else { rsa[i]=ef+DIFF+of; }
    }
    println(TextUtils.join(SEP,rsa));
  }
  
  private void incCount() { mCountAssert+=1; }
}
