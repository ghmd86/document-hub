As we have completed the document-hub service poc now we need to build the
service that will be responsible to generate the document when a print request 
comes in. The document generation vendor we are going to use is SmartComm. 
- We have the XML common schema(./template-schema/schema_core_v12.xsd) that will be used across the templates that can be used to generate the document(PDF).
- We have these example templates XMLs based on the above schema:
  - `./template-schema/Example_CI013_v12.xml`
  - `./template-schema/example_DV015_v12.xml`
  - `./template-schema/example_FR070_v12.xml`
  - `./template-schema/example_PR024_v12.xml`
  - `./template-schema/example_PR027_v12.xml`
  - `./template-schema/example_RF002_v12.xml`
- The above examples would be input to generate documents either through draft editor or bulk creation.
- The API POSTMAN collection is located in `./document-generation/pm-coll-doc-gen-smartcom/SmartComm-CreditOne.postman_collection.json`
- We have to design a flow how the document generation will happen.
- database schema `document-generation/database/schema.sql`.
- What would be the best strategy for solving this technical challenge?
- What would tech stack would be best for this requirement?
- What are the components needed?
- Is the database sufficient to handle this requirement?