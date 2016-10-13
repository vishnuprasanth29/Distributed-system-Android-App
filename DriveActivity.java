package com.Project258;
/**
 * Created by VISHNU PRASANTH on 3/22/2016.
 */
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DriveActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener {
	private static final String TAG = "DriveActivity";
	private static final int REQUEST_CODE_RESOLUTION = 3;

//	SharedPreferences prefs = this.getSharedPreferences(
//			"com.Project258", Context.MODE_PRIVATE);

	private DriveFolder parentFolder;
	private List<DriveFile> uploadedFileId = new ArrayList<DriveFile>();

	private GoogleApiClient mGoogleApiClient;
	private Button mBackupNow;
	private Button mDownloadNow;
	private ActionBar ab;


	// creates a file object for the Project258 folder //
	private File selected =  new File("sdcard/Project258/");      /// home directory
	private File[] files = selected.listFiles();

	private List<String> fileList = new ArrayList<String>();
	private List<String> fileNames = new ArrayList<String>();
	private List<String> fileMimes = new ArrayList<String>();
	public int filesDoneNotifyCount = 1;
	public int filesTotalNotifyCount;

	public NotificationCompat.Builder mBuilder;
	public NotificationManager mNotifyManager;
	public int id = 1;

//	SharedPreferences prefs = this.getSharedPreferences(
//			"com.Project258", Context.MODE_PRIVATE);

	@Override
	protected void onCreate(Bundle savedInstanceState){
		Log.i(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.drive);


		// backup now button initialization //
		mBackupNow = (Button)findViewById(R.id.driveBackupNow);
		mBackupNow.setEnabled(true);
		mBackupNow.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				onDriveBackupNow();
			}
		});
		Log.i(TAG, "done with onCreate()");

		////// download button to start download from google drive ///////
//		mDownloadNow = (Button)findViewById(R.id.driveDownloadNow);
//		mDownloadNow.setEnabled(true);
//		mDownloadNow.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//				downloadFiles();
//			}
//		});

		// notfication initialization, thrown when google drive upload is completed //
		mBuilder = new NotificationCompat.Builder(this);
		mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		ab = getActionBar();
		ab.setTitle("Project258: Backup Google Drive");
		ab.setSubtitle("Not Connected");

		// initalize the google drive api object //
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addApi(Drive.API)
				.addScope(Drive.SCOPE_FILE)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.build();

		// get the account name (email) using account manager //
		String accountName = getAccountName();
		// set ui element text box for account name
//		mDriveAccountStatus.setText(accountName);
		ab.setSubtitle(accountName);

		// connect to google drive server //
		//mGoogleApiClient.connect();

	}

	// make a list of all the files under the Project258 folder //
	public void populateFiles() {
		try {
			fileList.clear();
			//String mimeType = getContentResolver().getType(returnUri);
			//Uri uri;
			for (File file : files) {
				fileList.add(file.getPath());
				Log.i(TAG, "File Path:" + file.getPath());
				fileNames.add(file.getName());
				Log.i(TAG, "File Name:" + file.getName());
				fileMimes.add(getMimeType(file.getAbsolutePath()));
				Log.i(TAG, "File Mime Type: " + getMimeType(file.getAbsolutePath()));
				//uri = Uri.fromFile(file);
				//fileMimes.add(getContentResolver().getType(uri));
				//Log.i(TAG, "File Mime Type" + getContentResolver().getType(uri));
			}
			filesTotalNotifyCount = files.length;
		}catch (NullPointerException e){
			e.printStackTrace();
			Context context = getApplicationContext();
			CharSequence text = "No Files to upload";
			int duration = Toast.LENGTH_LONG;
			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
		}
	}


	/**
	 * Create a new file and save it to Drive.
	 */

	@Override
	protected void onResume() {
		Log.v(TAG, "entering onResume");
		super.onResume();
		populateFiles();
		//mGoogleApiClient.connect();

		if (mGoogleApiClient == null) {
			// Create the API client and bind it to an instance variable.
			// We use this instance as the callback for connection and connection
			// failures.
			// Since no account name is passed, the user is prompted to choose.
			mGoogleApiClient = new GoogleApiClient.Builder(this)
					.addApi(Drive.API)
					.addScope(Drive.SCOPE_FILE)
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this)
					.build();
		}
	}

	protected void onDriveBackupNow() {
		Log.v(TAG, "onDriveBackupNow");
		mGoogleApiClient.connect();
//		mDriveConnectionStatus.setText("Connected");
		Log.v(TAG, "exiting onDriveBackupNow");

	}


	@Override
	protected void onPause() {
		Log.v(TAG, "onPause()");
		if (mGoogleApiClient != null) {
			mGoogleApiClient.disconnect();
//			mDriveConnectionStatus.setText("Disconnected");
		}
		super.onPause();
	}



	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// Called whenever the API client fails to connect.
		Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
