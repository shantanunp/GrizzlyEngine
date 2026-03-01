# MISMO-Style Loan Example

This example showcases Grizzly Engine features using a **deal → loan → borrower, address, property, assets, credit** structure typical of mortgage/loan data (MISMO-style).

---

## 1. Input Schema

Input is a single root object `INPUT` with a `deal` that contains one `loan`. The loan has arrays (borrowers, assets) and nested objects (property with address, credit).

```
INPUT
└── deal
    ├── dealId          (string)
    ├── dealDate        (string, date)
    └── loan
        ├── loanId           (string)
        ├── loanAmount       (number)
        ├── interestRate     (number)
        ├── termMonths       (number)
        ├── loanType         (string)
        ├── borrowers        (array)
        │   └── [{
        │         firstName, lastName, ssn, income, role
        │       }]
        ├── property         (object, can be null)
        │   ├── address      (object, can be null)
        │   │   ├── street, street2, city, state, zipCode
        │   │   └── country
        │   ├── estimatedValue  (number)
        │   └── propertyType    (string)
        ├── assets          (array, can be null)
        │   └── [{
        │         type, value, institutionName, accountNumber
        │       }]
        └── credit          (object, can be null)
            ├── score, reportDate, bureau
            └── scoreFactors    (array)
```

---

## 2. Sample Input Payload

Variety: multiple borrowers, property with address, assets array, credit with score factors. Some fields are `null` to show safe navigation.

```json
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
        "scoreFactors": [
          "Length of credit history",
          "Number of open accounts"
        ]
      }
    }
  }
}
```

**Edge-case payload** (nulls and missing nodes for safe navigation):

```json
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
      "credit": { "score": 710, "reportDate": "2024-02-01" }
    }
  }
}
```

---

## 3. Grizzly Template (Mapping)

The template maps the input to a simplified output. It uses:

- **Safe navigation** `?.` and `?[` for optional deal/loan/property/credit
- **For loops** over `borrowers` and `assets`
- **Conditionals** (primary vs co-borrower, optional fields). Note: use nested `if` when you need “and” (e.g. `if borrowers:` then `if len(borrowers) > 0:`).
- **Built-ins**: `len`, `str`, `range`, string concat, arithmetic
- **Dict/list access** with `INPUT.deal.loan?...`

```python
def transform(INPUT):
    OUTPUT = {}
    
    # --- Loan summary (safe navigation: deal or loan may be missing) ---
    OUTPUT["loanId"] = INPUT?.deal?.loan?.loanId
    OUTPUT["loanAmount"] = INPUT?.deal?.loan?.loanAmount
    OUTPUT["interestRate"] = INPUT?.deal?.loan?.interestRate
    OUTPUT["termMonths"] = INPUT?.deal?.loan?.termMonths
    OUTPUT["loanType"] = INPUT?.deal?.loan?.loanType
    
    # --- Primary and co-borrowers (array iteration) ---
    borrowers = INPUT?.deal?.loan?.borrowers
    if borrowers:
        if len(borrowers) > 0:
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
    else:
        OUTPUT["primaryBorrower"] = None
        OUTPUT["coBorrowers"] = []
    
    # --- Property address (nested object, safe navigation) ---
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
    
    # --- Assets (array iteration, sum) ---
    assets = INPUT?.deal?.loan?.assets
    totalAssets = 0
    OUTPUT["assets"] = []
    if assets:
        for a in assets:
            totalAssets = totalAssets + a["value"]
            OUTPUT["assets"].append({"type": a["type"], "value": a["value"], "institution": a["institutionName"]})
    OUTPUT["totalAssetValue"] = totalAssets
    
    # --- Credit (optional object) ---
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
```

Note: Grizzly provides `dict.get("key", default)` for optional keys (e.g. `addr.get("street2", "")`).

---

## 4. Output Schema

What the template produces:

```
OUTPUT
├── loanId            (string | null)
├── loanAmount        (number | null)
├── interestRate      (number | null)
├── termMonths        (number | null)
├── loanType          (string | null)
├── primaryBorrower   (object | null)
│   ├── fullName      (string)
│   ├── income        (number)
│   └── role          (string)
├── coBorrowers       (array of { fullName, income })
├── propertyAddress   (object | null)
│   ├── street        (string)
│   ├── city          (string)
│   ├── state         (string)
│   ├── zipCode       (string)
│   └── formatted     (string)
├── propertyValue     (number | null)
├── propertyType      (string | null)
├── assets            (array of { type, value, institution })
├── totalAssetValue   (number)
├── creditScore       (number | null)
├── creditReportDate  (string | null)
└── creditBureau      (string | null)
```

---

## 5. Example Output (from sample payload)

Running the template on the **first sample payload** (Section 2) yields:

```json
{
  "loanId": "LN-55001",
  "loanAmount": 350000,
  "interestRate": 6.5,
  "termMonths": 360,
  "loanType": "Conventional",
  "primaryBorrower": {
    "fullName": "Jane Doe",
    "income": 85000,
    "role": "Primary"
  },
  "coBorrowers": [
    { "fullName": "John Doe", "income": 72000 }
  ],
  "propertyAddress": {
    "street": "123 Main St Apt 4",
    "city": "San Francisco",
    "state": "CA",
    "zipCode": "94102",
    "formatted": "San Francisco, CA 94102"
  },
  "propertyValue": 425000,
  "propertyType": "SingleFamily",
  "assets": [
    { "type": "Checking", "value": 15000, "institution": "First Bank" },
    { "type": "Savings", "value": 45000, "institution": "First Bank" },
    { "type": "Investment", "value": 120000, "institution": "InvestCo" }
  ],
  "totalAssetValue": 180000,
  "creditScore": 745,
  "creditReportDate": "2024-01-10",
  "creditBureau": "Experian"
}
```

With the **edge-case payload** (null property/assets), `propertyAddress` and `propertyValue`/`propertyType` become `null`, `assets` and `totalAssetValue` are `[]` and `0`, and credit fields are still populated from `credit`.

---

## Features Demonstrated

| Feature | Where in template |
|--------|---------------------|
| Safe navigation `?.` / `?[` | `INPUT?.deal?.loan?.loanId`, `INPUT?.deal?.loan?.borrowers` |
| Array iteration | `for i in range(1, len(borrowers))`, `for a in assets` |
| Conditionals | `if borrowers and len(borrowers) > 0`, `if addr`, `if credit` |
| Nested access | `borrowers[0]["firstName"]`, `addr["city"]` |
| String concat | `borrowers[0]["firstName"] + " " + borrowers[0]["lastName"]` |
| Arithmetic | `totalAssets = totalAssets + a["value"]` |
| List append | `OUTPUT["coBorrowers"].append(co)` |
| Optional / null handling | Safe navigation and `if credit` / `if addr` |

You can run this end-to-end with `JsonTemplate.compile(template).transform(inputJson)` and optionally `transformWithValidation(inputJson)` to inspect path errors and expected nulls.
