# How To Use The Plugin

<b>Prerequisites</b>

- `accumulo-access` plugin is installed.

<br>

<b>Dataset</b>

The following is a sample document stored in Elasticsearch. The dataset originates from [Open Sanctions](https://www.opensanctions.org/) datasets
and has been transformed slightly. The same dataset is available in this repository at [src/test/resources/sanction1.json](../src/test/resources/sanction1.json).

<br>

```json
{
  "id": "10689",
  "entity_type": "Individual",
  "sanction_list_ids": "SDN List",
  "sanction_list_publish_dates": "2008-05-27",
  "sanction_programs": "SDGT",
  "sanction_type": "Block",
  "entity_names": "SAEED, Muhammad, SAEED, Hafiz Muhammad, SAEED, Hafiz, HAFIZ SAHIB, SAEED, Hafiz Mohammad, SAYEED, Hafez Mohammad, SAYID, Hafiz Mohammad, SYEED, Hafiz Mohammad, TATA JI, SAYED, Hafiz Mohammad, SAEED HAFIZ, Muhammad",
  "address_country": "Pakistan",
  "address_address1": "House No. 116 E",
  "address_address2": "Mohalla Johar",
  "address_address3": "Town: Lahore, Tehsil:",
  "address_city": "Lahore City, Lahore District",
  "birthdate": "1950-06-05",
  "place_of_birth": "Sargodha, Punjab, Pakistan",
  "nationality_country": "Pakistan",
  "identity_documents": [
    {
      "document_type": "National ID No.",
      "document_name": "SAEED, Muhammad",
      "document_number": "3520025509842-7",
      "document_valid": "true",
      "issuing_country": "Pakistan"
    },
    {
      "document_type": "National ID No.",
      "document_name": "SAEED, Muhammad",
      "document_number": "23250460642",
      "document_valid": "true",
      "issuing_country": "Pakistan"
    },
    {
      "document_type": "Passport",
      "document_name": "SAEED, Muhammad",
      "document_number": "BE5978421",
      "document_valid": "true",
      "issuing_country": "Pakistan",
      "expiration_data": "2012-11-12"
    },
    {
      "document_type": "Passport",
      "document_name": "SAEED, Muhammad",
      "document_number": "Booklet A5250088",
      "document_valid": "true",
      "issuing_country": "Pakistan"
    }
  ]
}
```
<br>

<b>Elasticsearch Request</b>

```bash
GET https://localhost:9200/{index_name}/_search
```

```json
{
    "query": {
        "bool": {
            "filter": {
                "script": {
                    "script": {
                        "source": "accumulo-access",
                        "lang": "expert_scripts",
                        "params": {
                            "labelField": "sanction_programs",
                            "authorizations": "IFSR,NPWMD",
                            "labelExtractionPolicy": "SPLIT_OR"
                        }
                    }
                }
            }
        }
    }
}
```

where:
- `accumulo-access` is the name of the plugin.
- `labelField` specifies the field name which we are using for determining label for the record.
- `authorizations` is a comma separated string of user's authorizations.
- `labelExtractionPolicy` optional, specifies how we want to handle the label value if it is a comma separated string.
  - Possible values are: 
    - `NONE` - return the string as it is. This is also the default value when this field is not provided.
    - `SPLIT_OR` - string will be split and joined together using `|`.
    - `SPLIT_AND` - string will be split and joined together using `&`.

<br>

<b>What Happens Underneath</b>

Internally we create a Accumulo Access library's `AccessEvaluator` object based on user provided authorization string and use
this evaluator to evaluate against the label value of the document to determine whether user can see this doucment. 

Basically, only when evaluator's `canAccess` method return `true` will this document be returned in the response.

<br>

<b>Support for Multiple Label Extraction Policies</b>

In the sample data provided above, the label value is going to be `SDGT`. For documents with different value such as

```json
{
  "sanction_programs": "NPWMD, IRGC, IFSR, SDGT"
}
```
<br>

Label value will become `NPWMD|IRGC|IFSR|SDGT` if we are using `SPLIT_OR` policy and
`NPWMD&IRGC&IFSR&SDGT` if we are using `SPLIT_AND` policy.


<br>

<b>Support for Nested Field</b>

`labelField` can also point to nested element in the document using `.` notation. Continue using the same dataset,
when the value of `labelField` is configured to be `identity_documents.issuing_country`, label is going to be `Pakistan`. 

<br>

<b>More Complex Label Field</b>

Using the following contrived dataset (it is available in [src/test/resources/test_data1.json](../src/test/resources/test_data1.json))

```json
{
  "field8": {
    "field86": {
      "field865": [123, 456]
    }
  }  
}
```

<br>

When labelField is `field8.field86.field865`, the label value is going to become `123|456`.
What happens here is that values in the array are 'OR' together.

<br>