//		mDriveConnectionStatus.setText("Connection Failed");

		if (!result.hasResolution()) {
			// show the localized error dialog.
			GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
			return;
		}
		// The failure has a resolution. Resolve it.
		// Called typically when the app is not yet authorized, and an
		// authorization
		// dialog is displayed to the user.
		try {
			result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
		} catch (IntentSender.SendIntentException e) {
			Log.e(TAG, "Exception while starting resolution activity", e);
		}
	}

	// get the account name of the user using android account manager ////
	private String getAccountName() {

		String accountName = null;

		AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
		Account[] list = manager.getAccounts();
		for (Account account : list) {
			if (account.type.equalsIgnoreCase("com.google")) {
				accountName = account.name;
				break;
			}
		}
		return accountName;
	}

	@Override
	public void onConnected(Bundle connectionHint) {
	   // super.onConnected(connectionHint);
		// create new contents resource



		///// create project directory
		if(sFolderId==null) {
			createFolder();
		}
//		else{
//			DriveId folderId = sFolderId;
//			DriveFolder folder = Drive.DriveApi.getFolder(mGoogleApiClient, folderId);
//			folder.getMetadata(mGoogleApiClient).setResultCallback(metadataRetrievedCallback);
//		}
		/// sync files to project directory
		Drive.DriveApi.newDriveContents(getGoogleApiClient())
				.setResultCallback(driveContentsCallback);
	}

	final private ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback = new
			ResultCallback<DriveApi.DriveContentsResult>() {
				@Override
				public void onResult(DriveApi.DriveContentsResult result) {
					if (!result.getStatus().isSuccess()) {
						Log.i(TAG, "Error while trying to create new file contents");
						return;
					}


					// notification constructs for showing upload complete ///
					mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					mBuilder = new NotificationCompat.Builder(getApplicationContext());
					mBuilder.setContentTitle("Project258:Sync Files to Google Drive")
							.setContentText("File " + filesDoneNotifyCount + " of " + filesTotalNotifyCount)
							.setSmallIcon(R.drawable.ic_tab_drive);

					// Perform file upload off the UI thread.//
					new Thread() {
						@Override
						public void run() {
							try {
								int fileDone = 0;
								for (int i = 0; i < files.length; i++, fileDone++) {
									mBuilder.setProgress(100, i, false);
									mNotifyManager.notify(id, mBuilder.build());
									final String title = fileNames.get(i);
									final String mimeType = fileMimes.get(i);
									final int locator = i;
									filesDoneNotifyCount = fileDone;
									parentFolder = Drive.DriveApi.getFolder(getGoogleApiClient(), sFolderId);
									Log.v(TAG, "UploadThread: Files prepared " + fileDone + " of " + files.length);
									//void createFile(DriveFolder pFldr, final String titl, final String mime, final File file) {
									DriveId dId = null;
									if (mGoogleApiClient != null && mGoogleApiClient.isConnected() && title != null && mimeType != null && files[i] != null)
										try {

											final DriveFolder parent = parentFolder != null ? parentFolder : Drive.DriveApi.getRootFolder(getGoogleApiClient());
											if (parentFolder == null) return; //----------------->>>
											Log.v(TAG, "Upload started for file" + title);

											/// prepate the file contents in DriveContents type for upload ////
											Drive.DriveApi.newDriveContents(mGoogleApiClient).setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

												@Override
												public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
													DriveContents cont = driveContentsResult != null && driveContentsResult.getStatus().isSuccess() ?
															driveContentsResult.getDriveContents() : null;
													if (cont != null) try {
														OutputStream oos = cont.getOutputStream();
														if (oos != null) try {
															InputStream is = new FileInputStream(files[locator]);
															byte[] buf = new byte[4096];
															int c;
															while ((c = is.read(buf, 0, buf.length)) > 0) {
																oos.write(buf, 0, c);
																oos.flush();
															}
														} finally {
															oos.close();
														}


														// set the metadata for sending the file to google drive server ////
														MetadataChangeSet meta = new MetadataChangeSet.Builder().setTitle(title).setMimeType(mimeType).build();

														parent.createFile(mGoogleApiClient, meta, cont).setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
															@Override
															public void onResult(DriveFolder.DriveFileResult driveFileResult) {
																DriveFile dFil = driveFileResult != null && driveFileResult.getStatus().isSuccess() ?
																		driveFileResult.getDriveFile() : null;
																if (dFil != null) {
																	// save dFil, driveID returned after successful file upload
																	Log.v(TAG,"File upload DriveID: " + dFil);
																	uploadedFileId.add(locator,dFil);
																} else {
																	// handle error
																	Log.v(TAG,"File upload Failed: " + title);
																}
															}
														});
													} catch (Exception e) {
														e.printStackTrace();
													}
												}
											});
											Log.v(TAG, "File upload completed for:" + title);
										} catch (Exception e) {
											e.printStackTrace();
										}
								}
								mBuilder.setContentText("Upload complete")
										// Removes the progress bar
										.setProgress(0, 0, false);
								mNotifyManager.notify(id, mBuilder.build());
							}catch (Exception e){
								e.printStackTrace();
							}
						}
					}.start();
					Log.v(TAG,"Files Uploaded Successfully");
					Context context = getApplicationContext();
					CharSequence text = "Google Drive Upload Complete";
					int duration = Toast.LENGTH_LONG;
					Toast toast = Toast.makeText(context, text, duration);
					toast.show();
				}
			};


	// check if the google drive connection is suspended ////
	@Override
	public void onConnectionSuspended(int cause) {
		Log.i(TAG, "GoogleApiClient connection suspended");
//		mDriveConnectionStatus.setText("Suspended");
		mBackupNow.setEnabled(false);

	}

	//// method to find the mime type of the file /////
	public static String getMimeType(String url) {
		String type = null;
		String extension = MimeTypeMap.getFileExtensionFromUrl(url);
		if (extension != null) {
			type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		}
		return type;
	}


	/// getter for googleapiclient object///
    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

	/// method to create folder on google drive /////
	public void createFolder(){
		MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
				.setTitle("Project258").build();
		Drive.DriveApi.getRootFolder(getGoogleApiClient()).createFolder(getGoogleApiClient(), changeSet).setResultCallback(folderCreatedCallback);
	}

	private DriveId sFolderId = null;

	/// google drive call back for folder creation /////
	ResultCallback<DriveFolder.DriveFolderResult> folderCreatedCallback = new
			ResultCallback<DriveFolder.DriveFolderResult>() {
				@Override
				public void onResult(DriveFolder.DriveFolderResult result) {
					if (!result.getStatus().isSuccess()) {
						Log.v(TAG,"Error while trying to create the folder");
						return;
					}
					Log.v(TAG,"Created a folder: " + result.getDriveFolder().getDriveId());
					sFolderId = result.getDriveFolder().getDriveId();
					//preferences.edit().putString(encodeToString(sFolderId));

				}
			};

	// check if folder has been trashed ///
	final private ResultCallback<DriveResource.MetadataResult> metadataRetrievedCallback = new
			ResultCallback<DriveResource.MetadataResult>() {
				@Override
				public void onResult(DriveResource.MetadataResult result) {
					if (!result.getStatus().isSuccess()) {
						Log.v(TAG, "Problem while trying to fetch metadata.");
						return;
					}

					Metadata metadata = result.getMetadata();
					if(metadata.isTrashed()){
						Log.v(TAG, "Folder is trashed");
						createFolder();
					}else{
						Log.v(TAG, "Folder is not trashed");
					}

				}
			};

