# MPInspector
This repository contains the code for an analyze tool prototype for evaluating the security of IoT messaging protocols.

# Build Instructions

install the prerequisites: 

- Tamarin-Prover

  Follow the instruction given at Tamarin-Prover install page https://tamarin-prover.github.io/manual/book/002_installation.html

- Python 3.9

- JDK 1.8

- Node v12.16.1

- Maven v3.6.2

- use pip to install Stanford Core NLP

  `pip install stanfordcorenlp`

- compile MessageSemanticsExtraction
  ```
  cd ${path}$/MPInspector/MessageSemanticsExtraction
  mvn compile
  ```

- compile Interaction logic extraction
  ```
  cd ${path}$/MPInspector/MPlearner
  mvn package shade:shade -e
  cd target
  zip -d mplearner-0.0.1-SNAPSHOT.jar 'META-INF/.SF' 'META-INF/.RSA' 'META-INF/*SF'
  ```

# Usage

Fill in the configure file *MPInspector/build/server.properties*

**1. NLP-based semantics extraction**

NLPbasedsemanticsextraction.py automatically extract the parameter semantics based on the IoT platform documents in HTML format.

**Example:**

```
cd ${path}$/MPInspector
python MessageSemanticsExtraction/NLPbasedsemanticsextraction.py
```

**2. Traffic-based semantics extraction and the semantics assignment**

Run the maven project *MessageSemanticsExtraction* and the extracted semantics results are in the folder *traffic_analysis/${platformname}*.

**Example:**

```
cd ${path}$/MPInspector/MessageSemanticsExtraction
mvn exec:java -D"exec.mainClass"="main.java.mpinspector.MQTTSemantics"
```

**3. Interaction logic extraction**

Fill in the config file *serverjs.properties* in the folder *build* 
Load the MP adapter
```
cd ${path}$/MPInspector/Adapter/mqttjsadapter
node mclient.js
```

Perform parameter analysis
```
python ProcArg.py
```
ProcArg.py outputs the parameter validation results and fill in the configure file in *MPInspector/Adapter/mqttjsadapter/cache/${debug_result_yaml_save_path}$*, where ${debug_result_yaml_save_path}$ can be defined in *MPInspector/build/server.properties*.

Then, perform interaction logic extraction as follows:
```
cd ${path}$/MPInspector/build
java -jar ${path}$\MPInspector\mplearner\target\mplearner-0.0.1-SNAPSHOT.jar server.properties
```
The inferred state machine and excution log are outputed in the *MPInspector/build* directory.

Noted, the configure file for the interaction logic extraction can be manually defined to perform interaction logic extraction.

For using the adapter in javascript, change the host path in the configure file *MPInspector/build/server.properties*, and modify the host file taking the host files in directory *MPInspector/Adapter/mqttjsadapter/platforms/* as examples.

For using the adapter in java, modify the configure file *MPInspector/build/server.properties* taking the configure files in directory *MPInspector/MPlearner/example/* as examples.


**4. Translation**
To be updating 

# How to cite us
```
@inproceedings{wang2021mpinspector,
  title={MPInspector: A Systematic and Automatic Approach for Evaluating the Security of IoT Messaging Protocols},
  author={ Qinying Wang and Shouling Ji and Yuan Tian and Xuhong Zhang and Binbin Zhao and Yuhong Kan and Zhaowei Lin and Changting Lin and Shuiguang Deng and Alex X. Liu and Reheem Beyah},
  booktitle={30th $\{$USENIX$\}$ Security Symposium ($\{$USENIX$\}$ Security 21)},
  pages={},
  year={2021}
}
```

