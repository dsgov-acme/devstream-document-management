Feature: User Experience

  Scenario: upload a document - public user
    Given a user with the following roles
      | role              |
      | dm:document-uploader |
    And the document upload endpoint /api/v1/documents
    When the file drivers_license.png is uploaded
    Then it should return 202 and contain the JSON property document_id

  Scenario: Upload file with unsupported mimeType
    Given a user with the following roles
      | role              |
      | dm:document-uploader |
    And the document upload endpoint /api/v1/documents
    When the unsupported mime type file what_is_image_Processing.avif is uploaded
    Then it should return 415 and contain the JSON properties error-code,message

  Scenario: Upload file with unsupported octet-stream extension
    Given a user with the following roles
      | role              |
      | dm:document-uploader |
    And the document upload endpoint /api/v1/documents
    When the octet stream file drivers_license.png is uploaded
    Then it should return 415 and contain the JSON properties error-code,message

  Scenario: get file contents - public user
    Given a user with the following roles
      | role              |
      | dm:document-uploader |
    And the file drivers_license.png has been uploaded to /api/v1/documents
    When the file is downloaded
    Then it should be identical to uploaded file

  Scenario: get document metadata - public user
    Given a user with the following roles
      | role              |
      | dm:document-uploader |
    And the file drivers_license.png has been uploaded to /api/v1/documents
    When the document metadata is retrieved
    Then it should return 200 and contain the JSON properties id, filename, uploadedBy