//	///////////////////// download file code goes here /////
//	public NotificationCompat.Builder mBuildDownloader;
//	public NotificationManager mNotifyDownloader;
//	public void downloadFiles() {
//		filesDoneNotifyCount = 1;
//		mNotifyDownloader = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//		mBuildDownloader = new NotificationCompat.Builder(getApplicationContext());
//		mBuildDownloader.setContentTitle("Project258:Download Files from Google Drive")
//				.setContentText("File " + filesDoneNotifyCount + " of " + filesTotalNotifyCount + "Downloaded")
//				.setSmallIcon(R.drawable.ic_tab_drive);
//
////		mProgressBar.setProgress(0);
//		mBuildDownloader.setProgress(100, i, false);
//		mNotifyDownloader.notify(id, mBuildDownloader.build());
//
//		for (int i = 0; uploadedFileId.length; i++) {
//
//			DriveFile file = uploadedFileId.get(i);
//			file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null)
//					.setResultCallback(contentsOpenedCallback);
//
//			new Thread() {
//				@Override
//				public void run() {
//					for (int i = 0; i < uploadedFileId.size(); i++) {
//
//						DriveFile driveFile = uploadedFileId.get(i);
//						driveFile.open(getGoogleApiClient(), DriveFile.MODE_READ_ONLY, listener)
//								.setResultCallback(driveContentsCallback);
//						mSelectedFileDriveId = null;
//
//					}
//				}
//			}.start();
//			mBuildDownloader.setContentText("Download Complete")
//					// Removes the progress bar
//					.setProgress(0, 0, false);
//			mNotifyDownloader.notify(id, mBuildDownloader.build());
//		}
//	}
//
//


}

