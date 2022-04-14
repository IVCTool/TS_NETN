package org.nato.ivct.tc_lib_if;

import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;

public interface TestSuite {
    AbstractTestCaseIf getTestCase (String TestCaseId);
}
