package com.navbi.download;

public class DownloadLimitExceededException extends RuntimeException {

    public DownloadLimitExceededException(String message) {
        super(message);
    }
}
