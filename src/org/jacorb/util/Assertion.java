package org.jacorb.util;

/**
 * AssertionViolation.java
 *
 * @author 
 * @version $Revision: 1.4 $
 */

public class Assertion
{
    /**
     */
    
    public static void myAssert( boolean myAssertion, String msg )
    {
        if( !myAssertion )
            throw new AssertionViolation(msg);
    }  
}






