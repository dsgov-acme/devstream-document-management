package io.nuvalence.ds4g.documentmanagement.service.model;

/** Key names used for storing document details in Cloud Storage object metadata. */
public enum DocumentMetadataKey {

    /**
     * ID of the user that uploaded a document.
     */
    UPLOADED_BY("UPLOADED-BY"),

    /**
     * Original file name of the uploaded document.
     */
    ORIGINAL_FILENAME("ORIGINAL-FILENAME");

    /**
     * Key where this metadata will be stored in a Cloud Storage object's metadata.
     */
    public final String gcpKey;

    DocumentMetadataKey(String gcpKey) {
        this.gcpKey = gcpKey;
    }
}
