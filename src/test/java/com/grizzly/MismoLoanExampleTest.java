package com.grizzly;

import com.grizzly.format.json.JsonTemplate;
import com.grizzly.format.json.JsonTransformationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the MISMO-style loan example from docs/mismo-loan-example.md.
 * Demonstrates: deal → loan → borrower, address, property, assets, credit
 * with safe navigation, arrays, conditionals, and validation.
 */
@DisplayName("MISMO Loan Example")
class MismoLoanExampleTest {

    private static final String TEMPLATE = """
        def transform(INPUT):
            OUTPUT = {}
            
            OUTPUT["loanId"] = INPUT?.deal?.loan?.loanId
            OUTPUT["loanAmount"] = INPUT?.deal?.loan?.loanAmount
            OUTPUT["interestRate"] = INPUT?.deal?.loan?.interestRate
            OUTPUT["termMonths"] = INPUT?.deal?.loan?.termMonths
            OUTPUT["loanType"] = INPUT?.deal?.loan?.loanType
            
            borrowers = INPUT?.deal?.loan?.borrowers
            if borrowers and len(borrowers) > 0:
                OUTPUT["primaryBorrower"] = {}
                OUTPUT["primaryBorrower"]["fullName"] = borrowers[0]["firstName"] + " " + borrowers[0]["lastName"]
                OUTPUT["primaryBorrower"]["income"] = borrowers[0]["income"]
                OUTPUT["primaryBorrower"]["role"] = borrowers[0]["role"]

                OUTPUT["coBorrowers"] = []
                for i in range(1, len(borrowers)):
                    co = {}
                    co["fullName"] = borrowers[i]["firstName"] + " " + borrowers[i]["lastName"]
                    co["income"] = borrowers[i]["income"]
                    OUTPUT["coBorrowers"].append(co)
            else:
                OUTPUT["primaryBorrower"] = None
                OUTPUT["coBorrowers"] = []
            
            addr = INPUT?.deal?.loan?.property?.address
            if addr:
                OUTPUT["propertyAddress"] = {}
                street2 = addr.get("street2", "")
                if street2:
                    OUTPUT["propertyAddress"]["street"] = addr["street"] + " " + street2
                else:
                    OUTPUT["propertyAddress"]["street"] = addr["street"]
                OUTPUT["propertyAddress"]["city"] = addr["city"]
                OUTPUT["propertyAddress"]["state"] = addr["state"]
                OUTPUT["propertyAddress"]["zipCode"] = addr["zipCode"]
                OUTPUT["propertyAddress"]["formatted"] = addr["city"] + ", " + addr["state"] + " " + addr["zipCode"]
            else:
                OUTPUT["propertyAddress"] = None
            
            OUTPUT["propertyValue"] = INPUT?.deal?.loan?.property?.estimatedValue
            OUTPUT["propertyType"] = INPUT?.deal?.loan?.property?.propertyType
            
            assets = INPUT?.deal?.loan?.assets
            totalAssets = 0
            OUTPUT["assets"] = []
            if assets:
                for a in assets:
                    totalAssets = totalAssets + a["value"]
                    OUTPUT["assets"].append({"type": a["type"], "value": a["value"], "institution": a["institutionName"]})
            OUTPUT["totalAssetValue"] = totalAssets
            
            credit = INPUT?.deal?.loan?.credit
            if credit:
                OUTPUT["creditScore"] = credit["score"]
                OUTPUT["creditReportDate"] = credit["reportDate"]
                OUTPUT["creditBureau"] = credit["bureau"]
            else:
                OUTPUT["creditScore"] = None
                OUTPUT["creditReportDate"] = None
                OUTPUT["creditBureau"] = None
            
            return OUTPUT
        """;

