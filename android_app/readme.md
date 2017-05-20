# Java files file structure

## Files used in current implementation

In a current implementation, the following java source files are being used:
- `IppRequest.java`
- `PostingService.java`
- `UnifiedActivity.java`
- `jLpr.java` - LPR library

## Legacy files and their pupose

We’ve intentionally left some of the legacy files in the repository for a different purposes. The files were not further developed significantly after we’ve discovered that the approach they provide is no longer valid for our goal.

- `DiscoveryActivity.java` and `MainActivity.java`: The purpose of these two files is to showcase the interface of an application before the UX feedback mentioned in the thesis
- `PostingService.java`: This is the a functional interface to communicate with a SafeQ over HTTP POST. If the SafeQ starts supporting receiving 3D jobs over HTTP POST, this implementation sohuld be capable to accomplish the job. The implementation also features sending a job over a standard LPR. Right now the service is configured to send a job over LPR. With a slight modification, HTTP could be enabled as well.

