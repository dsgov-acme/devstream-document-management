Feature: Agent Experience

  Scenario: upload a document - agent
    Given a user with the following roles
      | role              |
      | dm:document-reviewer |
    And the document upload endpoint /api/v1/documents
    When the file drivers_license.png is uploaded
    Then it should return 403

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

  Scenario: get file contents - agent
    Given a user with the following roles
      | role              |
      | dm:document-uploader |
    And the file drivers_license.png has been uploaded to /api/v1/documents
    Given a user with the following roles
      | role              |
      | dm:document-reviewer |
    When the file is downloaded
    Then it should be identical to uploaded file

  Scenario: get document metadata - agent
    Given a user with the following roles
      | role              |
      | dm:document-uploader |
    And the file drivers_license.png has been uploaded to /api/v1/documents
    Given a user with the following roles
      | role              |
      | dm:document-reviewer |
    When the document metadata is retrieved
    Then it should return 200 and contain the JSON properties id,filename, uploadedBy
