package org.nato.netn.base;

import org.slf4j.Logger;

import de.fraunhofer.iosb.tc_lib.AbstractTestCase;
import de.fraunhofer.iosb.tc_lib.IVCT_BaseModel;
import de.fraunhofer.iosb.tc_lib.TcInconclusive;
import de.fraunhofer.iosb.tc_lib_if.TcFailedIf;
import de.fraunhofer.iosb.tc_lib_if.TcInconclusiveIf;

public class TC_BASE_0001 extends AbstractTestCase {

    @Override
    protected IVCT_BaseModel getIVCT_BaseModel(String tcParamJson, Logger logger) throws TcInconclusive {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void logTestPurpose(Logger logger) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void preambleAction(Logger logger) throws TcInconclusiveIf {
        logger.info("TC_BASE_0001 preamble");

        // TODO establish precondition for testing
        
    }

    @Override
    protected void performTest(Logger logger) throws TcInconclusiveIf, TcFailedIf {
        logger.info("TC_BASE_0001 test case started");

        // TODO implement the test logic here
        // throw new TcInconclusiveIf("no test case implementation");
        
        logger.info("TC_BASE_0001 test case passed");
    }

    @Override
    protected void postambleAction(Logger logger) throws TcInconclusiveIf {
        logger.info("TC_BASE_0001 postamble");

        // TODO clean up test artefacts after finished test
        
    }
    
}
