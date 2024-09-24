package se.kth.models;

import java.util.Objects;

@lombok.Getter
@lombok.Setter
public class ErrorInfo {

    /**
     * The line position in the client file where the error occurred.
     */
    private String clientLinePosition;

    /**
     * The file path of the client file where the error occurred.
     */
    private String clientFilePath;

    /**
     * The error message describing the issue.
     */
    private String errorMessage;

    /**
     * Any additional information related to the error.
     */
    private String additionalInfo;

    /**
     * The content of the line in the client file where the error occurred.
     */
    private String clientLineContent;

    /**
     * The GitHub link to the client file.
     */
    private String clientGithubLink;

    /**
     * The line position of the error within the file.
     */
    private int errorLinePositionInFile;

    /**
     * The GitHub link to the error log.
     */
    private String errorLogGithubLink;


    public ErrorInfo(String clientLinePosition, String clientFilePath, String errorMessage, int errorLinePositionInFile,String additionalInfo) {
        this.clientLinePosition = clientLinePosition;
        this.clientFilePath = clientFilePath;
        this.errorMessage = errorMessage;
        this.clientLineContent = "";
        this.additionalInfo = additionalInfo;
        this.errorLinePositionInFile = errorLinePositionInFile;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ErrorInfo errorInfo = (ErrorInfo) o;

        if (!Objects.equals(clientLinePosition, errorInfo.clientLinePosition))
            return false;
        if (!Objects.equals(clientFilePath, errorInfo.clientFilePath))
            return false;
        return Objects.equals(errorMessage, errorInfo.errorMessage);
    }

    @Override
    public String toString() {
        return "ErrorInfo{" +
                "clientLinePosition='" + clientLinePosition + '\'' +
                ", clientFilePath='" + clientFilePath + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", additionalInfo='" + additionalInfo + '\'' +
                ", clientLineContent='" + clientLineContent + '\'' +
                ", clientGithubLink='" + clientGithubLink + '\'' +
                ", errorLinePositionInFile=" + errorLinePositionInFile +
                ", errorLogGithubLink='" + errorLogGithubLink + '\'' +
                '}';
    }
}
