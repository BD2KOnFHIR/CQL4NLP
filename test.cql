library HeartFailurePhenotype version '1.1.000'

using FHIR version 'R4'

valueset "Primary Heart Failure Codes 428.X in ICD9CM": '2.16.840.1.113883.3.526.2.24'
valueset "Heart Failure Mentions in Unstructured Problem List": '2.16.840.1.113883.3.526.2.24'
valueset "Heart Failure SNOMED CT Code 84114007 in Structured Problem List": '2.16.840.1.113883.3.526.2.24'
valueset "Structured Problem List Section Codes": '2.16.840.1.113883.3.526.2.24'
valueset "Unstructured Problem List Section Codes": '2.16.840.1.113883.3.526.2.24'
valueset "Clinical Notes Document Type Codes": '2.16.840.1.113883.3.526.2.24'
valueset "NLP System ConText Codes": '2.16.840.1.113883.3.526.2.24'

context Patient

define "Presence of ICD9-CM Diagnosis Codes for HF":
                exists(["Condition": "Primary Heart Failure Codes 428.X in ICD9CM"])

define "Presence of SNOMED CT Code 84114007 in Structured Problem List":
                exists(["Composition": "Clinical Notes Document Type Codes"] doc
                       where doc.section.code in ["Structured Problem List Section Codes"]
                             and  doc.section.entry in ["Condition": "Heart Failure SNOMED CT Code 84114007 in Structured Problem List"]
                             )

define "Presence of Heart Failure Mentions in Unstructured Problem List":
                exists(["Composition": "Clinical Notes Document Type Codes"] doc
                       where doc.extension.nlp_system in ["NLP System ConText Codes"]
                             and doc.section.code in ["Unstructured Problem List Section Codes"]
                             and doc.section.entry in (
                                  ["Condition": "Heart Failure Mentions in Unstructured Problem List"] mentions
                                  where mentions.extension.certainty_modifier = 1 // only include positive mentions
                                  )
                      )

define "Heart Failure Cases":
    "Presence of ICD9-CM Diagnosis Codes for HF"
    and ( "Presence of Heart Failure Mentions in Unstructured Problem List"
        or "Presence of SNOMED CT Code 84114007 in Structured Problem List")
