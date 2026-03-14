# Nested Object Example

This example showcases Grizzly Engine features using a **root → node** structure with arrays and nested objects (members, location with address, holdings, scoreInfo).

---

## 1. Input Schema

Input is a single root object `INPUT` with a `root` that contains one `node`. The node has arrays (members, holdings) and nested objects (location with address, scoreInfo).

```
INPUT
└── root
    ├── rootId          (string)
    ├── rootDate        (string, date)
    └── node
        ├── recordId        (string)
        ├── amount           (number)
        ├── rate             (number)
        ├── term             (number)
        ├── kind             (string)
        ├── members          (array)
        │   └── [{ firstName, lastName, ssn, income, role }]
        ├── location         (object, can be null)
        │   ├── addr         (object, can be null)
        │   │   ├── street, street2, city, state, zipCode, country
        │   ├── estimatedValue  (number)
        │   └── locationType   (string)
        ├── holdings        (array, can be null)
        │   └── [{ type, value, institutionName, accountNumber }]
        └── scoreInfo       (object, can be null)
            ├── score, reportDate, bureau
            └── scoreFactors  (array)
```

---

## 2. Sample Input Payload

```json
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
```

**Edge-case payload** (nulls for safe navigation):

```json
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
      "scoreInfo": { "score": 710, "reportDate": "2024-02-01" }
    }
  }
}
```

---

## 3. Grizzly Template (Mapping)

- **Safe navigation** `?.` and `?[` for optional root/node/location/scoreInfo (Grizzly extensions; see [PYTHON_EXTENSIONS.md](PYTHON_EXTENSIONS.md))
- **For loops** over `members` and `holdings`
- **Conditionals** and **built-ins**: `len`, `range`, string concat, arithmetic

```python
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
```

---

## 4. Output Schema (summary)

OUTPUT includes: recordId, amount, rate, term, kind, primaryMember, otherMembers, locationAddr, estimatedValue, locationType, holdings, totalHoldingsValue, score, reportDate, bureau.

---

## 5. Features Demonstrated

| Feature | Where in template |
|--------|---------------------|
| Safe navigation `?.` / `?[` | `INPUT?.root?.node?.recordId`, `INPUT?.root?.node?.members` |
| Array iteration | `for i in range(1, len(members))`, `for a in holdings` |
| Conditionals | `if members and len(members) > 0`, `if addr`, `if scoreInfo` |
| Nested access | `members[0]["firstName"]`, `addr["city"]` |
| Optional / null handling | Safe navigation and `if scoreInfo` / `if addr` |

Run with `JsonTemplate.compile(template).transform(inputJson)` or `transformWithValidation(inputJson)`.
