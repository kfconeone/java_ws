package com.kfc.kfconeone;

// Imports the Google Cloud client library
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;
import com.kfc.kfconeone.RTDB.RTDataBase;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import sun.misc.IOUtils;
import sun.nio.ch.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Arrays;


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
