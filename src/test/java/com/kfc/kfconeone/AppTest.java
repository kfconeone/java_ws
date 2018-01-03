package com.kfc.kfconeone;

import com.kfc.kfconeone.RTDB.RTDataBase;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;



/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    RTDataBase db;
    public AppTest( String testName )
    {
        super( testName );
        db = new RTDataBase();
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() throws Exception
    {


        assertTrue( true );
    }

}
