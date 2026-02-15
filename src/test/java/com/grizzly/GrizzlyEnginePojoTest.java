package com.grizzly;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class GrizzlyEnginePojoTest {
    @TempDir Path tempDir;
    
    static class Customer {
        private String customerId, firstName, lastName;
        private PersonalInfo personalInfo;
        public Customer() {}
        public Customer(String customerId, String firstName, String lastName, PersonalInfo personalInfo) {
            this.customerId = customerId; this.firstName = firstName; 
            this.lastName = lastName; this.personalInfo = personalInfo;
        }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public PersonalInfo getPersonalInfo() { return personalInfo; }
        public void setPersonalInfo(PersonalInfo personalInfo) { this.personalInfo = personalInfo; }
    }
    
    static class PersonalInfo {
        private String email, phone;
        public PersonalInfo() {}
        public PersonalInfo(String email, String phone) { this.email = email; this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
    
    static class CustomerDTO {
        private String id, fullName;
        private Contact contact;
        public CustomerDTO() {}
        public CustomerDTO(String id, String fullName, Contact contact) {
            this.id = id; this.fullName = fullName; this.contact = contact;
        }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public Contact getContact() { return contact; }
        public void setContact(Contact contact) { this.contact = contact; }
    }
    
    static class Contact {
        private String email, phone;
        public Contact() {}
        public Contact(String email, String phone) { this.email = email; this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
    
    @Test
    void shouldTransformPojoToPojo() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["id"] = INPUT.customerId
                OUTPUT["fullName"] = INPUT.firstName
                OUTPUT["contact"]["email"] = INPUT.personalInfo.email
                OUTPUT["contact"]["phone"] = INPUT.personalInfo.phone
                return OUTPUT
            """;
        
        Path templateFile = tempDir.resolve("transform.py");
        Files.writeString(templateFile, template);
        
        PersonalInfo personalInfo = new PersonalInfo("john@example.com", "555-1234");
        Customer customer = new Customer("C123", "John", "Doe", personalInfo);
        
        GrizzlyEngine engine = new GrizzlyEngine();
        CustomerDTO result = engine.transform(customer, templateFile.toString(), CustomerDTO.class);
        
        assertThat(result.getId()).isEqualTo("C123");
        assertThat(result.getFullName()).isEqualTo("John");
        assertThat(result.getContact()).isNotNull();
        assertThat(result.getContact().getEmail()).isEqualTo("john@example.com");
        assertThat(result.getContact().getPhone()).isEqualTo("555-1234");
    }
    
    @Test
    void shouldTransformBatchOfPojos() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["id"] = INPUT.customerId
                OUTPUT["fullName"] = INPUT.firstName
                return OUTPUT
            """;
        
        Path templateFile = tempDir.resolve("transform.py");
        Files.writeString(templateFile, template);
        
        GrizzlyEngine engine = new GrizzlyEngine();
        GrizzlyTemplate compiled = engine.compile(templateFile.toString());
        
        Customer customer1 = new Customer("C1", "John", "Doe", null);
        Customer customer2 = new Customer("C2", "Jane", "Smith", null);
        Customer customer3 = new Customer("C3", "Bob", "Johnson", null);
        
        CustomerDTO dto1 = compiled.execute(customer1, CustomerDTO.class);
        CustomerDTO dto2 = compiled.execute(customer2, CustomerDTO.class);
        CustomerDTO dto3 = compiled.execute(customer3, CustomerDTO.class);
        
        assertThat(dto1.getId()).isEqualTo("C1");
        assertThat(dto1.getFullName()).isEqualTo("John");
        assertThat(dto2.getId()).isEqualTo("C2");
        assertThat(dto2.getFullName()).isEqualTo("Jane");
        assertThat(dto3.getId()).isEqualTo("C3");
        assertThat(dto3.getFullName()).isEqualTo("Bob");
    }
}
