# MPInspector
This repository contains the code for an analyze tool prototype for evaluating the security of IoT messaging protocols.

# Build Instructions

install the prerequisites: 

- Tamarin-Prover

  Follow the instruction given at Tamarin-Prover install page https://tamarin-prover.github.io/manual/book/002_installation.html

- Python 3.9

- JDK 1.8

- Node v12.16.1

- use pip to install Stanford Core NLP

  `pip install stanfordcorenlp`

# Usage

> mvn exec:java -D"exec.mainClass"="main.java.mpinspector.MQTTSemantics" -D"exec.arguments"=${platformtype}

>  Other platforms: mvn exec:java -Dexec.mainClass="main.java.mpinspector.MQTTSemantics"  -Dexec.arguments=${platformtype}


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

