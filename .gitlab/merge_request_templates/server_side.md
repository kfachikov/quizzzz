Closes <!--- specify issue number here preceded with "#" symbol -->

# Checklist
<!---
Leave the "Particular" section out if your issue covered only a creation of some particular classes.
-->
Particular:
- [ ] Have you tested your code in all possible manners?
  - Postman - does the back-end endpoints and functionality works are desired
  - Using the *h2 database* console - reached on http://localhost:8080/h2-console in case the server is started on port `8080`

General:
- [ ] Do you follow all style conventions agreed on in the "Code Of Conduct" and the "Checkstyle"?
  - Classes, fields and variables naming conventions
  - Sufficient Javadoc explaining the achieved functionality
  - Sufficient test coverage - all possible outcomes from an endpoint should be tested
- [ ] Make sure you merge into branch `development`

<!---
The following section should be omitted if one's issue did not included the creationg of any additional "beans"
-->
## New Classes
<!---
Include any relevant information about the classes ("beans") you have created.
Briefly describe your design choices and show your long-term view of how it could be integrated in the application in the big picture.
-->
- [ ] Do you have sufficient test coverage for all methods declared and defined?


# Details
<!---
Elaborate on the task you had to implement, the desired behavior and the one you achieved. 
If something made you struggle, make sure you include the reason and the information sources that lead to the particular approach in here, so that the reviewers can easily get familiar.
-->

# How to test
<!---
Give a detailed summary of how your piece of code can be tested.
State explicitly once more the expected behavior of particular occasions and guide the reviewers on how to reach them and how to test them properly.
-->

[//]: <> (
Information should be inserted instead of the comments of the format "<!--- -->". 
If not removed, the comments would still preserve the information containing - for example, commented tasks are still counted by GitLAb, so make sure you delete them.
)
