package com.grizzly;

import com.grizzly.format.json.JsonTemplate;
import com.grizzly.format.json.JsonTransformationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the nested object example from docs/nested-object-example.md.
 * Demonstrates root → node with members, location/addr, holdings, scoreInfo,
 * safe navigation, arrays, conditionals, and validation.
 */
@DisplayName("Nested Object Example")
class NestedObjectExampleTest {

    private static final String TEMPLATE = """
        def transform(INPUT):
            OUTPUT = {}
            OUTPUT["recordId"] = INPUT?.root?.node?.recordId
            OUTPUT["amount"] = INPUT?.root?.node?.amount
            OUTPUT["rate"] = INPUT?.root?.node?.rate
            OUTPUT["term"] = INPUT?.root?.node?.term
            OUTPUT["kind"] = INPUT?.root?.node?.kind

            members = INPUT?.root?.node?.members
            if members and len(members) > 0:
                OUTPUT["primaryMember"] = {}
                OUTPUT["primaryMember"]["fullName"] = members[0]["firstName"] + " " + members[0]["lastName"]
                OUTPUT["primaryMember"]["income"] = members[0]["income"]
                OUTPUT["primaryMember"]["role"] = members[0]["role"]
                OUTPUT["otherMembers"] = []
                for i in range(1, len(members)):
                    co = {}
                    co["fullName"] = members[i]["firstName"] + " " + members[i]["lastName"]
                    co["income"] = members[i]["income"]
                    OUTPUT["otherMembers"].append(co)
            else:
                OUTPUT["primaryMember"] = None
                OUTPUT["otherMembers"] = []

            addr = INPUT?.root?.node?.location?.addr
            if addr:
                OUTPUT["locationAddr"] = {}
                street2 = addr.get("street2", "")
                OUTPUT["locationAddr"]["street"] = addr["street"] + (" " + street2 if street2 else "")
                OUTPUT["locationAddr"]["city"] = addr["city"]
                OUTPUT["locationAddr"]["state"] = addr["state"]
                OUTPUT["locationAddr"]["zipCode"] = addr["zipCode"]
                OUTPUT["locationAddr"]["formatted"] = addr["city"] + ", " + addr["state"] + " " + addr["zipCode"]
            else:
                OUTPUT["locationAddr"] = None

            OUTPUT["estimatedValue"] = INPUT?.root?.node?.location?.estimatedValue
            OUTPUT["locationType"] = INPUT?.root?.node?.location?.locationType

            holdings = INPUT?.root?.node?.holdings
            totalValue = 0
            OUTPUT["holdings"] = []
            if holdings:
                for a in holdings:
                    totalValue = totalValue + a["value"]
                    OUTPUT["holdings"].append({"type": a["type"], "value": a["value"], "institution": a["institutionName"]})
            OUTPUT["totalHoldingsValue"] = totalValue

            scoreInfo = INPUT?.root?.node?.scoreInfo
            if scoreInfo:
                OUTPUT["score"] = scoreInfo["score"]
                OUTPUT["reportDate"] = scoreInfo["reportDate"]
                OUTPUT["bureau"] = scoreInfo["bureau"]
            else:
                OUTPUT["score"] = None
                OUTPUT["reportDate"] = None
                OUTPUT["bureau"] = None

            return OUTPUT
        """;

    private static final String SAMPLE_INPUT = """
        {
          "root": {
            "rootId": "R-2024-001",
            "rootDate": "2024-02-15",
            "node": {
              "recordId": "REC-55001",
              "amount": 350000,
              "rate": 6.5,
              "term": 360,
              "kind": "Standard",
              "members": [
                { "firstName": "Jane", "lastName": "Doe", "ssn": "***-**-1234", "income": 85000, "role": "Primary" },
                { "firstName": "John", "lastName": "Doe", "ssn": "***-**-5678", "income": 72000, "role": "Secondary" }
              ],
              "location": {
                "addr": {
                  "street": "123 Main St",
                  "street2": "Apt 4",
                  "city": "San Francisco",
                  "state": "CA",
                  "zipCode": "94102",
                  "country": "US"
                },
                "estimatedValue": 425000,
                "locationType": "Single"
              },
              "holdings": [
                { "type": "Checking", "value": 15000, "institutionName": "First Bank", "accountNumber": "****1234" },
                { "type": "Savings", "value": 45000, "institutionName": "First Bank", "accountNumber": "****5678" },
                { "type": "Investment", "value": 120000, "institutionName": "InvestCo", "accountNumber": null }
              ],
              "scoreInfo": {
                "score": 745,
                "reportDate": "2024-01-10",
                "bureau": "Experian",
                "scoreFactors": ["Length of history", "Number of open accounts"]
              }
            }
          }
        }
        """;

    @Test
    @DisplayName("transforms full payload to output schema")
    void transformFullPayload() {
        JsonTemplate template = JsonTemplate.compile(TEMPLATE);
        String output = template.transform(SAMPLE_INPUT);

        assertThat(output).contains("\"recordId\" : \"REC-55001\"");
        assertThat(output).contains("\"amount\" : 350000");
        assertThat(output).contains("\"rate\" : 6.5");
        assertThat(output).contains("\"primaryMember\"");
        assertThat(output).contains("\"fullName\" : \"Jane Doe\"");
        assertThat(output).contains("\"income\" : 85000");
        assertThat(output).contains("\"otherMembers\"");
        assertThat(output).contains("\"fullName\" : \"John Doe\"");
        assertThat(output).contains("\"locationAddr\"");
        assertThat(output).contains("\"street\" : \"123 Main St Apt 4\"");
        assertThat(output).contains("\"formatted\" : \"San Francisco, CA 94102\"");
        assertThat(output).contains("\"estimatedValue\" : 425000");
        assertThat(output).contains("\"totalHoldingsValue\" : 180000");
        assertThat(output).contains("\"score\" : 745");
        assertThat(output).contains("\"bureau\" : \"Experian\"");
    }

    @Test
    @DisplayName("transformWithValidation returns report and output")
    void transformWithValidation() {
        JsonTemplate template = JsonTemplate.compile(TEMPLATE);
        JsonTransformationResult result = template.transformWithValidation(SAMPLE_INPUT);

        assertThat(result.outputJson()).isNotEmpty();
        assertThat(result.validationReport()).isNotNull();
        assertThat(result.executionTimeMs()).isGreaterThanOrEqualTo(0);
        assertThat(result.outputJson()).contains("REC-55001");
    }

    @Test
    @DisplayName("handles edge-case payload with null location and holdings")
    void transformEdgeCasePayload() {
        String edgeInput = """
            {
              "root": {
                "rootId": "R-2024-002",
                "node": {
                  "recordId": "REC-55002",
                  "amount": 200000,
                  "members": [
                    { "firstName": "Solo", "lastName": "User", "income": 60000, "role": "Primary" }
                  ],
                  "location": null,
                  "holdings": null,
                  "scoreInfo": { "score": 710, "reportDate": "2024-02-01", "bureau": "TransUnion" }
                }
              }
            }
            """;

        JsonTemplate template = JsonTemplate.compile(TEMPLATE);
        String output = template.transform(edgeInput);

        assertThat(output).contains("\"recordId\" : \"REC-55002\"");
        assertThat(output).contains("\"amount\" : 200000");
        assertThat(output).contains("\"fullName\" : \"Solo User\"");
        assertThat(output).contains("\"locationAddr\" : null");
        assertThat(output).contains("\"estimatedValue\" : null");
        assertThat(output).contains("\"totalHoldingsValue\" : 0");
        assertThat(output).contains("\"score\" : 710");
    }
}
