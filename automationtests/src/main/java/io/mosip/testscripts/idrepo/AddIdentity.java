package io.mosip.testscripts.idrepo;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.ITest;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.internal.BaseTestMethod;
import org.testng.internal.TestResult;
import io.mosip.admin.fw.util.AdminTestException;
import io.mosip.admin.fw.util.AdminTestUtil;
import io.mosip.admin.fw.util.TestCaseDTO;
import io.mosip.authentication.fw.dto.OutputValidationDto;
import io.mosip.authentication.fw.precon.JsonPrecondtion;
import io.mosip.authentication.fw.util.AuthenticationTestException;
import io.mosip.authentication.fw.util.OutputValidationUtil;
import io.mosip.authentication.fw.util.ReportUtil;
import io.mosip.authentication.fw.util.RestClient;
import io.mosip.kernel.util.KernelAuthentication;
import io.restassured.response.Response;

public class AddIdentity extends AdminTestUtil implements ITest {
	private static final Logger logger = Logger.getLogger(AddIdentity.class);
	protected String testCaseName = "";
	private static String identity;
	public static void saveIdentityForUpdateIdentityVerification(String id) {
		identity=id;
	}
	
	public static String getIdentityForUpdateIdentityVerification() {
		return identity;
	}
	/**
	 * get current testcaseName
	 */
	@Override
	public String getTestName() {
		return testCaseName;
	}

	/**
	 * Data provider class provides test case list
	 * 
	 * @return object of data provider
	 */
	@DataProvider(name = "testcaselist")
	public Object[] getTestCaseList(ITestContext context) {
		String ymlFile = context.getCurrentXmlTest().getLocalParameters().get("ymlFile");
		logger.info("Started executing yml: "+ymlFile);
		return getYmlTestData(ymlFile);
	}

	/**
	 * Test method for OTP Generation execution
	 * 
	 * @param objTestParameters
	 * @param testScenario
	 * @param testcaseName
	 * @throws AuthenticationTestException
	 * @throws AdminTestException
	 */
	@Test(dataProvider = "testcaselist")
	public void test(TestCaseDTO testCaseDTO) throws AuthenticationTestException, AdminTestException {		
		testCaseName = testCaseDTO.getTestCaseName();
		addIdentity(testCaseDTO);
		
	}
	
	public void addIdentity(TestCaseDTO testCaseDTO) throws AuthenticationTestException, AdminTestException {
		String uin = JsonPrecondtion
				.getValueFromJson(RestClient.getRequestWithCookie(ApplnURI+"/v1/idgenerator/uin", MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, COOKIENAME, new KernelAuthentication().getTokenByRole(testCaseDTO.getRole())).asString(), "response.uin");
		DateFormat dateFormatter = new SimpleDateFormat("YYYYMMddHHmmss");
		Calendar cal = Calendar.getInstance();
		String timestampValue = dateFormatter.format(cal.getTime());
		String genRid = "27847" + RandomStringUtils.randomNumeric(10) + timestampValue;
		generatedRid=genRid;
		String inputJson =  getJsonFromTemplate(testCaseDTO.getInput(), testCaseDTO.getInputTemplate());
		inputJson = inputJson.replace("$UIN$", uin);
		inputJson = inputJson.replace("$RID$", genRid);
		Response response = postWithBodyAndCookie(ApplnURI + testCaseDTO.getEndPoint(), inputJson, COOKIENAME, testCaseDTO.getRole(), testCaseDTO.getTestCaseName());
		genertedUIN=uin;
		Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil
				.doJsonOutputValidation(response.asString(), getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate()));
		Reporter.log(ReportUtil.getOutputValiReport(ouputValid));
		if(testCaseDTO.getTestCaseName().contains("_Pos"))
			saveIdentityForUpdateIdentityVerification(JsonPrecondtion.getJsonValueFromJson(response.asString(), "request.identity"));
		Assert.assertEquals(OutputValidationUtil.publishOutputResult(ouputValid), true);
	}
	

	/**
	 * The method ser current test name to result
	 * 
	 * @param result
	 */
	@AfterMethod(alwaysRun = true)
	public void setResultTestName(ITestResult result) {
		try {
			Field method = TestResult.class.getDeclaredField("m_method");
			method.setAccessible(true);
			method.set(result, result.getMethod().clone());
			BaseTestMethod baseTestMethod = (BaseTestMethod) result.getMethod();
			Field f = baseTestMethod.getClass().getSuperclass().getDeclaredField("m_methodName");
			f.setAccessible(true);
			f.set(baseTestMethod, testCaseName);
		} catch (Exception e) {
			Reporter.log("Exception : " + e.getMessage());
		}
	}	
}