package iudx.catalogue;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.relevantcodes.extentreports.LogStatus;

public class TestListener extends MyRestIT implements ITestListener {
	 
    private static String getTestMethodName(ITestResult iTestResult) {
        return iTestResult.getMethod().getConstructorOrMethod().getName();
    }
 
    @Override
    public void onStart(ITestContext iTestContext) {
     }
 
    @Override
    public void onFinish(ITestContext iTestContext) {
        ExtentTestManager.endTest();
        ExtentManager.getReporter().flush();
    }
 
    @Override
    public void onTestStart(ITestResult iTestResult) {
     }
 
    @Override
    public void onTestSuccess(ITestResult iTestResult) {
         ExtentTestManager.getTest().log(LogStatus.PASS, "Passed");
    }
 
    @Override
    public void onTestFailure(ITestResult iTestResult) {
     }
 
    @Override
    public void onTestSkipped(ITestResult iTestResult) {
         ExtentTestManager.getTest().log(LogStatus.SKIP, "Test Skipped");
    }
 
    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult iTestResult) {
     }
}