package org.icicibank.google.analytics;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.icicibank.dao.data.DataPorter;
import org.icicibank.google.calander.DateUtil;
import org.icicibank.google.log.GaLogFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
//import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Joiner;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.Analytics.Data.Ga.Get;
import com.google.api.services.analytics.AnalyticsScopes;
import com.google.api.services.analytics.model.Account;
import com.google.api.services.analytics.model.Accounts;
//import com.google.api.services.analytics.model.CustomDimension;
import com.google.api.services.analytics.model.GaData;
import com.google.api.services.analytics.model.GaData.ProfileInfo;
import com.google.api.services.analytics.model.GaData.Query;
import com.google.api.services.analytics.model.Profiles;
import com.google.api.services.analytics.model.UnsampledReport;
import com.google.api.services.analytics.model.Webproperties;
import com.google.api.services.analytics.model.Webproperty;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

/**
 * A simple example of how to access the Google Analytics API using a service
 * account.
 */
public class GADataPorter {

	public static Logger LOGGER = GaLogFactory.getGALogger();
	private String applicationName;
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static final java.io.File DATA_STORE_DIR = new java.io.File("D:/data_GD", "drive-java-quickstart");
	

	/** Global instance of the {@link FileDataStoreFactory}. */
	private static FileDataStoreFactory DATA_STORE_FACTORY;
	private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE);

	/** Global instance of the HTTP transport. */
	private static HttpTransport HTTP_TRANSPORT;
	private String keyFileLocation;
	private String serviceAccountEmail;

	private String toDate;
	private String fromDate;
	private int toTRows;
	private PropertiesConfiguration properties;
	private Drive drive;
    private DataPorter dataPersister;
    
    private String dimesions;
    private String metrics;
    private String sortKey;
    private String filters;
    private String fileName;
	

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getDimesions() {
		return dimesions;
	}

	public void setDimesions(String dimesions) {
		this.dimesions = dimesions;
	}

	public String getMetrics() {
		return metrics;
	}

	public void setMetrics(String metrics) {
		this.metrics = metrics;
	}

	public String getSortKey() {
		return sortKey;
	}

	public void setSortKey(String sortKey) {
		this.sortKey = sortKey;
	}

	public String getFilters() {
		return filters;
	}

	public void setFilters(String filters) {
		this.filters = filters;
	}

	public DataPorter getDataPersister() {
		return dataPersister;
	}

	public void setDataPersister(DataPorter dataPersister) {
		this.dataPersister = dataPersister;
	}

	public Drive getDrive() {
		return drive;
	}

	public void setDrive(Drive drive) {
		this.drive = drive;
	}

	public String getToDate() {
		return toDate;
	}

	public void setToDate(String toDate) {
		this.toDate = toDate;
	}

	public String getFromDate() {
		return fromDate;
	}

	public void setFromDate(String fromDate) {
		this.fromDate = fromDate;
	}

	public int getTotrows() {
		return toTRows;
	}

	public void setTotrows(int totrows) {
		this.toTRows = totrows;
	}

	public int getToTRows() {
		return toTRows;
	}

	public void setToTRows(int toTRows) {
		this.toTRows = toTRows;
	}

	public PropertiesConfiguration getProperties() {
		return properties;
	}

	public void setProperties(PropertiesConfiguration properties) {
		this.properties = properties;
	}

	//By Ashru
	public void init(int attempts) throws Exception{
		int maxFailures = properties.getInt("GA_Max_Host_Con_Failures");
		try {
			applicationName = properties.getString("applicationName");
			keyFileLocation = properties.getString("keyFileLocation");
			serviceAccountEmail = properties.getString("serviceAccountEmail");
			LOGGER.debug("applicationName" + applicationName);

			LOGGER.debug("keyFileLocation" + keyFileLocation);
			LOGGER.debug("serviceAccountEmail" + serviceAccountEmail);
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(
						java.security.cert.X509Certificate[] certs,
						String authType) {
				}

				public void checkServerTrusted(
						java.security.cert.X509Certificate[] certs,
						String authType) {
				}
			} };

			try {
				SSLContext sc = SSLContext.getInstance(properties
						.getString("securityType"));
				sc.init(null, trustAllCerts, new java.security.SecureRandom());
				HttpsURLConnection.setDefaultSSLSocketFactory(sc
						.getSocketFactory());
			} catch (Exception e) {
			}

			Analytics analytics = initializeAnalytics();
			int si = 1;
			int lp = 1;
			String profile = getFirstProfileId(analytics);			
			//GaData gaData = getGAData(analytics, profile, si, command, 1);
			GaData gaData = getGAData(analytics, profile, si, 1);
			LOGGER.info("Dimensions from Query:- "+gaData.getQuery().getDimensions());
			LOGGER.info("Sample Report Count- "+gaData.getTotalResults());
			
			if(gaData.getContainsSampledData()&&(gaData.getQuery().getDimensions().split(",").length<5)){
				LOGGER.info("Initating UN-Sampled Report");
				
				//String documentId = unsampleResults(analytics,command, gaData.getQuery(),gaData.getProfileInfo(),1);
				String documentId = unsampleResults(analytics, gaData.getQuery(),gaData.getProfileInfo(),1);
				
				Drive googleDriveService = initializeDrive(drive, gaData.getProfileInfo().getProfileId(),1);
				
				String outputFilePath = fileName;//properties.getString("GA_DOWNLOAD_PATH")+"GA_"+ fileName +"_"+fromDate+ ".csv";
				
				dataPersister.exportDataFromGDriveToFile(googleDriveService, documentId, outputFilePath);
				
				File fileCheck = new File(outputFilePath);
				
				while(fileCheck.exists()==false&&fileCheck.canWrite()==false){
					TimeUnit.SECONDS.sleep(properties.getInt("SLEEP_TIME_SECONDS"));					
				  }				
			}
			else if(gaData.getQuery().getDimensions().split(",").length>=5){
				LOGGER.info("Initating Sampled Report");
				String outputFilePath = fileName;//properties.getString("GA_DOWNLOAD_PATH")+"GA_"+ fileName +"_"+fromDate;
				dataPersister.exportSampledDataToFile(gaData, outputFilePath);
			}
			else{
				LOGGER.info("Error retriving Unsampled data from web");
				
			}			

		} 
		catch (Exception e) {
			if (attempts <= maxFailures) {
				LOGGER.error(e.getMessage() + " but trying again " + attempts
						+ "/3 (Maximum Attempts)");
				e.printStackTrace();
				attempts = attempts + 1;
				init(attempts);
				
			} 
			else{
				e.printStackTrace();
				LOGGER.error("Received Exception While Writing data to file:"
						+ e.getMessage());
				throw e;
			}
			
		}
	}
	
	
	private Analytics initializeAnalytics() throws Exception {
		// Initializes an authorized analytics service object.

		// Construct a GoogleCredential object with the service account email
		// and p12 file downloaded from the developer console.
		LOGGER.debug("serviceAccountEmail" + serviceAccountEmail);
		LOGGER.debug("keyFileLocation" + keyFileLocation);
		LOGGER.debug("applicationName" + applicationName);
		HttpTransport httpTransport = GoogleNetHttpTransport
				.newTrustedTransport();
		GoogleCredential credential = new GoogleCredential.Builder()
				.setTransport(httpTransport)
				.setJsonFactory(JSON_FACTORY)
				.setServiceAccountId(serviceAccountEmail)
				.setServiceAccountPrivateKeyFromP12File(
						new File(keyFileLocation))
				.setServiceAccountScopes(AnalyticsScopes.all()).build();
		// Construct the Analytics service object.
		return new Analytics.Builder(httpTransport, JSON_FACTORY, credential)
				.setApplicationName(applicationName).build();
	}

	private static String getFirstProfileId(Analytics analytics)
			throws IOException {
		// Get the first view (profile) ID for the authorized user.
		String profileId = null;

		// Query for the list of all accounts associated with the service
		// account.
		Accounts accounts = analytics.management().accounts().list().execute();
		
		LOGGER.info("Accounts :: "+accounts.getItems().size());
		for(Account acc: accounts.getItems()){
			LOGGER.info(acc.getId());
		}

		if (accounts.getItems().isEmpty()) {
			System.err.println("No accounts found");
		} else {
			String firstAccountId = accounts.getItems().get(0).getId();

			// Query for the list of properties associated with the first
			// account.
			Webproperties properties = analytics.management().webproperties()
					.list(firstAccountId).execute();
			
			LOGGER.info("Profiles :: "+properties.getItems().size());
			for(Webproperty web : properties.getItems()){
				LOGGER.info("Profile :: "+web.getId());
			}

			if (properties.getItems().isEmpty()) {
				System.err.println("No Webproperties found");
			} else {
				String firstWebpropertyId = properties.getItems().get(0)
						.getId();

				// Query for the list views (profiles) associated with the
				// property.
				Profiles profiles = analytics.management().profiles()
						.list(firstAccountId, firstWebpropertyId).execute();

				if (profiles.getItems().isEmpty()) {
					System.err.println("No views (profiles) found");
				} else {
					// Return the first (view) profile associated with the
					// property.
					profileId = profiles.getItems().get(0).getId();
				}
			}
		}
		return profileId;
	}


	private String buildProperty(String command, String type) {
		StringBuilder returnStr = new StringBuilder("GA_").append(command);
		if (StringUtils.isEmpty(command)) {
			returnStr.append(type);
		}
		if (!StringUtils.isEmpty(type)) {
			returnStr.append("_").append(type);
		}
		return returnStr.toString();
	}

	//By Ashru
	private GaData getGAData(Analytics analytics, String profileId,int si,int attempts) throws Exception{
		int maxFailures = properties.getInt("GA_Max_Host_Con_Failures");
		LOGGER.debug("Received dimensionsGA:" + dimesions);
		LOGGER.debug("Received filtersGA:" + filters);
		GaData gaData = null;
		Get apiQuery = analytics
					   .data()
					   .ga()
					   .get("ga:" + profileId,
							 DateUtil.getFormattedDate(fromDate, "yyyy-MM-dd","yyyy-MM-dd"), // Start // date.
							 DateUtil.getFormattedDate(toDate, "yyyy-MM-dd","yyyy-MM-dd"), // End date.
							 metrics/*"ga:totalEvents"*/)	// Metrics
				.setDimensions(dimesions)
				//.setMetrics(metrics)
				.setSort(sortKey)
				.setFilters(filters)
				.setSamplingLevel("HIGHER_PRECISION")
				.setStartIndex(si).setMaxResults(100000);
		
		try {

			gaData = apiQuery.execute();
			
			toTRows = gaData.getTotalResults();
		} catch (SSLHandshakeException e) {
			// Catch API specific errors.
			// e.printStackTrace();
			if (attempts <= maxFailures) {
				LOGGER.error(e.getMessage() + " but trying again " + attempts
						+ "/3 (Maximum Attempts)");
				e.printStackTrace();
				attempts = attempts + 1;
				gaData = getGAData(analytics, profileId, si,attempts);
				
			} else {
				LOGGER.error(e.getMessage()
						+ " Breached maximum failed attempts " + attempts
						+ "/3 (Maximum Attempts)");
				throw e;
			}

		} 

		return gaData;
	}
		
	//By Ashru
	public String unsampleResults(Analytics analytics, Query query,ProfileInfo profileInfo, int attempts) throws Exception {

		UnsampledReport createdReport = new UnsampledReport();

				LOGGER.debug("query.getStartDate():" + query.getStartDate());
				LOGGER.debug("query.getEndDate():" + query.getEndDate());

				LOGGER.info("Dimensions in Unsampled"+query.getDimensions());
				LOGGER.info("Metrics in Unsampled: "+Joiner.on(',').join(query.getMetrics()));
				LOGGER.info("Filters in Unsampled"+query.getFilters());
				LOGGER.info("Start Date in Unsampled"+query.getStartDate());
				LOGGER.info("End Date in Unsampled"+query.getEndDate());
							
				UnsampledReport report = new UnsampledReport()
						.setDimensions(query.getDimensions())
						.setMetrics(Joiner.on(',').join(query.getMetrics()))
						.setStartDate(query.getStartDate())
						.setEndDate(query.getStartDate())
						.setFilters(query.getFilters())
						.setDownloadType("GOOGLE_DRIVE")
						.setTitle("GA_" + query.getStartDate());

				System.out.println("Report info " + report);
				
				System.out.println("Profile info " + profileInfo);

				com.google.api.services.analytics.Analytics.Management.UnsampledReports.Insert insertRequest = analytics
						.management()
						.unsampledReports()
						.insert(profileInfo.getAccountId(),
								profileInfo.getWebPropertyId(),
								profileInfo.getProfileId(), report);

				createdReport = insertRequest.execute();
				LOGGER.debug("==============drive Download Detils========:"+ createdReport.getDriveDownloadDetails());

				LOGGER.debug("==acc==" + profileInfo.getAccountId() + "==profile=="
						+ profileInfo.getProfileId() + "==id==" + createdReport.getId()
						+ "====" + profileInfo.getWebPropertyId());
				
				com.google.api.services.analytics.Analytics.Management.UnsampledReports.Get get = analytics
						.management()
						.unsampledReports()
						.get(profileInfo.getAccountId(),
								profileInfo.getWebPropertyId(),
								profileInfo.getProfileId(), createdReport.getId());
				createdReport = get.execute();
				while (!createdReport.getStatus().equals("COMPLETED")) {
					LOGGER.debug(".....WAITING FOR REPORT GENEREATATION TO BE COMPLETED....REPORT GENERATION IS IN PROGRESS....");

					TimeUnit.MINUTES.sleep(properties.getInt("SLEEP_TIME_MINUTES"));//to avoid un-wanted GA requests
					createdReport = get.execute();
					LOGGER.debug("===========Sleep Completed============");
				}

				LOGGER.debug("=========Completed=====drive Download Detils========:"
						+ createdReport.getDriveDownloadDetails());
				String fileId = createdReport.getDriveDownloadDetails().getDocumentId();
				return fileId;
	}
	
	private Drive initializeDrive(Drive drive, String profileId, int attempts)
			throws GeneralSecurityException, IOException {
		int maxFailures = properties.getInt("GA_Max_Host_Con_Failures");
		Drive driveService = null;
		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
			// SCOPES.add("https://www.googleapis.com/auth/admin.directory.user");

			if (null == drive) {
				GoogleCredential credential = new GoogleCredential.Builder()
						.setTransport(HTTP_TRANSPORT)
						.setJsonFactory(JSON_FACTORY)
						.setServiceAccountId(serviceAccountEmail)
						.setServiceAccountPrivateKeyFromP12File(
								new File(keyFileLocation))
						.setServiceAccountScopes(SCOPES).build();
				driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY,
						credential).setApplicationName(applicationName).build();
				
			}
		} catch (Throwable e) {
			
			if (attempts <= maxFailures) {
				LOGGER.error(e.getMessage() + " but trying again " + attempts
						+ "/3 (Maximum Attempts)");
				e.printStackTrace();
				attempts = attempts + 1;
				driveService = initializeDrive(drive, profileId, attempts);
				
			} else {
				LOGGER.error(e.getMessage()
						+ " Breached maximum failed attempts " + attempts
						+ "/3 (Maximum Attempts)");
				throw e;
			}
			e.printStackTrace();
			LOGGER.error("Unable to initialize Drive:" + e.getMessage());
			
			//System.exit(1);

		}

		return driveService;
	}
	
}
