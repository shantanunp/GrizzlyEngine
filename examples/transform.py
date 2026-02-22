def transform(INPUT):
    OUTPUT = {}
    
    # Basic mapping
    OUTPUT["id"] = INPUT.customerId
    OUTPUT["fullName"] = INPUT.firstName

    # Contact information
    OUTPUT["contact"]["email"] = INPUT.email
    OUTPUT["contact"]["phone"] = INPUT.phone
    
    # Address
    OUTPUT["address"]["street"] = INPUT.address.street
    OUTPUT["address"]["city"] = INPUT.address.city
    OUTPUT["address"]["state"] = INPUT.address.state
    OUTPUT["address"]["zip"] = INPUT.address.zipCode
    
    # Account details
    OUTPUT["account"]["type"] = INPUT.accountType
    OUTPUT["account"]["balance"] = INPUT.balance
    
    # Conditional logic
    if INPUT.age >= 18:
        OUTPUT["account"]["status"] = "adult"
    else:
        OUTPUT["account"]["status"] = "minor"
    
    # Premium tier determination
    if INPUT.accountType == "PREMIUM":
        if INPUT.balance > 10000:
            OUTPUT["account"]["tier"] = "GOLD"
        else:
            OUTPUT["account"]["tier"] = "SILVER"
    else:
        OUTPUT["account"]["tier"] = "BRONZE"
    
    # Settings
    OUTPUT["settings"]["notifications"] = INPUT.preferences.notifications
    
    return OUTPUT
