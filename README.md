# MFix: A Tool for the Automated Repair of Mobile Friendly Problems in Web Pages

Mobile devices have become a primary means of accessing the Internet. Unfortunately, many websites are not designed to be mobile friendly. This results in problems, such as unreadable text, cluttered navigation, and content over owing a device’s viewport; all of which can lead to a frustrating and poor user experience. Existing techniques are limited in helping developers repair these mobile friendly problems. To address this limitation of prior work, we designed a novel automated approach, *MFix*, for repairing mobile friendly problems in web pages. MFix builds graph-based models of the layout of a web page and uses constraints encoded by these graphs to  nd patches that can improve mobile friendliness while minimizing layout disruption. More algorithmic details of MFix can be found in our paper:

```
Automated Repair of Mobile Friendly Problems in Web Pages
Sonal Mahajan, Negarsadat Abolhassani, Phil McMinn, William G. J. Halfond
In Proceedings of the 40th International Conference on Software Engineering (ICSE). May 2018. Acceptance rate: 20%. (To Appear) 
```

## How to run MFix?
1. **Inputs:** Run  [TestMFix.java](https://github.com/USC-SQL/mfix/blob/master/src/test/java/eval/TestMFix.java) from the mfix project by passing the following inputs as Strings:<br />
	(a) URL of the page under test (PUT) hosted on a public DNS <br />
	(b) File system location of the PUT<br />

2. **Output:** The output produced by MFix can be found in the parent folder of the location provided in input (b):<br />
	(a) log.txt: stores the detailed log information of one run of the subject. A summary of the results can be found at the end of this file.<br />
	(b) index-fixed-*.html: Modified PUT with the repair applied.
	
2. **Configuration:**
	(a) Update "KEY_PAIR_PATH" in [Constants.java](https://github.com/USC-SQL/mfix/blob/master/src/main/java/mfix/Constants.java) with the Amazon Web Services (AWS) public key pair path.
	
## Evaluation Data
#### Subjects: 
The 38 real-world web pages used in the evaluation of MFix can be found [here](https://github.com/USC-SQL/mfix/tree/master/ICSE_paper_data/subjects).

## Questions
In case of any questions you can email at spmahaja [at] usc {dot} edu