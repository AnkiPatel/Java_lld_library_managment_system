package com.pravisolutions.models;

import com.pravisolutions.constants.AppConstants;

public class Librarian extends User{

    public Librarian(String name, String email, String phone,
                     String password, LibraryCard libraryCard) {
        super(name, email, phone, password, libraryCard);
    }

    @Override
    public String getRole() {
        return AppConstants.LIBRARIAN;
    }
}
