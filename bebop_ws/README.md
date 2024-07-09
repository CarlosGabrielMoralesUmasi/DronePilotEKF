# UCSP Drone Project


This project is an Android application that integrates DJI SDK functionalities for drone control. It allows interaction with DJI drones and performs various flight control and data recording operations.

## Requirements

Make sure you have the following requirements installed:

1. **Java 11:** Download and install [Java 11](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html).

2. **Project Packages:**

   - **Package Name:** `com.dji.sdk.sample`
   - **Google Services:** Ensure you have the `google-services.json` file configured for your project.

   ```json
   # google-services.json
   {
     "project_info": {
       "project_number": "115206810105",
       "firebase_url": "https://dronucsp-default-rtdb.firebaseio.com",
       "project_id": "dronucsp",
       "storage_bucket": "dronucsp.appspot.com"
     },
     "client": [
       {
         "client_info": {
           "mobilesdk_app_id": "1:115206810105:android:a466133d7878a2fefff528",
           "android_client_info": {
             "package_name": "com.dji.sdk.sample"
           }
         },
         "oauth_client": [],
         "api_key": [
           {
             "current_key": "YOUR_API_KEY_HERE"
           }
         ],
         "services": {
           "appinvite_service": {
             "other_platform_oauth_client": []
           }
         }
       }
     ],
     "configuration_version": "1"
   }
- Replace "YOUR_API_KEY_HERE" with your personal API key from your FIREBASE project.
- Replace "YOUR_API_KEY_HERE" with your personal API key obtained from the [DJI Developers](https://developer.dji.com/user/apps/#all) page.

   **APP INFORMATION**
   - **SDK Type:** Mobile SDK
   - **App Name:** ucsp_dron
   - **Software Platform:** Android
   - **Package Name:** **com.dji.sdk.sample**
   - **App Key:** 3b22aa_tiene_esta_forma_e8d5c
   - **Category:** Film shooting
   - **Description:** Project where the package with the name "com.dji.sdk.sample" is mandatory to have the complete code tools and the "personal key" APP KEY, which must be created only for a DJI DEVELOPERS account.


## Execution Instructions
1. Clone this repository to your development environment.

2. Open the project in Android Studio.

3. Run the application on an Android emulator or device.
   
## NOTES
It is not recommended to update the Gradle version to 8.0 due to unknown issues. Keep the version specified in the build.gradle file.

## Contact
Yerson Sanchez Y. (Project Head)
//Carlos Morales U.
//Cristhian Ocola P.
//Erick Yari M.

This `README.md` provides information about the project, requirements, execution instructions, and contact for possible inquiries. Be sure to customize it according to the specific needs of your project.
