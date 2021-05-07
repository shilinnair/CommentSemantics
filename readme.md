Feature Location Evaluation project based on Comment Semantics.
---------------------------------------------------------------
- It is a Maven based java project and uses Java 1.8.
- Open the project in eclipse, build the project and run Main.java to launch the application
- commentsemantics\Goldset is the gold set directory. Different queries, results and source files are present in it.
- Application has two modes. Goldset Evaluation Mode and 'Browse Project' mode.

     ##Goldset evalumation mode
	 Application runs in gold set evaluation mode when this option is checked from the UI. Gold set is present at commentsemantics\Goldset
	 directory. There are six sets of goldsets present under src, query and result folders. Predefined Queries are run on its source repository
	 and the results are validated aginst its result. Each resutls are recorded inside ./Result directory. Each query generate a result file
	 having a name of '[QueryNumber].txt'. After executing all queries a final result final is also generated 'FinalResult.txt' that has 
	 the evaluation matrices such as mAP, Recall, F-Score and mRR values.
	 
	 
	 ##Browse Project mode
	 This mode lets the user browse any project source and runs the custom query based on the UI options. 
	 Search results are stored inside the result directory with query as file name '[query].txt'.
	 Select various UI options to refine the search result and see how the results appears.
	 
- There is a mode in the UI to 'exclude commented code' from the search. If this is checked commented codes are exlcuded from the search.
  Excluded such codes from comments are saved in a file 'CommentsExcluded.txt' inside the resutl folder for review.


