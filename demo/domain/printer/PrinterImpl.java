package demo.domain.printer;
/**
 * PrinterImpl.java
 * Created: Fri Sep  8 19:11:26 2000
 * A dummy implementation of the IDL interface Printer
 * @author Herbert Kiefer
 * @version $Revision: 1.1 $
 */

public class PrinterImpl extends PrinterPOA 
{
  
  public PrinterImpl() 
  {}

  /** pseudo prints data. */
  public void printFile(java.lang.String data)
  {
    // dummy 
    System.out.println("PrinterImpl.printFile:  printing file with "
		       + data.length() + " bytes.");
  } // printFile

} // PrinterImpl
