package com.tulipupdater.main;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Downloader extends  Application{

	private Parent createContent() {
		
	    ArrayList<String> version= new ArrayList<String>();
	    String urlnew="";
	    
	    // code to get url from uploader using restAPI
		RestTemplate restTemplate = new RestTemplate();
        String downloadurl = getProperties("downloadurl");
        System.out.println("downloadurl="+downloadurl);		
        Map<String, String> map = new HashMap<String, String>();
        map.put("id", "1");

        String resulturl = restTemplate.getForObject(downloadurl, String.class,map);

        System.out.println("Download URL in updater="+resulturl);		
     
        if(resulturl.contains(",")) {
        
	        String[] arr=resulturl.replace("{","").replace("}", "").split(",");
	    
		    for (String str : arr) {
		    	
		    	version.add(str.substring(0,str.indexOf(":")).replace("\"", ""));
		    	urlnew=urlnew.concat(str.substring(str.indexOf(":")+1,str.length()).replace("\"", "")+",");
			}
     
        }
        else if (!resulturl.replace("{","").replace("}", "").isEmpty()){
        	
        	String urlnew1=resulturl.replace("{","").replace("}", "");
        	urlnew=urlnew1.substring(urlnew1.indexOf(":")+2,urlnew1.length()).replace("\"", "");
        }
	  // by suchita
		
		VBox root = new VBox();
		root.setPrefSize(400, 60);
		
		TextField fieldUrl = new TextField();
		
		if(urlnew.contains(","))
		fieldUrl.setText(urlnew.substring(0, urlnew.lastIndexOf(",")));
		else
			fieldUrl.setText(urlnew);
		root.getChildren().addAll(fieldUrl);
		
		fieldUrl.setOnAction(event -> {
			
			Task<Void> task = new DownloadTask(fieldUrl.getText());
			String url = fieldUrl.getText();
			 
			 ProgressBar progressBar = new ProgressBar();
			 System.out.println("fileName="+url.length());	
				
	        	if(!url.isEmpty()) {
	        	
				progressBar.setPrefWidth(400);
				progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
				progressBar.progressProperty().bind(task.progressProperty());
				root.getChildren().add(progressBar);
	        	
	        	fieldUrl.clear();
	        	
	        }
	        else {
	        		progressBar.setVisible(false);
					fieldUrl.setText("No Update");
					Alert alert = new Alert(AlertType.ERROR);
					alert.initModality(Modality.WINDOW_MODAL);
					alert.setTitle("Tulip Updater");
					alert.setHeaderText("No update available");
					alert.show();	
					System.out.println("Successful=false");
					task.cancel();
	        }
			
			Thread thread = new Thread(task);
			thread.setDaemon(true);
			thread.start();
			
			
			task.setOnFailed(failedEvent->{
				System.out.println(" NO best");
				progressBar.setVisible(false);
				fieldUrl.setText("Error occured while Updating");

				Alert alert = new Alert(AlertType.ERROR);
				//alert.initOwner(root);
				alert.initModality(Modality.WINDOW_MODAL);
				alert.setTitle("Tulip Updater");
				alert.setHeaderText("Error occured while Updating");
				alert.show();	 
			
			});
			
			task.setOnSucceeded(successEvent -> {
				
				System.out.println("best");
				fieldUrl.setText("Downloaded Sucessfully");
				Alert alert = new Alert(AlertType.CONFIRMATION);
				
				alert.initModality(Modality.WINDOW_MODAL);
				alert.setTitle("Tulip Updater");
				alert.setHeaderText("Press Ok to proceed further");
				alert.setContentText("");

				Optional<ButtonType> result = alert.showAndWait();
				if (result.isPresent() || result.get() == ButtonType.OK) {
					System.out.println("User Pressed Yes");
			
					/* Backup the old application */
					boolean isApplicationBackuped=backupApplication();
					
					/* If application is backup successful then backup database */
					if (isApplicationBackuped) {
						//if dbbackup is success then copy the new Tulipjar into the folder
					boolean isdbBackuped=backupOldDB();	
						
					}else {
						alert = new Alert(AlertType.ERROR);
						
						alert.initModality(Modality.WINDOW_MODAL);
						alert.setTitle("Tulip Updater");
						alert.setHeaderText("Error Occured while taking Backup");
						alert.setContentText("");
					}

				} else {
					System.out.println("User Pressed No");
				}

			});
		});
	
		return root;
	}
	
	private boolean backupOldDB() {
		boolean isSuccesfullyBackup=false;
		InputStream fis = null;
		String oldApplicationFile = null;
		try {
			
			oldApplicationFile=getProperties("olddbfilepath");

			File directory = new File(oldApplicationFile);
			if (!directory.exists()) {
				directory.mkdir();

			}
			File source = new File(getProperties("backupdbpath"));
			File dest = new File(directory + "\\tulip_mic.mv.db");
			try {
				copyFileUsingStream(source, dest);
				
				isSuccesfullyBackup=true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return isSuccesfullyBackup;

	
	}

	private boolean backupApplication() {
		boolean isSuccesfullyBackup=false;
		InputStream fis = null;
		String oldApplicationFile = null;
		try {
			oldApplicationFile = getProperties("oldapplicationfilepath");

			File directory = new File(oldApplicationFile);
			if (!directory.exists()) {
				directory.mkdir();

			}
			File source = new File(getProperties("backupjarpath"));
			File dest = new File(directory + "\\TulipClient.jar");
			try {
				copyFileUsingStream(source, dest);
				
				isSuccesfullyBackup=true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return isSuccesfullyBackup;

	}
	public String getProperties(String property) {
		
		String propertyName="";
		InputStream fis = null;
		
		try {
			fis = getClass().getClassLoader().getResourceAsStream("tulipupdater.properties");

			Properties prop = new Properties();
			prop.load(fis);
			propertyName = prop.getProperty(property);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return propertyName;
	}
	private static void copyFileUsingStream(File source, File dest) throws IOException {
	    InputStream is = null;
	    OutputStream os = null;
	    try {
	        is = new FileInputStream(source);
	        os = new FileOutputStream(dest);
	        byte[] buffer = new byte[1024];
	        int length;
	        while ((length = is.read(buffer)) > 0) {
	            os.write(buffer, 0, length);
	        }
	    }catch(Exception e) {
	    	e.printStackTrace();
	    }finally {
	        is.close();
	        os.close();
	    }
	}
	private class DownloadTask extends Task<Void> {

		private String url;

		public DownloadTask(String url) {
			this.url = url;
		}

		@Override
		protected Void call() throws Exception {
			
		List<String> urlarr= new ArrayList<String>();
			
				if(url.contains(",")) {
					String[] arr=url.split(",");
					urlarr=Arrays.asList(arr);
					
				}
				else {
					urlarr.add(url);
				}
					
				for (String downloadFile : urlarr) {
					
					String fileName=downloadFile.substring(downloadFile.lastIndexOf("/")+1,downloadFile.length());
					String extension=downloadFile.substring(downloadFile.lastIndexOf("."),downloadFile.length());
					String file=getProperties("downloadfilepath")+fileName.substring(0,fileName.lastIndexOf("_"))+extension;
					
				URLConnection connection = new URL(downloadFile).openConnection();
				try (InputStream is = connection.getInputStream();
							OutputStream os = Files.newOutputStream(Paths.get(file))) {
						long nread = 0L;
		
						byte[] buf = new byte[8192];
						int n;
						while ((n = is.read(buf)) > 0) {
							os.write(buf, 0, n);
							nread += n;
							updateProgress(nread, 100);
							
						}
							}
					}
				
			return null;
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Tulip Updater");
		primaryStage.setResizable(false);
		primaryStage.setScene(new Scene(createContent()));
		
		primaryStage.show();

		
	}
	
	public static void main(String[] args) {
		launch(args);
	}

}
