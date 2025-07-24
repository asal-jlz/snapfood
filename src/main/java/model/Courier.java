package model;

public class Courier extends User {
    public Courier(int id, String fullName, String phone, String email, String password,
                   String address, String profileImageBase64, BankInfo bankInfo, String status, String salt) {
        super(id, fullName, phone, email, password, "courier", address, profileImageBase64, bankInfo, status, salt);
    }

    @Override
    public void validateRequiredFields() {
        if (getFullName() == null || getPhone() == null || getPassword() == null || getRole() == null || getAddress() == null) {
            throw new IllegalArgumentException("Required fields missing");
        }
    }

}

