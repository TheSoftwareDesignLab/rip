# RIP
### 2018, Universidad de los Andes - The Software Design Lab

## Dependencies

To build the .JAR you must install Java 8 JDK and Gradle 4.5.1 

Gradle will install the following dependencies:
```
* JUnit (4.12)
* Apache Commons IO (2.6)
* Jackson-Core (2.9.4)
* Log4J (2.10.0)
```
## Installation

To build the project run the command:
```
gradle jarjar
```
The JAR file will be generated at
```
build/libs/RIP.jar
```
Set the following environment variables

```
APK_LOCATION  
AAPT_LOCATION
```
where APK_LOCATION corresponds to the path of the apk to be analyzed, and AAPT_LOCATION is where AAPT is located.

**Example:**

```
APK_LOCATION= /home/maria/Documentos/RIP/androidApps/Spunky2.apk	
AAPT_LOCATION= /home/maria/Android/Sdk/build-tools/27.0.3/aapt

```

## Running

Before running you must have an android emulator turned on or a device connected with the debugging option enabled.
To run an android emulator from the terminal you can use the command **emulator -list-avds** (under Android/sdk/tools/) to check the available devices. Then, you can turn on one device by running the **emulator -avd EMULATOR_NAME command**.

To run the jar file, go to the folder where it was generated at (build/libs). Then, run the command `java -jar RIP.jar` to start the execution. 