    private static final String SAMPLE_INPUT = """
        {
          "deal": {
            "dealId": "DL-2024-001",
            "dealDate": "2024-02-15",
            "loan": {
              "loanId": "LN-55001",
              "loanAmount": 350000,
              "interestRate": 6.5,
              "termMonths": 360,
              "loanType": "Conventional",
              "borrowers": [
                {
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "ssn": "***-**-1234",
                  "income": 85000,
                  "role": "Primary"
                },
                {
                  "firstName": "John",
                  "lastName": "Doe",
                  "ssn": "***-**-5678",
                  "income": 72000,
                  "role": "CoBorrower"
                }
              ],
              "property": {
                "address": {
                  "street": "123 Main St",
                  "street2": "Apt 4",
                  "city": "San Francisco",
                  "state": "CA",
                  "zipCode": "94102",
                  "country": "US"
                },
                "estimatedValue": 425000,
                "propertyType": "SingleFamily"
              },
              "assets": [
                { "type": "Checking", "value": 15000, "institutionName": "First Bank", "accountNumber": "****1234" },
                { "type": "Savings", "value": 45000, "institutionName": "First Bank", "accountNumber": "****5678" },
                { "type": "Investment", "value": 120000, "institutionName": "InvestCo", "accountNumber": null }
              ],
              "credit": {
                "score": 745,
                "reportDate": "2024-01-10",
                "bureau": "Experian",
                "scoreFactors": ["Length of credit history", "Number of open accounts"]
              }
            }
          }
        }
        """;

    @Test
    @DisplayName("transforms full MISMO payload to output schema")
    void transformFullPayload() {
        JsonTemplate template = JsonTemplate.compile(TEMPLATE);
        String output = template.transform(SAMPLE_INPUT);

        assertThat(output).contains("\"loanId\" : \"LN-55001\"");
        assertThat(output).contains("\"loanAmount\" : 350000");
        assertThat(output).contains("\"interestRate\" : 6.5");
        assertThat(output).contains("\"primaryBorrower\"");
        assertThat(output).contains("\"fullName\" : \"Jane Doe\"");
        assertThat(output).contains("\"income\" : 85000");
        assertThat(output).contains("\"coBorrowers\"");
        assertThat(output).contains("\"fullName\" : \"John Doe\"");
        assertThat(output).contains("\"propertyAddress\"");
        assertThat(output).contains("\"street\" : \"123 Main St Apt 4\"");
        assertThat(output).contains("\"formatted\" : \"San Francisco, CA 94102\"");
        assertThat(output).contains("\"propertyValue\" : 425000");
        assertThat(output).contains("\"totalAssetValue\" : 180000");
        assertThat(output).contains("\"creditScore\" : 745");
        assertThat(output).contains("\"creditBureau\" : \"Experian\"");
    }

    @Test
    @DisplayName("transformWithValidation returns report and output")
    void transformWithValidation() {
        JsonTemplate template = JsonTemplate.compile(TEMPLATE);
        JsonTransformationResult result = template.transformWithValidation(SAMPLE_INPUT);

        assertThat(result.outputJson()).isNotEmpty();
        assertThat(result.validationReport()).isNotNull();
        assertThat(result.executionTimeMs()).isGreaterThanOrEqualTo(0);
        assertThat(result.outputJson()).contains("LN-55001");
    }

    @Test
    @DisplayName("handles edge-case payload with null property and assets")
    void transformEdgeCasePayload() {
        String edgeInput = """
            {
              "deal": {
                "dealId": "DL-2024-002",
                "loan": {
                  "loanId": "LN-55002",
                  "loanAmount": 200000,
                  "borrowers": [
                    { "firstName": "Solo", "lastName": "Borrower", "income": 60000, "role": "Primary" }
                  ],
                  "property": null,
                  "assets": null,
                  "credit": { "score": 710, "reportDate": "2024-02-01", "bureau": "TransUnion" }
                }
              }
            }
            """;

        JsonTemplate template = JsonTemplate.compile(TEMPLATE);
        String output = template.transform(edgeInput);

        assertThat(output).contains("\"loanId\" : \"LN-55002\"");
        assertThat(output).contains("\"loanAmount\" : 200000");
        assertThat(output).contains("\"fullName\" : \"Solo Borrower\"");
        assertThat(output).contains("\"propertyAddress\" : null");
        assertThat(output).contains("\"propertyValue\" : null");
        assertThat(output).contains("\"totalAssetValue\" : 0");
        assertThat(output).contains("\"creditScore\" : 710");
    }
}
