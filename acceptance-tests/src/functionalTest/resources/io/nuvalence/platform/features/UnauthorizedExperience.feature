Feature: Agent Experience

  Scenario: upload a document - unauthorized role
    Given a user with the following roles
      | role              |
      | dm:document-unauthorized-role |
    And the document upload endpoint /api/v1/documents
    When the file drivers_license.png is uploaded
    Then it should return 403
    
  Scenario: get file content - unauthorized role
    Given a user with the following roles
      | role              |
      | dm:document-uploader |
    And the file drivers_license.png has been uploaded to /api/v1/documents
    Given a user with the following roles
      | role              |
      | dm:document-unauthorized-role |
    When the file is downloaded it should return 404

  Scenario: get document metadata - unauthorized role
    Given a user with the following roles
      | role              |
      | dm:document-uploader |
    And the file drivers_license.png has been uploaded to /api/v1/documents
    Given a user with the following roles
      | role              |
      | dm:document-unauthorized-role |
    When the document metadata is retrieved it should return 404
